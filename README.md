# Amazon Selenium Automation – TestMu AI (LambdaTest) Assignment
**Aastha Gupta**

Automated test suite using **Java + Selenium WebDriver + TestNG** that:
- **TC1**: Searches Amazon for an iPhone, adds to cart, prints price to console
- **TC2**: Searches Amazon for a Samsung Galaxy device, adds to cart, prints price to console
- Both tests run **in parallel** simultaneously

---

## Prerequisites

- Java 11 or higher (`java -version`)
- Google Chrome (latest)
- Internet connection

> Maven is **bundled** in the repo – no installation needed.

---

## How to Run

### Run TC1 + TC2 in Parallel (Local)

```bash
./run.sh
```

### Run a single test

```bash
# iPhone only
./apache-maven-3.9.6/bin/mvn test -Dtest=TC1_IphoneTest

# Galaxy only
./apache-maven-3.9.6/bin/mvn test -Dtest=TC2_GalaxyTest
```

---

## Expected Console Output

```
═════════════════════════════════════════════════════════════════
  TEST CASE 1 – iPhone
═════════════════════════════════════════════════════════════════
  Step 1: Searching Amazon for 'Apple iPhone 15' …
  Search results loaded.
  Step 2: Opening first eligible iPhone product …
  Product : "Apple iPhone 15, 128GB, Black - Unlocked"
  Step 3: Extracting price …
  ───────────────────────────────────────────────────────────────
  iPhone Price  :  $699.00
  ───────────────────────────────────────────────────────────────
  Step 4: Adding to cart …
  Added to cart!

═════════════════════════════════════════════════════════════════
  TC1 SUMMARY – iPhone
  ───────────────────────────────────────────────────────────────
  Product : Apple iPhone 15, 128GB, Black - Unlocked
  Price   : $699.00
  In Cart : Yes 
  Cart    : 1
═════════════════════════════════════════════════════════════════
```

*(TC2 output appears interleaved since both run at the same time.)*

---

## Bonus: Run on LambdaTest Cloud

### Step 1 – Sign up
Go to [lambdatest.com](https://www.lambdatest.com) → Create free account

### Step 2 – Get credentials
Go to **Profile → Account Settings** → copy your **Username** and **Access Key**

### Step 3 – Run

```bash
./apache-maven-3.9.6/bin/mvn test -P lambdatest \
  -DLT_USERNAME=your_actual_username \
  -DLT_ACCESS_KEY=your_actual_access_key
```

After the run, view video recordings at [app.lambdatest.com/automation](https://app.lambdatest.com/automation)

> **Note:** Replace `your_actual_username` and `your_actual_access_key` with your real LambdaTest credentials. Using placeholder values will cause a 401 error.

---

## Tech Stack

| | |
|---|---|
| Language | Java 11 |
| Automation | Selenium WebDriver 4.18 |
| Test Runner | TestNG 7.9 |
| Driver Manager | WebDriverManager 5.8 |
| Build Tool | Maven 3.9.6 (bundled) |
