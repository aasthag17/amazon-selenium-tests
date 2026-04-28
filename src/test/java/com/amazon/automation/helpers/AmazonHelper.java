package com.amazon.automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * AmazonHelper
 * ─────────────────────────────────────────────────────────────────────────────
 * Reusable, stateless helper methods for interacting with Amazon.com.
 *
 * Key design decisions:
 *  • Uses direct search URL (/s?k=...) instead of going to homepage first –
 *    this bypasses Amazon's homepage bot-detection page reliably.
 *  • Removes navigator.webdriver via JS injection on every page load.
 *  • All methods are stateless and thread-safe (driver passed as parameter).
 *  • Explicit WebDriverWait everywhere; implicitWait is set to 0.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class AmazonHelper {

    // Timeout constants
    private static final Duration SHORT  = Duration.ofSeconds(8);
    private static final Duration MEDIUM = Duration.ofSeconds(25);
    private static final Duration LONG   = Duration.ofSeconds(45);

    // ── Navigation & Search ───────────────────────────────────────────────

    /**
     * Navigates DIRECTLY to Amazon's search-results page for the given query.
     *
     * Strategy: skip the homepage entirely and go straight to
     * /s?k=<query>&ref=nb_sb_noss  — this avoids the homepage
     * bot-detection / CAPTCHA interstitial that fires on automated sessions.
     *
     * @param driver WebDriver instance
     * @param query  Search term (e.g. "iPhone", "Samsung Galaxy smartphone")
     */
    public static void searchAmazon(WebDriver driver, String query) {
        // Remove navigator.webdriver fingerprint immediately
        removeWebdriverFlag(driver);

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String searchUrl = "https://www.amazon.com/s?k=" + encoded + "&ref=nb_sb_noss";

        System.out.println("  [AmazonHelper] Loading: " + searchUrl);
        driver.get(searchUrl);

        // Remove navigator.webdriver again after page load
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);

        // Wait until at least one result card or the search box is visible
        WebDriverWait wait = new WebDriverWait(driver, LONG);
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-component-type='s-search-result']")),
            ExpectedConditions.presenceOfElementLocated(
                By.id("twotabsearchtextbox"))
        ));

        // If we landed on homepage (rare fallback) – use search box
        if (!driver.getCurrentUrl().contains("/s?") &&
            !driver.getCurrentUrl().contains("/s/")) {
            System.out.println("  [AmazonHelper] Fell back to homepage search …");
            searchFromHomepage(driver, query, wait);
        }
    }

    /**
     * Fallback: fills the search box and submits via JS (avoids click timeouts).
     */
    private static void searchFromHomepage(WebDriver driver, String query,
                                            WebDriverWait wait) {
        WebElement searchBox = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("twotabsearchtextbox"))
        );
        // JS fill – more reliable than sendKeys when Chrome is throttled
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];", searchBox, query);
        searchBox.sendKeys(Keys.RETURN);
        wait.until(ExpectedConditions.urlContains("/s"));
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);
    }

    /**
     * Removes the navigator.webdriver flag that sites use to detect Selenium.
     */
    public static void removeWebdriverFlag(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
            );
        } catch (Exception ignored) {
            // Not critical – swallow silently
        }
    }

    /**
     * Dismisses the "Choose your location" popup if it appears.
     * Silently swallows any error if the popup is absent.
     */
    public static void dismissLocationPopup(WebDriver driver) {
        try {
            WebDriverWait popupWait = new WebDriverWait(driver, SHORT);
            WebElement close = popupWait.until(
                ExpectedConditions.elementToBeClickable(
                    By.cssSelector(
                        "button[data-action='a-popover-close'], " +
                        "input[data-action='a-popover-close'], " +
                        "#glow-ingress-block button, " +
                        "span.a-button-text[aria-label*='Dismiss']"
                    )
                )
            );
            close.click();
        } catch (TimeoutException | NoSuchElementException ignored) {
            // No popup – that is fine
        }
    }

    // ── Product Selection ─────────────────────────────────────────────────

    /**
     * Clicks the first non-sponsored search result.
     * Falls back to the very first result if all are sponsored.
     *
     * @param driver WebDriver on the search-results page
     */
    public static void openFirstProduct(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, LONG);

        // Wait for at least one result card
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("[data-component-type='s-search-result']")
        ));

        removeWebdriverFlag(driver);

        // Try organic (non-sponsored) results first
        List<WebElement> organic = driver.findElements(
            By.cssSelector(
                "[data-component-type='s-search-result']:not([data-sponsored-label-info]) h2 a.a-link-normal"
            )
        );

        if (organic.isEmpty()) {
            // Try without the sponsored exclusion
            organic = driver.findElements(
                By.cssSelector("[data-component-type='s-search-result'] h2 a.a-link-normal")
            );
        }

        if (organic.isEmpty()) {
            // Final fallback – any clickable result link
            organic = driver.findElements(
                By.cssSelector("[data-component-type='s-search-result'] a.a-link-normal[href*='/dp/']")
            );
        }

        if (organic.isEmpty()) {
            throw new NoSuchElementException(
                "No product results found on the search page. URL: " + driver.getCurrentUrl());
        }

        WebElement target = organic.get(0);

        // Scroll target into view
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", target);

        // Small pause so the page settles after scroll
        sleep(500);

        // JS click – bypasses overlay and renderer-timeout issues
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);

        // Wait for product detail page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);
    }

    // ── Data Extraction ───────────────────────────────────────────────────

    /**
     * Extracts the product title from the detail page.
     *
     * @param driver WebDriver on the product detail page
     * @return Trimmed product title string, or "Title not found"
     */
    public static String extractTitle(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, MEDIUM);
            WebElement titleEl = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("productTitle"))
            );
            return titleEl.getText().trim();
        } catch (Exception e) {
            return "Title not found";
        }
    }

    /**
     * Extracts the product price from the detail page.
     * Tries multiple CSS selectors in order of preference for robustness
     * against Amazon's A/B-tested layouts.
     *
     * @param driver WebDriver on the product detail page
     * @return Price string (e.g. "$999.00"), or "Price not found"
     */
    public static String extractPrice(WebDriver driver) {
        // Ordered from most-specific to most-generic
        String[] selectors = {
            // Standard buy-box price
            ".priceToPay .a-offscreen",
            "#corePrice_feature_div .a-offscreen",
            "#corePriceDisplay_desktop_feature_div .a-offscreen",
            // Renewed / used / 3rd-party seller prices
            "#usedBuySection .a-price .a-offscreen",
            "#newAccordionRow .a-price .a-offscreen",
            "#aod-price-1 .a-offscreen",
            "#olp_feature_div .a-color-price",
            "#price_inside_buybox",
            "#priceblock_ourprice",
            "#priceblock_dealprice",
            "#apex_offerDisplay_desktop .a-price .a-offscreen",
            // Generic fallbacks
            ".a-price .a-offscreen",
            "[data-feature-id='desktop-dp-price'] .a-offscreen",
            ".a-price-whole",
            ".a-price"
        };

        for (String selector : selectors) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(selector));
                for (WebElement el : els) {
                    // .a-offscreen is visually hidden – use textContent attribute
                    String text = (String) ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].textContent;", el);
                    if (text != null) {
                        text = text.replaceAll("[\\n\\r\\t]", " ")
                                   .replaceAll(" +", " ").trim();
                        // Accept anything that looks like a currency amount
                        if (text.contains("$") || text.contains("₹") || text.contains("INR")
                            || text.matches(".*\\d+[,.]\\d{2}.*")) {
                            return text;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Try next selector
            }
        }

        // Last-resort: grab ALL text from .a-price-whole + .a-price-fraction
        try {
            String whole = (String) ((JavascriptExecutor) driver).executeScript(
                "var w = document.querySelector('.a-price-whole');"
              + "var f = document.querySelector('.a-price-fraction');"
              + "if(w) return (w.textContent + (f ? f.textContent : '')).trim();"
              + "return null;");
            if (whole != null && !whole.isEmpty()) return "$" + whole;
        } catch (Exception ignored) {}

        return "Price not found";
    }

    // ── Cart Interaction ──────────────────────────────────────────────────

    /**
     * Attempts to add the current product to the cart.
     * Handles pages that require variant selection (size, colour, etc.)
     * by gracefully returning {@code false} when the button is absent.
     *
     * @param driver WebDriver on the product detail page
     * @return {@code true} if "Add to Cart" was successfully clicked,
     *         {@code false} if the button could not be located
     */
    public static boolean addToCart(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, MEDIUM);

        // Primary add-to-cart button selectors
        By addToCartBy = By.cssSelector(
            "#add-to-cart-button, " +
            "input[name='submit.add-to-cart'], " +
            "#submit\\.add-to-cart, " +
            "[data-feature-id='add-to-cart'] input[type='submit']"
        );

        try {
            WebElement addBtn = wait.until(
                ExpectedConditions.elementToBeClickable(addToCartBy)
            );

            // Scroll into view and click via JS to avoid element-not-interactable
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", addBtn);
            sleep(300);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

            // Wait for any confirmation signal (side-panel, cart count change, etc.)
            new WebDriverWait(driver, MEDIUM).until(
                ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(
                        By.id("attachSiNoCoverage-announce")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("[data-feature-id='add-to-cart-confirmation']")),
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("#sw-atc-confirmation, #huc-v2-order-row-confirm-text")),
                    ExpectedConditions.urlContains("/cart")
                )
            );
            return true;

        } catch (TimeoutException e) {
            System.out.println("  ⚠  'Add to Cart' button not found or confirmation not received.");
            return false;
        }
    }

    /**
     * Returns the current cart count badge text from the nav bar.
     *
     * @param driver Any page on Amazon.com
     * @return Cart count string, or "?" if unavailable
     */
    public static String getCartCount(WebDriver driver) {
        try {
            WebElement badge = driver.findElement(By.id("nav-cart-count"));
            String text = badge.getText().trim();
            if (text.isEmpty()) {
                text = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent;", badge);
                if (text != null) text = text.trim();
            }
            return text != null ? text : "?";
        } catch (Exception e) {
            return "?";
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

    /** Millisecond sleep – use sparingly, only where truly needed. */
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
