Feature: Compare end-of-day banking transactions

  As a test engineer
  I want to compare transactions returned by Version A and Version B
  So that I can identify differences after a system migration

  @positive
  Scenario: Both applications return identical transactions
    Given both banking applications are running
    And both applications contain the same transactions
    When I retrieve transactions for value date "2026-11-12"
    Then both applications should return HTTP status 200
    And both applications should return the same transactions

  @negative @missing
  Scenario: Detect a transaction missing from Version B
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-12"
    Then Version A should return 5 transactions
    And Version B should return 4 transactions
    And transaction "TXN003" should be missing from Version B

  @negative @extra
  Scenario: Detect an extra transaction in Version B
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-13"
    Then Version A should return 3 transactions
    And Version B should return 4 transactions
    And transaction "TXN011" should be present in Version B but missing from Version A

  @negative @amount
  Scenario: Detect an amount mismatch in Version B
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-13"
    Then transaction "TXN006" should have amount "75.25" in Version A
    And transaction "TXN006" should have amount "300.00" in Version B

  @negative @currency
  Scenario: Detect a currency mismatch in Version B
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-13"
    Then transaction "TXN007" should have currency "CHF" in Version A
    And transaction "TXN007" should have currency "EUR" in Version B

  @validation
  Scenario: Return no transactions for a date with no records
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-15"
    Then Version A should return 0 transactions
    And Version B should return 0 transactions


  @validation
  Scenario: Validate required fields in API responses
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-12"
    Then Version A response should contain all required transaction fields
    And Version B response should contain all required transaction fields

  @validation
  Scenario: Validate transaction field data types
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-12"
    Then Version A transaction fields should have correct data types
    And Version B transaction fields should have correct data types

  @validation
  Scenario: Accept nullable description fields
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-12"
    Then Version A transaction "TXN003" should have a null description
    And Version B transaction "TXN003" should have a null description


  @validation
  Scenario: Confirm nullable description field is present
    Given both banking applications are running
    When I retrieve transactions for value date "2026-11-12"
    Then Version A transaction "TXN003" should contain the description field
    And Version B transaction "TXN003" should contain the description field