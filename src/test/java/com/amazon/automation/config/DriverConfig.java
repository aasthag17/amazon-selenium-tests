package com.amazon.automation.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

/**
 * DriverConfig
 * ─────────────────────────────────────────────────────────────────────────────
 * Central factory for creating WebDriver instances.
 *
 * • Local mode  → spins up a headless ChromeDriver via WebDriverManager.
 * • LambdaTest  → connects to LT's W3C-compliant RemoteWebDriver grid.
 *
 * Mode is controlled by the system property  RUN_ON_LT=true
 * or the environment variable              RUN_ON_LT=true
 *
 * LambdaTest credentials are loaded (in priority order) from:
 *   1. System properties   -DLT_USERNAME=... -DLT_ACCESS_KEY=...
 *   2. Environment vars    LT_USERNAME / LT_ACCESS_KEY
 *   3. .env file           in the project root
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DriverConfig {

    // LambdaTest Hub URL (W3C)
    private static final String LT_HUB =
            "https://%s:%s@hub.lambdatest.com/wd/hub";

    // ── Dotenv (ignore missing file so env-var-only setups work too) ──────
    private static final Dotenv dotenv = loadDotenv();

    private static Dotenv loadDotenv() {
        try {
            return Dotenv.configure().ignoreIfMissing().load();
        } catch (DotenvException e) {
            return Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
        }
    }

    // ── Public factory method ─────────────────────────────────────────────

    /**
     * Creates and returns a fully configured WebDriver instance.
     * Thread-safe: each call produces an independent driver, so TestNG
     * parallel threads each call this from their own @BeforeMethod.
     */
    public static WebDriver createDriver() {
        boolean runOnLT = resolveRunOnLT();
        return runOnLT ? buildLambdaTestDriver() : buildLocalDriver();
    }

    // ── Local Chrome driver ───────────────────────────────────────────────

    private static WebDriver buildLocalDriver() {
        System.out.println("  [DriverConfig] Launching local ChromeDriver …");
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // ── Window & display ────────────────────────────────────────────
        options.addArguments("--start-maximized");
        options.addArguments("--window-size=1920,1080");

        // ── Anti-bot / stealth flags ────────────────────────────────────
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        // Real Chrome 124 user-agent – avoids bot fingerprinting
        options.addArguments(
            "--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Safari/537.36"
        );
        // Exclude the automation switch from navigator.webdriver
        options.setExperimentalOption("excludeSwitches",
            java.util.List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // ── Stability ───────────────────────────────────────────────────
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));  // explicit waits only
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return driver;
    }

    // ── LambdaTest remote driver ──────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static WebDriver buildLambdaTestDriver() {
        String username  = resolve("LT_USERNAME");
        String accessKey = resolve("LT_ACCESS_KEY");

        if (username == null || username.isBlank() ||
            accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                "[DriverConfig] LT_USERNAME or LT_ACCESS_KEY is not set.\n" +
                "Set them via -DLT_USERNAME=... -DLT_ACCESS_KEY=... or in .env"
            );
        }

        System.out.println("  [DriverConfig] Connecting to LambdaTest cloud as: " + username);

        // ── W3C capabilities ────────────────────────────────────────────
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName",  "Chrome");
        caps.setCapability("browserVersion", "latest");

        // LambdaTest-specific options
        java.util.Map<String, Object> ltOptions = new java.util.HashMap<>();
        ltOptions.put("username",    username);
        ltOptions.put("accessKey",   accessKey);
        ltOptions.put("platformName", "Windows 10");
        ltOptions.put("build",        "Amazon Automation – TestMu AI Assignment");
        ltOptions.put("project",      "Amazon Selenium Java");
        ltOptions.put("video",        true);
        ltOptions.put("network",      true);
        ltOptions.put("console",      true);
        ltOptions.put("selenium_version", "4.0.0");
        ltOptions.put("w3c", true);
        caps.setCapability("LT:Options", ltOptions);

        String hubURL = String.format(LT_HUB, username, accessKey);
        try {
            // Use ClientConfig with extended timeouts (free LT accounts need longer waits)
            org.openqa.selenium.remote.http.ClientConfig clientConfig =
                org.openqa.selenium.remote.http.ClientConfig.defaultConfig()
                    .connectionTimeout(Duration.ofMinutes(3))
                    .readTimeout(Duration.ofMinutes(10));

            org.openqa.selenium.remote.HttpCommandExecutor executor =
                new org.openqa.selenium.remote.HttpCommandExecutor(
                    Collections.emptyMap(), new URL(hubURL), clientConfig);

            WebDriver driver = new RemoteWebDriver(executor, caps);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("[DriverConfig] Invalid LambdaTest hub URL: " + hubURL, e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Resolves a key from (1) system properties, (2) env vars, (3) .env file. */
    private static String resolve(String key) {
        String val = System.getProperty(key);
        if (val != null && !val.isBlank()) return val;
        val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;
        return dotenv.get(key, null);
    }

    private static boolean resolveRunOnLT() {
        String val = resolve("RUN_ON_LT");
        return "true".equalsIgnoreCase(val);
    }
}
