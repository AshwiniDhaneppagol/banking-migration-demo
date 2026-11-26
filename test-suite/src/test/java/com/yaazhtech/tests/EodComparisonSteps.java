
package com.yaazhtech.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

public class EodComparisonSteps {

    private Response responseA;
    private Response responseB;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String VERSION_A =
            "http://localhost:8081/api/v1";

    private static final String VERSION_B =
            "http://localhost:8082/api/v1";

    private static final String TEST_DATE = "2026-11-12";

    @Given("both banking applications are running")
    public void bothApplicationsAreRunning() {

        Response healthA = RestAssured.get(VERSION_A + "/health");
        Response healthB = RestAssured.get(VERSION_B + "/health");

        if (healthA.statusCode() != 200) {
            throw new AssertionError("Version A is not running");
        }

        if (healthB.statusCode() != 200) {
            throw new AssertionError("Version B is not running");
        }

        System.out.println("Both banking applications are running.");
    }

    @Given("both applications contain the same transactions")
    public void bothApplicationsContainSameTransactions() {

        Response checkA = RestAssured.given()
                .queryParam("valueDate", TEST_DATE)
                .get(VERSION_A + "/eod-transactions");

        Response checkB = RestAssured.given()
                .queryParam("valueDate", TEST_DATE)
                .get(VERSION_B + "/eod-transactions");

        if (checkA.statusCode() != 200 || checkB.statusCode() != 200) {
            throw new AssertionError("Unable to retrieve transactions");
        }

        int countA = checkA.jsonPath().getInt("recordCount");
        int countB = checkB.jsonPath().getInt("recordCount");

        if (countA == 0 || countB == 0) {
            throw new AssertionError("Both applications must contain test transactions");
        }

        System.out.println("Version A records: " + countA);
        System.out.println("Version B records: " + countB);
    }

    @When("I retrieve transactions for value date {string}")
    public void retrieveTransactions(String valueDate) {

        responseA = RestAssured.given()
                .queryParam("valueDate", valueDate)
                .get(VERSION_A + "/eod-transactions");

        responseB = RestAssured.given()
                .queryParam("valueDate", valueDate)
                .get(VERSION_B + "/eod-transactions");

    }

    @Then("both applications should return HTTP status 200")
    public void bothApplicationsReturnStatus200() {

        if (responseA.statusCode() != 200) {
            throw new AssertionError("Version A returned HTTP "
                    + responseA.statusCode());
        }

        if (responseB.statusCode() != 200) {
            throw new AssertionError("Version B returned HTTP "
                    + responseB.statusCode());
        }

        System.out.println("Both APIs returned HTTP 200.");
    }

    @Then("both applications should return the same transactions")
    public void bothApplicationsReturnSameTransactions() throws Exception {

        JsonNode jsonA = objectMapper.readTree(responseA.asString());
        JsonNode jsonB = objectMapper.readTree(responseB.asString());

        JsonNode transactionsA = jsonA.get("transactions");
        JsonNode transactionsB = jsonB.get("transactions");

        if (!transactionsA.equals(transactionsB)) {
            throw new AssertionError(
                    "Transaction differences detected between Version A and Version B");
        }

        System.out.println("Both applications returned identical transactions.");
    }

    @Then("Version A should return {int} transactions")
    public void versionAShouldReturnTransactions(int expectedCount) {
        int actualCount = responseA.jsonPath().getInt("recordCount");

        if (actualCount != expectedCount) {
            throw new AssertionError(
                    "Expected Version A to return " + expectedCount
                            + " transactions, but got " + actualCount);
        }

        System.out.println("Version A record count verified: " + actualCount);
    }

    @Then("Version B should return {int} transactions")
    public void versionBShouldReturnTransactions(int expectedCount) {
        int actualCount = responseB.jsonPath().getInt("recordCount");

        if (actualCount != expectedCount) {
            throw new AssertionError(
                    "Expected Version B to return " + expectedCount
                            + " transactions, but got " + actualCount);
        }

        System.out.println("Version B record count verified: " + actualCount);
    }

    @Then("transaction {string} should be missing from Version B")
    public void transactionShouldBeMissingFromVersionB(String transactionId)
            throws Exception {

        JsonNode jsonA = objectMapper.readTree(responseA.asString());
        JsonNode jsonB = objectMapper.readTree(responseB.asString());

        JsonNode transactionsA = jsonA.path("transactions");
        JsonNode transactionsB = jsonB.path("transactions");

        boolean foundInA = false;
        boolean foundInB = false;

        for (JsonNode transaction : transactionsA) {
            if (transactionId.equals(
                    transaction.path("transactionId").asText())) {
                foundInA = true;
            }
        }

        for (JsonNode transaction : transactionsB) {
            if (transactionId.equals(
                    transaction.path("transactionId").asText())) {
                foundInB = true;
            }
        }

        if (!foundInA) {
            throw new AssertionError(
                    transactionId + " is not present in Version A");
        }

        if (foundInB) {
            throw new AssertionError(
                    transactionId + " was expected to be missing from Version B");
        }

        System.out.println(
                "Missing transaction correctly detected: " + transactionId);
    }

    @Then("transaction {string} should be present in Version B but missing from Version A")
    public void transactionShouldBePresentInVersionBButMissingFromVersionA(
            String transactionId) throws Exception {

        JsonNode jsonA = objectMapper.readTree(responseA.asString());
        JsonNode jsonB = objectMapper.readTree(responseB.asString());

        JsonNode transactionsA = jsonA.path("transactions");
        JsonNode transactionsB = jsonB.path("transactions");

        boolean foundInA = false;
        boolean foundInB = false;

        for (JsonNode transaction : transactionsA) {
            if (transactionId.equals(
                    transaction.path("transactionId").asText())) {
                foundInA = true;
            }
        }

        for (JsonNode transaction : transactionsB) {
            if (transactionId.equals(
                    transaction.path("transactionId").asText())) {
                foundInB = true;
            }
        }

        if (foundInA || !foundInB) {
            throw new AssertionError(
                    transactionId + " was not correctly identified as an extra transaction in Version B");
        }

        System.out.println(
                "Extra transaction correctly detected: " + transactionId);
    }

    @Then("transaction {string} should have amount {string} in Version A")
    public void checkAmountInVersionA(String transactionId, String expectedAmount)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(responseA.asString())
                .path("transactions");

        for (JsonNode transaction : transactions) {
            if (transactionId.equals(transaction.path("transactionId").asText())) {
                BigDecimal actual = transaction.path("amount").decimalValue();

                if (actual.compareTo(new BigDecimal(expectedAmount)) != 0) {
                    throw new AssertionError("Incorrect amount in Version A");
                }
                System.out.println(transactionId + " amount verified in Version A: " + actual);
                return;
            }
        }

        throw new AssertionError(transactionId + " not found in Version A");
    }

    @Then("transaction {string} should have amount {string} in Version B")
    public void checkAmountInVersionB(String transactionId, String expectedAmount)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(responseB.asString())
                .path("transactions");

        for (JsonNode transaction : transactions) {
            if (transactionId.equals(transaction.path("transactionId").asText())) {
                BigDecimal actual = transaction.path("amount").decimalValue();

                if (actual.compareTo(new BigDecimal(expectedAmount)) != 0) {
                    throw new AssertionError("Incorrect amount in Version B");
                }
                System.out.println(transactionId + " amount verified in Version B: " + actual);
                return;
            }
        }

        throw new AssertionError(transactionId + " not found in Version B");
    }

    @Then("transaction {string} should have currency {string} in Version A")
    public void checkCurrencyInVersionA(String transactionId, String expectedCurrency)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(responseA.asString())
                .path("transactions");

        for (JsonNode transaction : transactions) {
            if (transactionId.equals(transaction.path("transactionId").asText())) {
                String actual = transaction.path("currency").asText();

                if (!actual.equals(expectedCurrency)) {
                    throw new AssertionError("Incorrect currency in Version A");
                }

                System.out.println(transactionId + " currency verified in Version A: " + actual);
                return;
            }
        }

        throw new AssertionError(transactionId + " not found in Version A");
    }

    @Then("transaction {string} should have currency {string} in Version B")
    public void checkCurrencyInVersionB(String transactionId, String expectedCurrency)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(responseB.asString())
                .path("transactions");

        for (JsonNode transaction : transactions) {
            if (transactionId.equals(transaction.path("transactionId").asText())) {
                String actual = transaction.path("currency").asText();

                if (!actual.equals(expectedCurrency)) {
                    throw new AssertionError("Incorrect currency in Version B");
                }

                System.out.println(transactionId + " currency verified in Version B: " + actual);
                return;
            }
        }

        throw new AssertionError(transactionId + " not found in Version B");
    }

    @Then("Version A response should contain all required transaction fields")
    public void validateVersionAFields() throws Exception {
        validateRequiredFields(responseA, "Version A");
    }

    @Then("Version B response should contain all required transaction fields")
    public void validateVersionBFields() throws Exception {
        validateRequiredFields(responseB, "Version B");
    }

    private void validateRequiredFields(Response response, String version)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(response.asString())
                .path("transactions");

        String[] requiredFields = {
                "transactionId",
                "accountId",
                "valueDate",
                "bookingDate",
                "currency",
                "amount",
                "debitCreditIndicator",
                "transactionType",
                "description",
                "status"
        };

        for (JsonNode transaction : transactions) {

            for (String field : requiredFields) {

                if (!transaction.has(field)) {
                    throw new AssertionError(
                            version + " is missing required field: " + field);
                }
            }
        }

        System.out.println(
                version + " contains all required transaction fields.");
    }

    @Then("Version A transaction fields should have correct data types")
    public void validateVersionATypes() throws Exception {
        validateFieldTypes(responseA, "Version A");
    }

    @Then("Version B transaction fields should have correct data types")
    public void validateVersionBTypes() throws Exception {
        validateFieldTypes(responseB, "Version B");
    }

    private void validateFieldTypes(Response response, String version)
            throws Exception {

        JsonNode transactions = objectMapper.readTree(response.asString())
                .path("transactions");

        String[] stringFields = {
                "transactionId",
                "accountId",
                "valueDate",
                "bookingDate",
                "currency",
                "debitCreditIndicator",
                "transactionType",
                "status"
        };

        for (JsonNode transaction : transactions) {

            String id = transaction.path("transactionId").asText();

            for (String field : stringFields) {
                if (!transaction.path(field).isTextual()) {
                    throw new AssertionError(
                            version + " " + id +
                                    ": " + field + " should be a string");
                }
            }

            JsonNode description = transaction.path("description");

            if (!description.isNull() && !description.isTextual()) {
                throw new AssertionError(
                        version + " " + id +
                                ": description should be a string or null");
            }

            if (!transaction.path("amount").isNumber()) {
                throw new AssertionError(
                        version + " " + id +
                                ": amount should be a number");
            }

            try {
                java.time.LocalDate.parse(
                        transaction.path("valueDate").asText());

                java.time.LocalDate.parse(
                        transaction.path("bookingDate").asText());

            } catch (java.time.format.DateTimeParseException ex) {
                throw new AssertionError(
                        version + " " + id + ": invalid date format", ex);
            }
        }

        System.out.println(
                version + " transaction fields have correct data types.");
    }

    @Then("Version A transaction {string} should have a null description")
    public void versionATransactionShouldHaveNullDescription(String transactionId) {
        JsonNode transactions = responseA.jsonPath().getObject("transactions", JsonNode.class);

        boolean found = false;

        for (JsonNode transaction : transactions) {
            if (transaction.path("transactionId").asText().equals(transactionId)) {
                assertTrue(transaction.has("description"),
                        "Version A: description field should exist");
                assertTrue(transaction.get("description").isNull(),
                        "Version A: description should be null");
                found = true;
                break;
            }
        }

        assertTrue(found, "Version A: transaction " + transactionId + " was not found");
    }

    @Then("Version B transaction {string} should have a null description")
    public void versionBTransactionShouldHaveNullDescription(String transactionId) {
        JsonNode transactions = responseB.jsonPath().getObject("transactions", JsonNode.class);

        boolean found = false;

        for (JsonNode transaction : transactions) {
            if (transaction.path("transactionId").asText().equals(transactionId)) {
                assertTrue(transaction.has("description"),
                        "Version B: description field should exist");
                assertTrue(transaction.get("description").isNull(),
                        "Version B: description should be null");
                found = true;
                break;
            }
        }

        assertTrue(found, "Version B: transaction " + transactionId + " was not found");
    }

    @Then("Version A transaction {string} should contain the description field")
    public void versionATransactionShouldContainDescriptionField(String transactionId) {
        JsonNode transactions = responseA.jsonPath().getObject("transactions", JsonNode.class);

        boolean found = false;
        for (JsonNode transaction : transactions) {
            if (transaction.path("transactionId").asText().equals(transactionId)) {
                assertTrue(transaction.has("description"),
                        "Version A: description field should exist");
                found = true;
                break;
            }
        }
        assertTrue(found, "Version A: transaction " + transactionId + " was not found");
    }

    @Then("Version B transaction {string} should contain the description field")
    public void versionBTransactionShouldContainDescriptionField(String transactionId) {
        JsonNode transactions = responseB.jsonPath().getObject("transactions", JsonNode.class);

        boolean found = false;
        for (JsonNode transaction : transactions) {
            if (transaction.path("transactionId").asText().equals(transactionId)) {
                assertTrue(transaction.has("description"),
                        "Version B: description field should exist");
                found = true;
                break;
            }
        }
        assertTrue(found, "Version B: transaction " + transactionId + " was not found");
    }
}
