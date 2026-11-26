Feature: Bulk load banking transactions

  As a test engineer
  I want to test bulk transaction loading in both applications
  So that I can verify data loading and validation

  @bulkload
  Scenario: Load transactions into Version A and Version B
    Given both banking applications are running
    When I bulk load the transaction dataset into both applications
    Then both applications should process all input transactions
    And both bulk load results should be displayed

  @bulkload
  Scenario: Reject a transaction with a missing transaction ID
    Given both banking applications are running
    When I submit an invalid transaction without a transaction ID
    Then Version A should reject the invalid transaction
    And Version B should reject the invalid transaction
