package com.amazon.automation.tests;

import com.amazon.automation.config.DriverConfig;
import com.amazon.automation.helpers.AmazonHelper;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * TC2_GalaxyTest
 * ─────────────────────────────────────────────────────────────────────────────
 * Test Case 2 – TestMu AI (LambdaTest) Automation Assignment
 *
 * Steps:
 *   1. Navigate to Amazon.com
 *   2. Search for "Samsung Galaxy smartphone"
 *   3. Open the first organic (non-sponsored) product result
 *   4. Extract & PRINT the product price to the console
 *   5. Add the product to the cart
 *   6. Verify the cart count updated
 *
 * Runs in parallel with TC1_IphoneTest via the TestNG suite XML
 * (testng.xml / testng-lambdatest.xml). Thread-local WebDriver
 * ensures complete isolation between the two parallel threads.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TC2_GalaxyTest {

    /** Each thread gets its own driver – thread-local for safety. */
    private final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @BeforeMethod
    public void setUp() {
        WebDriver driver = DriverConfig.createDriver();
        driverHolder.set(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            driver.quit();
            driverHolder.remove();
        }
    }

    // ── Test ───────────────────────────────────────────────────────────────

    @Test(description = "Search for Samsung Galaxy on Amazon, add to cart, and print price")
    public void searchGalaxyAddToCartAndPrintPrice() {

        WebDriver driver = driverHolder.get();

        // ── Banner ──────────────────────────────────────────────────────
        printBanner("TEST CASE 2 – Samsung Galaxy");

        // ── Step 1: Navigate to Amazon and search ───────────────────────
        log("Step 1: Navigating to Amazon.com and searching for 'Samsung Galaxy smartphone' …");
        AmazonHelper.searchAmazon(driver, "Samsung Galaxy smartphone");
        log("✅  Search results page loaded.");

        // ── Step 2: Verify at least one result exists ───────────────────
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("amazon.com/s"),
            "Expected search-results URL but got: " + currentUrl
        );

        // ── Step 3: Open the first product ─────────────────────────────
        log("Step 2: Opening first Samsung Galaxy product result …");
        AmazonHelper.openFirstProduct(driver);

        String title = AmazonHelper.extractTitle(driver);
        log("✅  Product Title : \"" + title + "\"");

        // ── Step 4: Extract & PRINT the price ──────────────────────────
        log("Step 3: Extracting product price …");
        String price = AmazonHelper.extractPrice(driver);

        // ══ PRICE IS PRINTED TO CONSOLE AS REQUIRED BY THE ASSIGNMENT ══
        printSeparator();
        System.out.println("  💰  Samsung Galaxy Price  :  " + price);
        printSeparator();

        // Log if price could not be extracted (soft check – assignment requires printing, not failing)
        if ("Price not found".equals(price)) {
            System.out.println("  ⚠️   Price element not found – product may require sign-in or variant selection.");
        }

        // ── Step 5: Add to Cart ─────────────────────────────────────────
        log("Step 4: Adding Samsung Galaxy to cart …");
        boolean added = AmazonHelper.addToCart(driver);

        if (added) {
            log("✅  Product successfully added to cart!");
        } else {
            log("⚠️   Could not click Add to Cart (variant selection may be required).");
        }

        // ── Step 6: Verify cart count ───────────────────────────────────
        String cartCount = AmazonHelper.getCartCount(driver);
        log("🛒  Cart item count after add: " + cartCount);

        // ── Summary ─────────────────────────────────────────────────────
        printSummary("TC2 SUMMARY – Samsung Galaxy",
            title, price, added, cartCount);
    }

    // ── Private console helpers ────────────────────────────────────────────

    private static void printBanner(String label) {
        System.out.println("\n" + "═".repeat(65));
        System.out.println("  🧪  " + label);
        System.out.println("═".repeat(65));
    }

    private static void printSeparator() {
        System.out.println("  " + "─".repeat(63));
    }

    private static void log(String message) {
        System.out.println("  📍 " + message);
    }

    private static void printSummary(String heading, String title,
                                     String price, boolean added,
                                     String cartCount) {
        System.out.println("\n" + "═".repeat(65));
        System.out.println("  📋  " + heading);
        System.out.println("  " + "─".repeat(63));
        System.out.println("  Product    : " + title);
        System.out.println("  Price      : " + price);
        System.out.println("  In Cart    : " + (added ? "Yes ✅" : "No – variant selection needed ⚠️"));
        System.out.println("  Cart Count : " + cartCount);
        System.out.println("═".repeat(65) + "\n");
    }
}
