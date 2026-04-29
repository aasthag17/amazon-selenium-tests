package com.amazon.automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AmazonHelper - thread-safe, stateless helper for Amazon.com interactions.
 *
 * Key strategies:
 *   1. Navigate directly to /s?k=<query>  (bypasses homepage bot-check)
 *   2. Collect up to 5 organic result URLs, skip "Renewed" / "Refurbished"
 *   3. On each product page:
 *        a. Set delivery to a US ZIP (fixes geo-restriction "cannot ship" errors)
 *        b. Auto-select required variants (color, size, storage)
 *        c. Click Add to Cart and wait for confirmation
 *   4. Move to next result if Add to Cart still fails
 */
public class AmazonHelper {

    private static final Duration SHORT  = Duration.ofSeconds(8);
    private static final Duration MEDIUM = Duration.ofSeconds(20);
    private static final Duration LONG   = Duration.ofSeconds(45);

    // A real US ZIP — avoids "cannot ship to your location" blocks on LambdaTest
    private static final String US_ZIP = "10001";

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    public static void searchAmazon(WebDriver driver, String query) {
        removeWebdriverFlag(driver);
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // Explicitly filter for "New" condition to avoid "Renewed" listings
        String url = "https://www.amazon.com/s?k=" + encoded + "&rh=p_n_condition-type%3A6503240011";
        System.out.println("[AmazonHelper] Navigating to: " + url);
        driver.get(url);
        removeWebdriverFlag(driver);
        dismissPopups(driver);

        new WebDriverWait(driver, LONG).until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-component-type='s-search-result']")),
            ExpectedConditions.presenceOfElementLocated(By.id("twotabsearchtextbox"))
        ));
        System.out.println("[AmazonHelper] Search results loaded.");
    }

    // -------------------------------------------------------------------------
    // Smart product opener — tries up to 5 results, skips Renewed
    // -------------------------------------------------------------------------

    /**
     * Opens the best available product from the search results:
     * - Collects up to 5 organic result URLs (no sponsored)
     * - Skips products with "Renewed" or "Refurbished" in their title
     * - Sets US delivery ZIP on each product page to unblock Add to Cart
     * - Returns after successfully opening a product with a visible title
     */
    public static void openFirstProduct(WebDriver driver) {
        openFirstProduct(driver, false);
    }

    public static void openFirstProduct(WebDriver driver, boolean skipRenewed) {
        WebDriverWait wait = new WebDriverWait(driver, LONG);

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("[data-component-type='s-search-result']")));
        removeWebdriverFlag(driver);

        // Collect up to 10 result URLs to give more chances to find a new product
        List<String> urls = collectResultUrls(driver, 10);
        if (urls.isEmpty()) throw new NoSuchElementException(
            "[AmazonHelper] No product URLs found on search results page.");

        System.out.println("[AmazonHelper] Found " + urls.size() + " result URLs to try.");

        String searchUrl = driver.getCurrentUrl();
        String fallbackUrl   = null;  // best Renewed fallback if all are Renewed
        String fallbackTitle = null;

        for (int i = 0; i < urls.size(); i++) {
            String productUrl = urls.get(i);
            System.out.println("[AmazonHelper] Trying result " + (i + 1) + ": " + productUrl);

            driver.get(productUrl);
            removeWebdriverFlag(driver);
            dismissPopups(driver);

            // Wait for product title
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
            } catch (TimeoutException e) {
                System.out.println("[AmazonHelper] No product title on result " + (i + 1) + " - skipping.");
                driver.get(searchUrl);
                sleep(300);
                continue;
            }

            String title = extractTitle(driver);
            String lower  = title.toLowerCase();
            boolean isRenewed = lower.contains("renewed") || lower.contains("refurbished");

            if (skipRenewed && isRenewed) {
                System.out.println("[AmazonHelper] Skipping Renewed product: " + title);
                if (fallbackUrl == null) { fallbackUrl = productUrl; fallbackTitle = title; }
                driver.get(searchUrl);
                sleep(300);
                continue;
            }

            // Set US delivery ZIP to unblock geo-restricted Add to Cart
            setDeliveryZip(driver);
            System.out.println("[AmazonHelper] Opened product: " + title);
            return;
        }

        // All results were Renewed - use the first one as fallback rather than failing
        if (skipRenewed && fallbackUrl != null) {
            System.out.println("[AmazonHelper] All results are Renewed. Using fallback: " + fallbackTitle);
            driver.get(fallbackUrl);
            removeWebdriverFlag(driver);
            dismissPopups(driver);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
            setDeliveryZip(driver);
            return;
        }

        throw new NoSuchElementException(
            "[AmazonHelper] No suitable product found after trying all results.");
    }

    // -------------------------------------------------------------------------
    // Delivery location fix (unblocks "cannot ship" geo-restriction)
    // -------------------------------------------------------------------------

    /**
     * Clicks the delivery location widget and sets ZIP to US_ZIP.
     * This is required when LambdaTest's machine is detected as non-US,
     * which makes Amazon show "This item cannot be shipped to your location."
     */
    public static void setDeliveryZip(WebDriver driver) {
        try {
            // Check if the delivery-block is present
            List<WebElement> deliveryBlock = driver.findElements(
                By.cssSelector("#nav-global-location-popover-link, #glow-ingress-block"));
            if (deliveryBlock.isEmpty()) return;

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();", deliveryBlock.get(0));
            sleep(800);

            // Type ZIP into the ZIP field
            WebDriverWait shortWait = new WebDriverWait(driver, SHORT);
            WebElement zipInput = shortWait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[data-action='GLUXZipUpdateInput'], " +
                                   "#GLUXZipUpdateInput")));
            zipInput.clear();
            zipInput.sendKeys(US_ZIP);
            sleep(300);

            // Click Apply
            WebElement applyBtn = driver.findElement(
                By.cssSelector("span[data-action='GLUXZipUpdate'] input, " +
                               "#GLUXZipUpdate input, " +
                               "input[aria-labelledby='GLUXZipUpdate-announce']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyBtn);
            sleep(1000);

            // Dismiss the popup (Done / Continue button)
            try {
                WebElement done = new WebDriverWait(driver, SHORT).until(
                    ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#GLUXConfirmClose, " +
                                       "[data-action='GLUXConfirmClose'] input, " +
                                       "input[name='glowDoneButton'], " +
                                       ".a-popover-footer input")));
                
                // Get a reference to an element to wait for staleness
                WebElement oldLocation = driver.findElement(By.id("nav-global-location-slot"));
                
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", done);
                
                // Wait for the page to reload or the location to update
                try {
                    new WebDriverWait(driver, SHORT).until(ExpectedConditions.stalenessOf(oldLocation));
                } catch (Exception ignored) {}
                
                sleep(2000); // Give it extra time to render the Add to Cart button
            } catch (Exception ignored) {}

            System.out.println("[AmazonHelper] Delivery ZIP set to " + US_ZIP);
        } catch (Exception e) {
            System.out.println("[AmazonHelper] Could not set delivery ZIP: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Variant selection (fixes "Please choose a variation" alert)
    // -------------------------------------------------------------------------

    public static boolean selectDefaultVariants(WebDriver driver) {
        boolean anySelected = false;

        // Style A: swatch / button tiles
        List<WebElement> swatchGroups = driver.findElements(
            By.cssSelector("[id^='variation_']"));

        for (WebElement group : swatchGroups) {
            try {
                List<WebElement> selected = group.findElements(
                    By.cssSelector("li.selected, li[class*='selected'], span.selection:not(:empty)"));
                if (!selected.isEmpty()) continue;

                List<WebElement> tiles = group.findElements(
                    By.cssSelector("li:not(.dimmed):not([class*='unavailable']):not([class*='disabled'])"));
                if (!tiles.isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center'});", tiles.get(0));
                    sleep(200);
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();", tiles.get(0));
                    sleep(400);
                    anySelected = true;
                    System.out.println("[AmazonHelper] Selected variant tile in: " +
                        group.getAttribute("id"));
                }
            } catch (Exception ignored) {}
        }

        // Style B: native <select> dropdowns
        List<WebElement> dropdowns = driver.findElements(
            By.cssSelector("[id^='variation_'] select, select[id*='native_dropdown_selected']"));

        for (WebElement sel : dropdowns) {
            try {
                Select select = new Select(sel);
                String firstVal = select.getFirstSelectedOption().getAttribute("value");
                if (firstVal == null || firstVal.isEmpty() ||
                    select.getFirstSelectedOption().getText().toLowerCase().contains("select")) {
                    List<WebElement> opts = select.getOptions();
                    for (int i = 1; i < opts.size(); i++) {
                        if (opts.get(i).isEnabled()) {
                            select.selectByIndex(i);
                            sleep(500);
                            anySelected = true;
                            System.out.println("[AmazonHelper] Selected dropdown option: " +
                                opts.get(i).getText());
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return anySelected;
    }

    public static boolean hasVariantSelectionError(WebDriver driver) {
        try {
            List<WebElement> alerts = driver.findElements(By.cssSelector(
                "#variation-swatch-0_error_notification_message, " +
                ".a-alert-content, " +
                "#variation_style_name_error_notification_message"));
            for (WebElement el : alerts) {
                String text = el.getText().toLowerCase();
                if (text.contains("please") || text.contains("choose") || text.contains("select")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // -------------------------------------------------------------------------
    // Check for geo-restriction error on product page
    // -------------------------------------------------------------------------

    public static boolean hasShippingRestriction(WebDriver driver) {
        try {
            List<WebElement> els = driver.findElements(By.cssSelector(
                "#exports_desktop_qualifiedBuybox_tlc_feature_div, " +
                "#buybox-see-all-buying-choices, " +
                "#outOfStock, " +
                ".a-color-error"));
            for (WebElement el : els) {
                String text = el.getText().toLowerCase();
                if (text.contains("cannot be shipped") ||
                    text.contains("not available") ||
                    text.contains("unavailable") ||
                    text.contains("no offers")) {
                    return true;
                }
            }
            // Also check the full page body for the error message
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("this item cannot be shipped to your selected delivery location");
        } catch (Exception ignored) {}
        return false;
    }

    // -------------------------------------------------------------------------
    // Data Extraction
    // -------------------------------------------------------------------------

    public static String extractTitle(WebDriver driver) {
        try {
            WebElement el = new WebDriverWait(driver, MEDIUM).until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productTitle")));
            return el.getText().trim();
        } catch (Exception e) { return "Title not found"; }
    }

    public static String extractPrice(WebDriver driver) {
        String[] selectors = {
            ".priceToPay .a-offscreen",
            "#corePrice_feature_div .a-offscreen",
            "#corePriceDisplay_desktop_feature_div .a-offscreen",
            "#usedBuySection .a-price .a-offscreen",
            "#newAccordionRow .a-price .a-offscreen",
            "#price_inside_buybox",
            "#priceblock_ourprice",
            "#priceblock_dealprice",
            "#apex_offerDisplay_desktop .a-price .a-offscreen",
            "[data-feature-id='desktop-dp-price'] .a-offscreen",
            ".a-price .a-offscreen",
            ".a-price"
        };

        for (String sel : selectors) {
            try {
                for (WebElement el : driver.findElements(By.cssSelector(sel))) {
                    String text = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent;", el);
                    if (text != null) {
                        text = text.replaceAll("[\\n\\r\\t]", " ")
                                   .replaceAll(" +", " ").trim();
                        if (text.contains("$") || text.contains("\u20b9") ||
                            text.contains("INR") || text.matches(".*\\d+[,.]\\d{2}.*")) {
                            return text;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Last resort: whole + fraction
        try {
            String w = (String) ((JavascriptExecutor) driver).executeScript(
                "var w=document.querySelector('.a-price-whole');" +
                "var f=document.querySelector('.a-price-fraction');" +
                "return w ? '$'+(w.textContent+(f?f.textContent:'')).trim() : null;");
            if (w != null && !w.isEmpty()) return w;
        } catch (Exception ignored) {}

        return "Price not found";
    }

    // -------------------------------------------------------------------------
    // Cart
    // -------------------------------------------------------------------------

    /**
     * Adds the product to cart with full retry logic:
     *   - Attempt 1: click Add to Cart
     *   - If variant error: auto-select variants, retry
     *   - Waits for cart confirmation dialog or URL change
     */
    public static boolean addToCart(WebDriver driver) {
        By btnSelector = By.cssSelector(
            "#add-to-cart-button, " +
            "input[name='submit.add-to-cart'], " +
            "#submit\\.add-to-cart, " +
            "[data-feature-id='add-to-cart'] input[type='submit']");

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, MEDIUM);
                WebElement addBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(btnSelector));
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", addBtn);
                sleep(400);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

                sleep(800);

                if (hasVariantSelectionError(driver)) {
                    System.out.println("[AmazonHelper] Variant selection required — selecting defaults...");
                    selectDefaultVariants(driver);
                    sleep(600);
                    continue;
                }

                new WebDriverWait(driver, MEDIUM).until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(
                        By.id("attachSiNoCoverage-announce")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("[data-feature-id='add-to-cart-confirmation']")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("#sw-atc-confirmation, #huc-v2-order-row-confirm-text")),
                    ExpectedConditions.urlContains("/cart")
                ));
                System.out.println("[AmazonHelper] Added to cart successfully.");
                return true;

            } catch (TimeoutException | NoSuchElementException e) {
                // If primary Add to Cart failed, try "See All Buying Options"
                try {
                    WebElement seeAllBtn = driver.findElement(By.cssSelector(
                        "a[title='See All Buying Options'], #buybox-see-all-buying-choices a, #buybox-see-all-buying-choices-announce"));
                    System.out.println("[AmazonHelper] Primary Add to Cart missing. Clicking 'See All Buying Options'...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", seeAllBtn);
                    sleep(1500);

                    // Wait for the side panel and click the first Add to Cart button in the list
                    WebElement flyoutAddBtn = new WebDriverWait(driver, SHORT).until(
                        ExpectedConditions.elementToBeClickable(
                            By.cssSelector("#aod-offer-list input[name='submit.addToCart'], .aod-add-to-cart-button input")));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", flyoutAddBtn);
                    sleep(1500);
                    
                    System.out.println("[AmazonHelper] Added to cart from buying options panel successfully.");
                    return true;
                } catch (Exception ignored2) {
                    if (attempt == 1 && hasVariantSelectionError(driver)) {
                        System.out.println("[AmazonHelper] Variant error on timeout — selecting defaults...");
                        selectDefaultVariants(driver);
                        sleep(600);
                    } else {
                        System.out.println("[AmazonHelper] Add to Cart timed out on attempt " + attempt);
                        return false;
                    }
                }
            }
        }

        System.out.println("[AmazonHelper] Add to Cart failed after all attempts.");
        return false;
    }

    public static String getCartCount(WebDriver driver) {
        try {
            WebElement badge = driver.findElement(By.id("nav-cart-count"));
            String t = badge.getText().trim();
            if (t.isEmpty()) t = (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].textContent;", badge);
            return t != null ? t.trim() : "0";
        } catch (Exception e) { return "0"; }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Collects up to maxCount product URLs from the current search results page.
     * Prefers organic (non-sponsored) results.
     */
    private static List<String> collectResultUrls(WebDriver driver, int maxCount) {
        List<String> urls = new ArrayList<>();

        // Prefer non-sponsored results first
        String[] selectors = {
            "[data-component-type='s-search-result']:not([data-sponsored-label-info]) h2 a[href*='/dp/']",
            "[data-component-type='s-search-result'] h2 a[href*='/dp/']",
            "[data-component-type='s-search-result'] a[href*='/dp/']"
        };

        for (String sel : selectors) {
            if (urls.size() >= maxCount) break;
            List<WebElement> elements = driver.findElements(By.cssSelector(sel));
            for (WebElement el : elements) {
                if (urls.size() >= maxCount) break;
                try {
                    String href = el.getAttribute("href");
                    if (href != null && href.contains("/dp/") && !urls.contains(href)) {
                        urls.add(href);
                    }
                } catch (Exception ignored) {}
            }
        }

        return urls;
    }

    public static void dismissPopups(WebDriver driver) {
        try {
            new WebDriverWait(driver, SHORT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(
                    "button[data-action='a-popover-close'], " +
                    "input[data-action='a-popover-close'], " +
                    "#glow-ingress-block button"
                ))
            ).click();
        } catch (Exception ignored) {}
    }

    public static void removeWebdriverFlag(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator,'webdriver',{get:()=>undefined});");
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); }
    }
}
