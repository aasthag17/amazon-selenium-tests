package com.amazon.automation.tests;

import com.amazon.automation.config.DriverConfig;
import com.amazon.automation.helpers.AmazonHelper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * TC2 - Samsung Galaxy Search, Add to Cart, Print Price.
 * Runs in parallel with TC1_IphoneTest via testng.xml / testng-lambdatest.xml.
 */
public class TC2_GalaxyTest {

    private final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    @BeforeMethod
    public void setUp() {
        driverHolder.set(DriverConfig.createDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver d = driverHolder.get();
        if (d != null) {
            try {
                String status = result.isSuccess() ? "passed" : "failed";
                String reason = result.isSuccess()
                    ? "TC2 Galaxy test passed"
                    : (result.getThrowable() != null
                        ? result.getThrowable().getMessage() : "TC2 failed");
                ((JavascriptExecutor) d).executeScript(
                    "lambda-status=" + status + ";lambda-status-reason=" + reason);
            } catch (Exception ignored) {}
            d.quit();
            driverHolder.remove();
        }
    }

    @Test(description = "Search Samsung Galaxy on Amazon, add to cart, print price")
    public void searchGalaxyAddToCartAndPrintPrice() {
        WebDriver driver = driverHolder.get();

        printBanner("TEST CASE 2 - Samsung Galaxy");

        // Step 1: Search
        log("Step 1: Searching Amazon for 'Samsung Galaxy smartphone'...");
        AmazonHelper.searchAmazon(driver, "Samsung Galaxy smartphone");

        // Step 2: Open first product, set US delivery ZIP
        log("Step 2: Opening best available Galaxy product...");
        AmazonHelper.openFirstProduct(driver, false);

        String title = AmazonHelper.extractTitle(driver);
        log("Product: " + title);

        // Step 3: Print price to console  (required by assignment)
        log("Step 3: Extracting price...");
        String price = AmazonHelper.extractPrice(driver);
        separator();
        System.out.println("  Samsung Galaxy Price: " + price);
        separator();

        // Step 4: Add to cart
        log("Step 4: Adding to cart...");
        boolean added = AmazonHelper.addToCart(driver);
        log(added ? "Added to cart successfully." : "Could not add to cart on this listing.");

        String cartCount = AmazonHelper.getCartCount(driver);
        log("Cart count: " + cartCount);

        printSummary("TC2 SUMMARY - Samsung Galaxy", title, price, added, cartCount);
    }

    private static void printBanner(String label) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  " + label);
        System.out.println("=".repeat(65));
    }

    private static void separator() {
        System.out.println("  " + "-".repeat(63));
    }

    private static void log(String msg) {
        System.out.println("  [TC2] " + msg);
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
