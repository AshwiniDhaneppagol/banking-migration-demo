# YaazhTech Associate Test Engineer Challenge — reference implementation

A small end-to-end demo of two independently runnable Spring Boot services (Version A and Version B) plus an automated cucumber BDD test runner. It uses synthetic banking data only.

## Architecture

- `version-a`: Spring Boot service, default port **8081**.
- `version-b`: separate Spring Boot service, default port **8082**.
- `test-suite`: loads the same JSON dataset into both services, verifies the load, calls the EOD APIs and compares responses by `transactionId` (not response order).
- `test-suite/report`: generated source-load JSON reports, consolidated comparison JSON/HTML and mismatch CSV.

Both services have isolated in-memory stores. This keeps the demo easy to run; data is cleared when a service restarts. For a production-like extension, replace the in-memory map with separate PostgreSQL databases or isolated schemas.

## Prerequisites

- JDK 17 or later (Java 21 is fine)
- Maven 3.9+ for building the source code and run it
- Spring Boot 3 framework for building HTTP Rest Web Services
- Cucumber plugin for running the cucumber test feature file within intellij

## 1. Start the two applications

Open two terminals from this project root:

```bash
mvn -pl version-a spring-boot:run
```

```bash
mvn -pl version-b spring-boot:run
```

Check health:

```bash
curl http://localhost:8081/api/v1/health
curl http://localhost:8082/api/v1/health
```

## 2. Run the Complete Automated Test Suite

Open a terminal in the `test-suite` directory and execute:

```bash
mvn test
```

This runs all Cucumber scenarios, including:

- Bulk loading transactions into both applications
- Rejecting invalid transactions with a missing transaction ID
- Comparing identical EOD transaction responses
- Detecting missing and extra transactions
- Detecting transaction amount mismatches
- Detecting currency mismatches
- Validating required response fields
- Validating field data types and nullable fields
- Verifying date filtering and empty results

### Run Specific Test Groups

To run only the bulk-load scenarios:

```bash
mvn test -Dcucumber.filter.tags="@bulkload"
```

To run only the negative comparison scenarios:

```bash
mvn test -Dcucumber.filter.tags="@negative"
```

To run only the positive comparison scenario:

```bash
mvn test -Dcucumber.filter.tags="@positive"
```

### Test Data

The synthetic banking transaction dataset is stored in:

```text
test-suite/data/transactions.json
```

The dataset contains 10 transactions across the following value dates:

| Value Date | Expected Records |
|---|---:|
| 2026-11-12 | 5 |
| 2026-11-13 | 3 |
| 2026-11-14 | 2 |
| 2026-11-15 | 0 |

The automated bulk-load scenario submits the dataset to both applications. The negative comparison scenarios temporarily introduce data differences in Version B and restore the original records after each scenario.

**Important:** Both applications use in-memory storage. Keep them running during test execution. Restarting an application clears its data, so load the dataset again before running comparison tests.

### Test Reports

After execution, the Cucumber HTML report is generated at:

```text
test-suite/target/cucumber-reports/cucumber.html
```

The automated bulk-load metrics report is generated at:

```text
test-suite/report/bulk-load/automated-bulk-load-report.html
```

Additional saved test reports and bulk-load JSON results are available under:

```text
test-suite/report/
```

### Expected Test Result

A successful complete test execution should finish with:

```text
BUILD SUCCESS
```

The Cucumber report displays the number of passed and failed scenarios, execution duration and test details.

## 3. API contract

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/health` | Health check |
| POST | `/api/v1/transactions/bulk` | Bulk-load `{ "transactions": [...] }` |
| GET | `/api/v1/eod-transactions?valueDate=YYYY-MM-DD` | EOD transactions for one value date |
| PUT | `/api/v1/transactions/{id}` | Test-support endpoint to replace a transaction for negative testing |
| DELETE | `/api/v1/transactions/{id}` | Test-support endpoint to remove a transaction for negative testing |

The bulk endpoint returns input, inserted, duplicate and rejected counts. The EOD endpoint returns `valueDate`, `recordCount` and `transactions`.

## 4. Comparison rules and assumptions

1. `transactionId` is the stable business key. Ordering is ignored.
2. Missing and additional records are reported separately.
3. Schema differences (missing/additional fields and incompatible JSON types) are reported separately from value mismatches.
4. JSON numeric values compare numerically (`10` and `10.00` are equivalent); strings are case- and whitespace-sensitive.
5. `null` is a real value. A missing JSON property is a schema difference, not the same as explicit null.
6. A failed API call is an execution failure and cannot produce PASS.
7. The sample loader verifies the default date in its source-load report; the A/B runner can compare any requested date.
8. The demo stores records in memory and treats an existing transaction ID as a duplicate. It does not update an existing record during bulk load.
9. The services are deliberately small test doubles, not real banking systems. Authentication, persistent storage, pagination, concurrency/load testing and full field-level business validation are outside this demo's scope.

## 5 .Demonstrating negative scenarios

First load data normally. Then use one of the following examples against Version B and run the comparison again with `--skip-load`.

**Value mismatch:** get a transaction from the EOD response, change its amount, then PUT the complete changed transaction to `/api/v1/transactions/TXN001`. The comparison should identify `TXN001`, field `amount`, and both values.

**Missing record:** `curl -X DELETE http://localhost:8082/api/v1/transactions/TXN002`

**Additional record:** POST one new transaction only to Version B using `/api/v1/transactions/bulk`, with `valueDate` equal to the date being compared.

**API failure:** stop Version B and run the comparison. The runner should report FAIL due to API execution failure rather than a false PASS.

**Value-date filter:** compare `2026-11-13`; only records for that date should be returned.

For a clean positive run after negative testing, restart both services and run the normal command again (their in-memory stores will be empty).

## 6. Reports

- `source-load-version a.json` and `source-load-version b.json`: run metadata, counts, verification and outcome.
- `ab-comparison.json`: API execution, record summary, schema differences, data mismatches and overall result.
- `ab-comparison.html`: readable stakeholder summary.
- `ab-mismatches.csv`: one row per data/schema mismatch for spreadsheet review.

## 7. Known limitations / next improvements
- Add JDBC database persistence with two separate databases or schemas.
- Add unit tests for comparison utilities and integration tests for each endpoint.
- Expand input validation (currency code, debit/credit enum, amount precision, date relationships).
