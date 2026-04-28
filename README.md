# Amazon Selenium Automation – TestMu AI (LambdaTest) Assignment
**Aastha Gupta**

Automated test suite using **Java + Selenium WebDriver + TestNG** that:
- **Test Case 1**: Navigates to Amazon.com, searches for an iPhone, adds it to the cart, and prints the device price to the console.
- **Test Case 2**: Navigates to Amazon.com, searches for a Galaxy device, adds it to the cart, and prints the device price to the console.
- **Parallel Execution**: Both tests are configured to run in parallel simultaneously.
- **Cloud Execution**: Bonus points achieved by integrating with LambdaTest Cloud.

---

## Prerequisites

- Java 11 or higher (`java -version`)
- Google Chrome (latest)
- Internet connection

> **Note:** Maven is **bundled** in the repository – no external Maven installation is needed!

---

## How to Run Locally

To execute Test Case 1 and Test Case 2 in parallel on your local machine, simply run the provided shell script from the project root:

```bash
./run.sh
```

### Expected Local Console Output

You will see the steps interleaved, and eventually both prices printed:

```
  💰  Samsung Galaxy Price  :  INR62,445.61
  💰  iPhone Price  :  INR54,782.66
```


## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 11 |
| Automation | Selenium WebDriver 4.18 |
| Test Runner | TestNG 7.9 |
| Cloud Grid | LambdaTest W3C |
| Build Tool | Maven 3.9.6 (bundled) |
