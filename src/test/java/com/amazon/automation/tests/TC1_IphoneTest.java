package com.amazon.automation.tests;

import com.amazon.automation.config.DriverConfig;
import com.amazon.automation.helpers.AmazonHelper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * TC1 - iPhone Search, Add to Cart, Print Price.
 * Runs in parallel with TC2_GalaxyTest via testng.xml / testng-lambdatest.xml.
 */
public class TC1_IphoneTest {

    private final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    @BeforeMethod
    public void setUp() {
        driverHolder.set(DriverConfig.createDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver d = driverHolder.get();
        if (d != null) {
            // Report pass/fail back to LambdaTest dashboard
            try {
                String status = result.isSuccess() ? "passed" : "failed";
                String reason = result.isSuccess()
                    ? "TC1 completed successfully"
                    : (result.getThrowable() != null
                        ? result.getThrowable().getMessage() : "TC1 failed");
                ((JavascriptExecutor) d).executeScript(
                    "lambda-status=" + status + ";lambda-status-reason=" + reason);
            } catch (Exception ignored) {}
            d.quit();
            driverHolder.remove();
        }
    }

    @Test(description = "Search iPhone on Amazon, add to cart, print price")
    public void searchIphoneAddToCartAndPrintPrice() {
        WebDriver driver = driverHolder.get();

        printBanner("TEST CASE 1 - iPhone");

        // Step 1: Search
        log("Step 1: Searching Amazon for 'Apple iPhone 15'...");
        AmazonHelper.searchAmazon(driver, "Apple iPhone 15");
        log("Search results loaded.");

        // Step 2: Open first product
        log("Step 2: Opening first iPhone product...");
        AmazonHelper.openFirstProduct(driver);

        String title = AmazonHelper.extractTitle(driver);
        log("Product: \"" + title + "\"");

        // Step 3: Print price to console  (required by assignment)
        log("Step 3: Extracting price...");
        String price = AmazonHelper.extractPrice(driver);
        separator();
        System.out.println("  iPhone Price: " + price);
        separator();

        if ("Price not found".equals(price)) {
            System.out.println("  [INFO] Price not visible on this listing.");
        }

        // Step 4: Add to cart (auto-handles variant selection if needed)
        log("Step 4: Adding to cart...");
        boolean added = AmazonHelper.addToCart(driver);
        log(added ? "Added to cart successfully." : "Could not add to cart.");

        String cartCount = AmazonHelper.getCartCount(driver);
        log("Cart count: " + cartCount);

        // Summary
        printSummary("TC1 SUMMARY - iPhone", title, price, added, cartCount);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void printBanner(String label) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  " + label);
        System.out.println("=".repeat(65));
    }

    private static void separator() {
        System.out.println("  " + "-".repeat(63));
    }

    private static void log(String msg) {
        System.out.println("  [TC1] " + msg);
    }

    private static void printSummary(String header, String title, String price,
                                     boolean added, String cart) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  " + header);
        System.out.println("  " + "-".repeat(63));
        System.out.println("  Product : " + title);
        System.out.println("  Price   : " + price);
        System.out.println("  In Cart : " + (added ? "Yes" : "No"));
        System.out.println("  Cart    : " + cart);
        System.out.println("=".repeat(65) + "\n");
    }
}
