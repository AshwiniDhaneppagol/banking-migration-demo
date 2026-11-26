
package com.yaazhtech.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.nio.file.Files;
import java.nio.file.Path;


import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class BulkLoadSteps {

    private static final String VERSION_A =
            "http://localhost:8081/api/v1";

    private static final String VERSION_B =
            "http://localhost:8082/api/v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Response responseA;
    private Response responseB;
    private int expectedInputCount;

    private long responseTimeA;
    private long responseTimeB;

    private LocalDateTime reportTime;

    @When("I bulk load the transaction dataset into both applications")
    public void bulkLoadDataset() throws Exception {

        // Read the existing 10-transaction JSON file
        JsonNode transactions = objectMapper.readTree(
                Files.readString(Path.of("data", "transactions.json"))
        );

        expectedInputCount = transactions.size();

        // Wrap the JSON array in the format expected by the API
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.set("transactions", transactions);

        // Upload to Version A
        long startA = System.currentTimeMillis();

        responseA = given()
                .contentType("application/json")
                .body(requestBody.toString())
                .post(VERSION_A + "/transactions/bulk");

        responseTimeA = System.currentTimeMillis() - startA;

        // Upload to Version B
        long startB = System.currentTimeMillis();

        responseB = given()
                .contentType("application/json")
                .body(requestBody.toString())
                .post(VERSION_B + "/transactions/bulk");

        responseTimeB = System.currentTimeMillis() - startB;
    }

    @Then("both applications should process all input transactions")
    public void validateBulkLoadResults() {

        assertEquals(200, responseA.statusCode(),
                "Version A HTTP status should be 200");

        assertEquals(200, responseB.statusCode(),
                "Version B HTTP status should be 200");

        validateCounts(responseA, "Version A");
        validateCounts(responseB, "Version B");
    }

    private void validateCounts(Response response, String version) {

        int input = response.jsonPath().getInt("inputCount");
        int inserted = response.jsonPath().getInt("insertedCount");
        int duplicates = response.jsonPath().getInt("duplicateCount");
        int rejected = response.jsonPath().getInt("rejectedCount");

        assertEquals(expectedInputCount, input,
                version + " input count is incorrect");

        assertEquals(input, inserted + duplicates + rejected,
                version + " processing counts do not add up");

        assertEquals(0, rejected,
                version + " unexpectedly rejected valid transactions");
    }

    @Then("both bulk load results should be displayed")
    public void displayBulkLoadResults() throws Exception {

        System.out.println("===== BULK LOAD RESULTS =====");

        System.out.println("Version A: " + responseA.asString());

        System.out.println("Version B: " + responseB.asString());

        generateBulkLoadReport();
    }

    @When("I submit an invalid transaction without a transaction ID")
    public void submitInvalidTransaction() throws Exception {

        String invalidJson = """
                {
                  "transactions": [
                    {
                      "accountId": "ACC010",
                      "valueDate": "2026-11-24",
                      "bookingDate": "2026-11-24",
                      "currency": "EUR",
                      "amount": 100.00,
                      "debitCreditIndicator": "CREDIT",
                      "transactionType": "TRANSFER",
                      "description": "Invalid transaction",
                      "status": "COMPLETED"
                    }
                  ]
                }
                """;

        responseA = given()
                .contentType("application/json")
                .body(invalidJson)
                .post(VERSION_A + "/transactions/bulk");

        responseB = given()
                .contentType("application/json")
                .body(invalidJson)
                .post(VERSION_B + "/transactions/bulk");
    }

    @Then("Version A should reject the invalid transaction")
    public void validateVersionARejection() {
        validateRejection(responseA, "Version A");
    }

    @Then("Version B should reject the invalid transaction")
    public void validateVersionBRejection() {
        validateRejection(responseB, "Version B");
    }

    private void validateRejection(Response response, String version) {

        assertEquals(200, response.statusCode());

        assertEquals(1, response.jsonPath().getInt("inputCount"));

        assertEquals(1, response.jsonPath().getInt("rejectedCount"));

        assertEquals(0, response.jsonPath().getInt("insertedCount"));

        assertEquals(0, response.jsonPath().getInt("duplicateCount"));

        System.out.println(
                version + " correctly rejected the invalid transaction."
        );
    }

    private void generateBulkLoadReport() throws Exception {

        reportTime = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String reportDate = reportTime.format(formatter);

        String report = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Bulk Load Automation Report</title>
                    <style>
                                body { font-family: Arial; margin: 30px; }
                                table { border-collapse: collapse; width: 100%%; }
                                th, td { border: 1px solid #ccc; padding: 10px; }
                                th { background-color: #eeeeee; }
                                .pass { color: green; font-weight: bold; }
                            </style>
                </head>
                <body>
                    <h1>Bulk Load Automation Report</h1>
                    <p>Execution date: %s</p>
                
                    <h2>Version A</h2>
                    <table>
                        <tr><th>Metric</th><th>Result</th></tr>
                        <tr><td>HTTP Status</td><td>%d</td></tr>
                        <tr><td>Response Time (ms)</td><td>%d</td></tr>
                        <tr><td>Content Type</td><td>%s</td></tr>
                        <tr><td>Input Count</td><td>%d</td></tr>
                        <tr><td>Inserted Count</td><td>%d</td></tr>
                        <tr><td>Duplicate Count</td><td>%d</td></tr>
                        <tr><td>Rejected Count</td><td>%d</td></tr>
                    </table>
                
                    <h2>Version B</h2>
                    <table>
                        <tr><th>Metric</th><th>Result</th></tr>
                        <tr><td>HTTP Status</td><td>%d</td></tr>
                        <tr><td>Response Time (ms)</td><td>%d</td></tr>
                        <tr><td>Content Type</td><td>%s</td></tr>
                        <tr><td>Input Count</td><td>%d</td></tr>
                        <tr><td>Inserted Count</td><td>%d</td></tr>
                        <tr><td>Duplicate Count</td><td>%d</td></tr>
                        <tr><td>Rejected Count</td><td>%d</td></tr>
                    </table>
                
                    <h2>Test Outcome</h2>
                    <p class="pass">PASS - Both applications processed the dataset successfully.</p>
                </body>
                </html>
                """.formatted(
                reportDate,
                responseA.statusCode(),
                responseTimeA,
                responseA.contentType(),
                responseA.jsonPath().getInt("inputCount"),
                responseA.jsonPath().getInt("insertedCount"),
                responseA.jsonPath().getInt("duplicateCount"),
                responseA.jsonPath().getInt("rejectedCount"),
                responseB.statusCode(),
                responseTimeB,
                responseB.contentType(),
                responseB.jsonPath().getInt("inputCount"),
                responseB.jsonPath().getInt("insertedCount"),
                responseB.jsonPath().getInt("duplicateCount"),
                responseB.jsonPath().getInt("rejectedCount")
        );

        Path reportFolder = Path.of("report", "bulk-load");

        Files.createDirectories(reportFolder);

        Files.writeString(
                reportFolder.resolve("automated-bulk-load-report.html"),
                report
        );

        System.out.println("Bulk load HTML report generated successfully.");
    }
}
