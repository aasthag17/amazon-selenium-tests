package com.amazon.automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * AmazonHelper - thread-safe, stateless helper for Amazon.com interactions.
 *
 * Strategy:
 *   1. Navigate directly to /s?k=<query>  (bypasses homepage bot-check)
 *   2. Click first organic result
 *   3. If the product page shows a variant-selection prompt ("Please choose"),
 *      auto-select the first available option for each dimension (size, color,
 *      storage, etc.) so the Add-to-Cart button becomes active.
 */
public class AmazonHelper {

    private static final Duration SHORT  = Duration.ofSeconds(8);
    private static final Duration MEDIUM = Duration.ofSeconds(25);
    private static final Duration LONG   = Duration.ofSeconds(45);

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    public static void searchAmazon(WebDriver driver, String query) {
        removeWebdriverFlag(driver);
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.amazon.com/s?k=" + encoded;
        System.out.println("[AmazonHelper] Loading: " + url);
        driver.get(url);
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);

        new WebDriverWait(driver, LONG).until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-component-type='s-search-result']")),
            ExpectedConditions.presenceOfElementLocated(By.id("twotabsearchtextbox"))
        ));
    }

    public static void dismissLocationPopup(WebDriver driver) {
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

    // -------------------------------------------------------------------------
    // Product opener
    // -------------------------------------------------------------------------

    /**
     * Clicks the first organic product result on the search page using JS,
     * then waits for the product detail page to load.
     */
    public static void openFirstProduct(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, LONG);

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("[data-component-type='s-search-result']")));
        removeWebdriverFlag(driver);

        String[] linkSelectors = {
            "[data-component-type='s-search-result']:not([data-sponsored-label-info]) h2 a",
            "[data-component-type='s-search-result'] h2 a",
            "[data-component-type='s-search-result'] a[href*='/dp/']"
        };

        WebElement target = null;
        for (String sel : linkSelectors) {
            List<WebElement> found = driver.findElements(By.cssSelector(sel));
            if (!found.isEmpty()) { target = found.get(0); break; }
        }

        if (target == null) throw new NoSuchElementException(
            "No product links found on: " + driver.getCurrentUrl());

        System.out.println("[AmazonHelper] Clicking first result...");
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", target);
        sleep(300);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);
    }

    // -------------------------------------------------------------------------
    // Variant selection  (fixes "Please choose a variation" alert)
    // -------------------------------------------------------------------------

    /**
     * Detects any unselected variant dimension (size, color, storage, etc.) and
     * clicks the first available (non-disabled) option for each.
     *
     * Amazon renders variant selectors in two ways:
     *   A) Swatch tiles  - li elements inside #variation_<dimension>
     *   B) Dropdown      - select element inside #variation_<dimension>
     *
     * We try both styles and click/select the first enabled option for every
     * dimension present on the page.
     *
     * @return true if at least one variant was selected, false if none needed.
     */
    public static boolean selectDefaultVariants(WebDriver driver) {
        boolean anySelected = false;

        // --- Style A: swatch / button tiles ---
        // Each dimension wrapper has id="variation_<name>"
        List<WebElement> swatchGroups = driver.findElements(
            By.cssSelector("[id^='variation_']"));

        for (WebElement group : swatchGroups) {
            try {
                // Skip if a tile is already selected
                List<WebElement> selected = group.findElements(
                    By.cssSelector("li.selected, li[class*='selected'], " +
                                   "span.selection:not(:empty)"));
                if (!selected.isEmpty()) continue;

                // Click the first non-disabled tile
                List<WebElement> tiles = group.findElements(
                    By.cssSelector("li:not(.dimmed):not([class*='unavailable'])" +
                                   ":not([class*='disabled'])"));
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

        // --- Style B: <select> dropdowns (some products use these) ---
        List<WebElement> dropdowns = driver.findElements(
            By.cssSelector("[id^='variation_'] select, " +
                           "select[id*='native_dropdown_selected']"));

        for (WebElement sel : dropdowns) {
            try {
                org.openqa.selenium.support.ui.Select select =
                    new org.openqa.selenium.support.ui.Select(sel);
                if (select.getFirstSelectedOption().getAttribute("value") == null ||
                    select.getFirstSelectedOption().getAttribute("value").isEmpty() ||
                    select.getFirstSelectedOption().getText().toLowerCase().contains("select")) {
                    // Pick the first real option (index 1 skips the placeholder)
                    List<WebElement> opts = select.getOptions();
                    for (int i = 1; i < opts.size(); i++) {
                        if (!opts.get(i).getAttribute("class").contains("dropdownAvailable") ||
                             opts.get(i).isEnabled()) {
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

    /**
     * Checks whether the page shows a "Please choose" / "Please select" prompt,
     * indicating that variant selection is required before adding to cart.
     */
    public static boolean hasVariantSelectionError(WebDriver driver) {
        try {
            List<WebElement> alerts = driver.findElements(By.cssSelector(
                "#variation-swatch-0_error_notification_message, " +
                ".a-alert-content, " +
                "#variation_style_name_error_notification_message"));
            for (WebElement el : alerts) {
                String text = el.getText().toLowerCase();
                if (text.contains("please") || text.contains("choose") ||
                    text.contains("select")) {
                    return true;
                }
            }
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

        // Last resort: combine .a-price-whole + .a-price-fraction
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
     * Adds the current product to cart.
     * If a "Please choose a variation" error appears after clicking Add to Cart,
     * this method automatically selects the first available variant for every
     * dimension and retries once.
     *
     * @return true if successfully added, false if the button was not found or
     *         cart confirmation did not appear within the timeout.
     */
    public static boolean addToCart(WebDriver driver) {
        By btnSelector = By.cssSelector(
            "#add-to-cart-button, " +
            "input[name='submit.add-to-cart'], " +
            "#submit\\.add-to-cart, " +
            "[data-feature-id='add-to-cart'] input[type='submit']");

        // Attempt 1 (and attempt 2 after variant fix if needed)
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, MEDIUM);
                WebElement addBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(btnSelector));
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", addBtn);
                sleep(300);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

                // Wait briefly, then check for variant-selection error
                sleep(800);
                if (hasVariantSelectionError(driver)) {
                    System.out.println("[AmazonHelper] Variant selection required — selecting defaults...");
                    selectDefaultVariants(driver);
                    sleep(600);
                    // Loop continues: retry the Add-to-Cart click on attempt 2
                    continue;
                }

                // Wait for any cart-confirmation signal
                new WebDriverWait(driver, MEDIUM).until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(
                        By.id("attachSiNoCoverage-announce")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("[data-feature-id='add-to-cart-confirmation']")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("#sw-atc-confirmation, #huc-v2-order-row-confirm-text")),
                    ExpectedConditions.urlContains("/cart")
                ));
                return true;

            } catch (TimeoutException e) {
                if (attempt == 1 && hasVariantSelectionError(driver)) {
                    System.out.println("[AmazonHelper] Variant error on timeout — selecting defaults...");
                    selectDefaultVariants(driver);
                    sleep(600);
                } else {
                    System.out.println("[AmazonHelper] Add to Cart not available on this product.");
                    return false;
                }
            }
        }

        System.out.println("[AmazonHelper] Add to Cart failed after variant selection attempt.");
        return false;
    }

    public static String getCartCount(WebDriver driver) {
        try {
            WebElement badge = driver.findElement(By.id("nav-cart-count"));
            String t = badge.getText().trim();
            if (t.isEmpty()) t = (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].textContent;", badge);
            return t != null ? t.trim() : "?";
        } catch (Exception e) { return "?"; }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); }
    }
}
