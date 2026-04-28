package com.amazon.automation.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * AmazonHelper – thread-safe, stateless helper for Amazon.com interactions.
 * Strategy: navigate directly to /s?k=<query> (bypasses homepage bot-check).
 * Tries up to 3 products from results to find one with a price + Add to Cart.
 */
public class AmazonHelper {

    private static final Duration SHORT  = Duration.ofSeconds(8);
    private static final Duration MEDIUM = Duration.ofSeconds(25);
    private static final Duration LONG   = Duration.ofSeconds(45);

    // ── Search ────────────────────────────────────────────────────────────

    public static void searchAmazon(WebDriver driver, String query) {
        removeWebdriverFlag(driver);
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.amazon.com/s?k=" + encoded;
        System.out.println("  [AmazonHelper] Loading: " + url);
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

    // ── Smart product opener – tries up to 3 results ─────────────────────

    /**
     * Clicks the first organic product result on the search page using JS.
     * Preserves the Referer header chain so Amazon doesn't flag it as a bot.
     */
    public static void openFirstProduct(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, LONG);

        // Wait for result cards
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("[data-component-type='s-search-result']")));
        removeWebdriverFlag(driver);

        // Try selectors in order of preference (no class restriction – Amazon changes classes)
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
            "No product links on: " + driver.getCurrentUrl());

        System.out.println("  [AmazonHelper] Clicking first result …");
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", target);
        sleep(300);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("productTitle")));
        removeWebdriverFlag(driver);
        dismissLocationPopup(driver);
    }

    // ── Data Extraction ───────────────────────────────────────────────────

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
                        if (text.contains("$") || text.contains("₹") ||
                            text.contains("INR") || text.matches(".*\\d+[,.]\\d{2}.*")) {
                            return text;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Last resort: grab whole + fraction
        try {
            String w = (String) ((JavascriptExecutor) driver).executeScript(
                "var w=document.querySelector('.a-price-whole');" +
                "var f=document.querySelector('.a-price-fraction');" +
                "return w ? '$'+(w.textContent+(f?f.textContent:'')).trim() : null;");
            if (w != null && !w.isEmpty()) return w;
        } catch (Exception ignored) {}

        return "Price not found";
    }

    // ── Cart ──────────────────────────────────────────────────────────────

    public static boolean addToCart(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, MEDIUM);
        By btn = By.cssSelector(
            "#add-to-cart-button, input[name='submit.add-to-cart'], " +
            "#submit\\.add-to-cart, [data-feature-id='add-to-cart'] input[type='submit']");

        try {
            WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(btn));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", addBtn);
            sleep(300);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

            new WebDriverWait(driver, MEDIUM).until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("attachSiNoCoverage-announce")),
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[data-feature-id='add-to-cart-confirmation']")),
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("#sw-atc-confirmation, #huc-v2-order-row-confirm-text")),
                ExpectedConditions.urlContains("/cart")
            ));
            return true;
        } catch (TimeoutException e) {
            System.out.println("  ⚠  Add to Cart not available on this product.");
            return false;
        }
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
