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


## Tech Stack

| | |
|---|---|
| Language | Java 11 |
| Automation | Selenium WebDriver 4.18 |
| Test Runner | TestNG 7.9 |
| Driver Manager | WebDriverManager 5.8 |
| Build Tool | Maven 3.9.6 (bundled) |
