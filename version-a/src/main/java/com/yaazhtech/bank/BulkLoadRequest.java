package com.yaazhtech.bank;

import java.util.List;

public record BulkLoadRequest(List<Transaction> transactions) {
}
