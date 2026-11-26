package com.yaazhtech.bank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(String transactionId, String accountId, LocalDate valueDate, LocalDate bookingDate,
                          String currency, BigDecimal amount, String debitCreditIndicator, String transactionType,
                          String description, String status) {
}
