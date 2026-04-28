# 🛒 Amazon Selenium Automation TestMu AI (LambdaTest) Assignment - Aastha Gupta

> **Automated test suite using Java + Selenium WebDriver + TestNG**  
> Searches Amazon.com for an **iPhone** and a **Samsung Galaxy** device, adds each to cart, and **prints the price to the console** — both tests running **in parallel**.

---

## Test Cases

| # | Description | Search Term | Parallel? |
|---|-------------|-------------|-----------|
| TC1 | Navigate to Amazon → search iPhone → add to cart → print price | `iPhone` |  Yes |
| TC2 | Navigate to Amazon → search Galaxy → add to cart → print price | `Samsung Galaxy smartphone` | Yes |

Parallelism is configured via **TestNG suite XML** (`testng.xml`) using `parallel="tests" thread-count="2"`. Each test gets its own **isolated `WebDriver` instance** via `ThreadLocal`, so they never share browser state.

---

##  Project Structure

```
amazon-selenium-tests/
├── pom.xml                          # Maven dependencies & build config
├── testng.xml                       # Local parallel execution suite
├── testng-lambdatest.xml            # LambdaTest cloud parallel suite
├── .env.example                     # LambdaTest credentials template
├── .gitignore
├── README.md
└── src/
    └── test/
        └── java/
            └── com/amazon/automation/
                ├── config/
                │   └── DriverConfig.java      # WebDriver factory (local + LT cloud)
                ├── helpers/
                │   └── AmazonHelper.java      # Reusable Amazon page interactions
                └── tests/
                    ├── TC1_IphoneTest.java    # Test Case 1 – iPhone
                    └── TC2_GalaxyTest.java    # Test Case 2 – Galaxy
```

---

## Prerequisites

| Tool | Minimum Version | Check Command |
|------|----------------|---------------|
| Java (JDK) | 11+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Google Chrome | Latest | — |

> **ChromeDriver** is downloaded automatically by **WebDriverManager** — you do NOT need to install it manually.

---

## Quick Start (Local Parallel Execution)

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/amazon-selenium-tests.git
cd amazon-selenium-tests
```

### 2. Install dependencies
```bash
mvn dependency:resolve
```

### 3. Run both tests in parallel
```bash
mvn test
```

That's it! Maven reads `testng.xml`, which launches **TC1 and TC2 simultaneously** in two parallel Chrome windows.

---

## Expected Console Output

When the tests run, you will see price output like:

```
═════════════════════════════════════════════════════════════
  🧪  TEST CASE 1 – iPhone
═════════════════════════════════════════════════════════════
  Step 1: Navigating to Amazon.com and searching for 'iPhone' …
  Search results page loaded.
  Step 2: Opening first iPhone product result …
  Product Title : "Apple iPhone 15 (128 GB) - Black"
  Step 3: Extracting product price …
  ───────────────────────────────────────────────────────────────
    iPhone Price  :  $799.00
  ───────────────────────────────────────────────────────────────
   Step 4: Adding iPhone to cart …
    Product successfully added to cart!
    Cart item count after add: 1

═════════════════════════════════════════════════════════════
   TC1 SUMMARY – iPhone
  ─────────────────────────────────────────────────────────
  Product    : Apple iPhone 15 (128 GB) - Black
  Price      : $799.00
  In Cart    : Yes 
  Cart Count : 1
═════════════════════════════════════════════════════════════
```

*(TC2 Galaxy output appears interleaved, since both run simultaneously.)*

---

##  Running Individual Test Cases

Run only TC1 (iPhone):
```bash
mvn test -Dtest=TC1_IphoneTest
```

Run only TC2 (Galaxy):
```bash
mvn test -Dtest=TC2_GalaxyTest
```

---

##  Bonus: Run on LambdaTest Cloud

### Step 1 – Sign up at LambdaTest
Go to [https://www.lambdatest.com](https://www.lambdatest.com) and create a free account.

### Step 2 – Get your credentials
Navigate to **Profile → Account Settings** (or visit [accounts.lambdatest.com/detail/profile](https://accounts.lambdatest.com/detail/profile)) and copy your **Username** and **Access Key**.

### Step 3 – Set credentials
**Option A – Environment variables (recommended):**
```bash
export LT_USERNAME=your_lambdatest_username
export LT_ACCESS_KEY=your_lambdatest_access_key
```

**Option B – .env file:**
```bash
cp .env.example .env
# Edit .env and fill in your real credentials
```

**Option C – Maven flags:**
```bash
mvn test -P lambdatest -DLT_USERNAME=your_username -DLT_ACCESS_KEY=your_access_key
```

### Step 4 – Run on cloud
```bash
mvn test -P lambdatest
```

Both tests will run **in parallel on LambdaTest's cloud grid** (Windows 10 / Chrome Latest). After the run, view the video recordings and logs at **[app.lambdatest.com/automation](https://app.lambdatest.com/automation)**.

---

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Java 11** | Primary language |
| **Selenium WebDriver 4.18** | Browser automation |
| **TestNG 7.9** | Test runner + parallel execution |
| **WebDriverManager 5.8** | Auto-manages ChromeDriver binaries |
| **dotenv-java** | Loads `.env` credentials for LambdaTest |
| **Maven** | Build & dependency management |

---

##  Parallel Execution Architecture

```
mvn test
    └── Surefire Plugin reads testng.xml
            └── TestNG Suite (parallel="tests", thread-count="2")
                    ├── Thread 1 → TC1_IphoneTest
                    │               └── @BeforeMethod → DriverConfig.createDriver()
                    │               └── @Test         → searchIphoneAddToCartAndPrintPrice()
                    │               └── @AfterMethod  → driver.quit()
                    │
                    └── Thread 2 → TC2_GalaxyTest
                                    └── @BeforeMethod → DriverConfig.createDriver()
                                    └── @Test         → searchGalaxyAddToCartAndPrintPrice()
                                    └── @AfterMethod  → driver.quit()
```

**Thread safety** is guaranteed by:
- `ThreadLocal<WebDriver>` in each test class — no shared driver instance
- All helper methods in `AmazonHelper` are stateless (receive driver as parameter)
- `DriverConfig.createDriver()` is stateless — each call returns a brand-new driver

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `ChromeDriver not found` | Run `mvn dependency:resolve` — WebDriverManager handles it automatically |
| `Price not found` in output | Amazon's layout varies by region/session; the test will still pass if any price format is detected |
| Add to Cart button not clicked | Some products require colour/storage variant selection first; the test logs a warning and continues |
| LambdaTest `401 Unauthorized` | Double-check `LT_USERNAME` and `LT_ACCESS_KEY` values |
| Tests run sequentially, not parallel | Ensure you're running `mvn test` (not `mvn test -Dtest=...`) so the suite XML is used |

---

## Author

**Aastha Gupta**  
Customer Engineering Intern Assignment – TestMu AI (LambdaTest)
