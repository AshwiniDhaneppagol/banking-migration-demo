package com.yaazhtech.bank;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {
    private final Map<String, Transaction> store = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "application", "bank-service");
    }

    @PostMapping("/transactions/bulk")
    public Map<String, Object> bulk(@RequestBody BulkLoadRequest request) {
        if (request == null || request.transactions() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactions is required");
        int inserted = 0, duplicates = 0, rejected = 0;
        List<Map<String, String>> errors = new ArrayList<>();
        for (Transaction t : request.transactions()) {
            if (t == null || t.transactionId() == null || t.transactionId().isBlank() || t.valueDate() == null || t.amount() == null) {
                rejected++;
                errors.add(Map.of("transactionId", t == null ? "<null>" : String.valueOf(t.transactionId()), "reason", "transactionId, valueDate and amount are required"));
                continue;
            }
            if (store.putIfAbsent(t.transactionId(), t) == null) inserted++;
            else duplicates++;
        }
        return Map.of("inputCount", request.transactions().size(), "insertedCount", inserted, "duplicateCount", duplicates, "rejectedCount", rejected, "errors", errors);
    }

    @GetMapping("/eod-transactions")
    public Map<String, Object> eod(@RequestParam("valueDate") String valueDate) {
        LocalDate date;
        try {
            date = LocalDate.parse(valueDate);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valueDate must use YYYY-MM-DD");
        }
        List<Transaction> result = store.values().stream().filter(t -> date.equals(t.valueDate())).sorted(Comparator.comparing(Transaction::transactionId)).toList();
        return Map.of("valueDate", date, "recordCount", result.size(), "transactions", result);
    }

    // Test-support endpoint to demonstrate controlled regression defects in the interview demo.
    @PutMapping("/transactions/{id}")
    public Transaction replace(@PathVariable("id") String id, @RequestBody Transaction t) {
        if (!store.containsKey(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not found");
        if (!id.equals(t.transactionId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path and transactionId must match");
        store.put(id, t);
        return t;
    }

    @DeleteMapping("/transactions/{id}")
    public void delete(@PathVariable("id") String id) {
        if (store.remove(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not found");
    }
}
