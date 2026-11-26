
package com.yaazhtech.tests;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.restassured.RestAssured;

public class NegativeScenarioHooks {

    private static final String VERSION_B =
            "http://localhost:8082/api/v1";

    private static final String BULK_URL =
            VERSION_B + "/transactions/bulk";

    // Test data for TXN003
    private static final String TXN003 = """
        {
          "transactions": [{
            "transactionId": "TXN003",
            "accountId": "ACC001",
            "valueDate": "2026-11-12",
            "bookingDate": "2026-11-11",
            "currency": "USD",
            "amount": 250.75,
            "debitCreditIndicator": "CREDIT",
            "transactionType": "INTEREST",
            "description": null,
            "status": "BOOKED"
          }]
        }
        """;

    // Test data for TXN011
    private static final String TXN011 = """
        {
          "transactions": [{
            "transactionId": "TXN011",
            "accountId": "ACC007",
            "valueDate": "2026-11-13",
            "bookingDate": "2026-11-13",
            "currency": "EUR",
            "amount": 150.00,
            "debitCreditIndicator": "CREDIT",
            "transactionType": "DEPOSIT",
            "description": "Extra test transaction",
            "status": "BOOKED"
          }]
        }
        """;

    // Change TXN006 amount
    private static final String TXN006_CHANGED = """
        {
          "transactionId": "TXN006",
          "accountId": "ACC002",
          "valueDate": "2026-11-13",
          "bookingDate": "2026-11-13",
          "currency": "EUR",
          "amount": 300.00,
          "debitCreditIndicator": "CREDIT",
          "transactionType": "REFUND",
          "description": "Card refund",
          "status": "BOOKED"
        }
        """;

    // Restore TXN006 amount
    private static final String TXN006_ORIGINAL = """
        {
          "transactionId": "TXN006",
          "accountId": "ACC002",
          "valueDate": "2026-11-13",
          "bookingDate": "2026-11-13",
          "currency": "EUR",
          "amount": 75.25,
          "debitCreditIndicator": "CREDIT",
          "transactionType": "REFUND",
          "description": "Card refund",
          "status": "BOOKED"
        }
        """;

    // Change TXN007 currency
    private static final String TXN007_CHANGED = """
        {
          "transactionId": "TXN007",
          "accountId": "ACC005",
          "valueDate": "2026-11-13",
          "bookingDate": "2026-11-13",
          "currency": "EUR",
          "amount": -42.10,
          "debitCreditIndicator": "DEBIT",
          "transactionType": "CARD_PAYMENT",
          "description": "Transport",
          "status": "BOOKED"
        }
        """;

    // Restore TXN007 currency
    private static final String TXN007_ORIGINAL = """
        {
          "transactionId": "TXN007",
          "accountId": "ACC005",
          "valueDate": "2026-11-13",
          "bookingDate": "2026-11-13",
          "currency": "CHF",
          "amount": -42.10,
          "debitCreditIndicator": "DEBIT",
          "transactionType": "CARD_PAYMENT",
          "description": "Transport",
          "status": "BOOKED"
        }
        """;

    @Before("@missing")
    public void removeTXN003() {
        RestAssured.given()
                .delete(VERSION_B + "/transactions/TXN003")
                .then()
                .statusCode(200);
    }

    @After("@missing")
    public void restoreTXN003() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN003)
                .post(BULK_URL)
                .then()
                .statusCode(200);
    }

    @Before("@extra")
    public void addTXN011() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN011)
                .post(BULK_URL)
                .then()
                .statusCode(200);
    }

    @After("@extra")
    public void removeTXN011() {
        RestAssured.given()
                .delete(VERSION_B + "/transactions/TXN011")
                .then()
                .statusCode(200);
    }

    @Before("@amount")
    public void changeTXN006Amount() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN006_CHANGED)
                .put(VERSION_B + "/transactions/TXN006")
                .then()
                .statusCode(200);
    }

    @After("@amount")
    public void restoreTXN006Amount() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN006_ORIGINAL)
                .put(VERSION_B + "/transactions/TXN006")
                .then()
                .statusCode(200);
    }

    @Before("@currency")
    public void changeTXN007Currency() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN007_CHANGED)
                .put(VERSION_B + "/transactions/TXN007")
                .then()
                .statusCode(200);
    }

    @After("@currency")
    public void restoreTXN007Currency() {
        RestAssured.given()
                .contentType("application/json")
                .body(TXN007_ORIGINAL)
                .put(VERSION_B + "/transactions/TXN007")
                .then()
                .statusCode(200);
    }
}

