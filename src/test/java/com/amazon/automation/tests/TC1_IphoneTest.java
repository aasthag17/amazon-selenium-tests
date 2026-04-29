package com.amazon.automation.tests;

import com.amazon.automation.config.DriverConfig;
import com.amazon.automation.helpers.AmazonHelper;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

/**
 * TC1 – iPhone Search, Add to Cart, Print Price
 * Parallel: runs simultaneously with TC2_GalaxyTest via testng.xml
 */
public class TC1_IphoneTest {

    private final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    @BeforeMethod
    public void setUp() {
        driverHolder.set(DriverConfig.createDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver d = driverHolder.get();
        if (d != null) { d.quit(); driverHolder.remove(); }
    }

    @Test(description = "Search iPhone on Amazon, add to cart, print price")
    public void searchIphoneAddToCartAndPrintPrice() {
        WebDriver driver = driverHolder.get();

        printBanner("TEST CASE 1 – iPhone");

        // Step 1: Search
        log("Step 1: Searching Amazon for 'Apple iPhone 15' …");
        AmazonHelper.searchAmazon(driver, "Apple iPhone 15");
        log(" Search results loaded.");

        // Step 2: Open best product (tries up to 3 results for price + ATC)
        log("Step 2: Opening first iPhone product …");
        AmazonHelper.openFirstProduct(driver);

        String title = AmazonHelper.extractTitle(driver);
        log(" Product : \"" + title + "\"");

        // Step 3: Print price to console  ← REQUIRED BY ASSIGNMENT
        log("Step 3: Extracting price …");
        String price = AmazonHelper.extractPrice(driver);
        sep();
        System.out.println("  iPhone Price  :  " + price);
        sep();

        if ("Price not found".equals(price))
            System.out.println("   Price hidden – may need sign-in.");

        // Step 4: Add to cart
        log("Step 4: Adding to cart …");
        boolean added = AmazonHelper.addToCart(driver);
        log(added ? " Added to cart!" : " Could not add (variant selection may be needed).");

        String cartCount = AmazonHelper.getCartCount(driver);
        log(" Cart count: " + cartCount);

        // Summary
        printSummary("TC1 SUMMARY – iPhone", title, price, added, cartCount);
    }

    private static void printBanner(String s) {
        System.out.println("\n" + "═".repeat(65));
        System.out.println("    " + s);
        System.out.println("═".repeat(65));
    }
    private static void sep()      { System.out.println("  " + "─".repeat(63)); }
    private static void log(String m) { System.out.println("   " + m); }
    private static void printSummary(String h, String title, String price,
                                      boolean added, String cart) {
        System.out.println("\n" + "═".repeat(65));
        System.out.println("    " + h);
        System.out.println("  " + "─".repeat(63));
        System.out.println("  Product : " + title);
        System.out.println("  Price   : " + price);
        System.out.println("  In Cart : " + (added ? "Yes " : "No "));
        System.out.println("  Cart    : " + cart);
        System.out.println("═".repeat(65) + "\n");
    }
}
