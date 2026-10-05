package com.bloodmanagement.service;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.model.EmergencyRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EmergencyProcessingService implements AutoCloseable {
    private final ExecutorService workers = Executors.newFixedThreadPool(3, task -> {
        Thread thread = new Thread(task, "emergency-request-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final DonorMatchingService matching;
    private final BloodInventoryService inventory;

    public EmergencyProcessingService(DonorMatchingService matching, BloodInventoryService inventory) {
        this.matching = matching;
        this.inventory = inventory;
    }

    public CompletableFuture<ProcessingResult> process(EmergencyRequest request) {
        CompletableFuture<List<DonorMatchingService.MatchResult>> donorSearch =
                CompletableFuture.supplyAsync(
                        () -> matching.findMatches(request.bloodGroupRequired(), request.hospitalLocation()), workers);
        CompletableFuture<Integer> inventoryCheck =
                CompletableFuture.supplyAsync(
                        () -> inventory.availableStock(request.bloodGroupRequired()), workers);
        CompletableFuture<Integer> priority =
                CompletableFuture.supplyAsync(() -> priorityRank(request.emergencyLevel()), workers);
        return CompletableFuture.allOf(donorSearch, inventoryCheck, priority)
                .thenApply(ignored -> new ProcessingResult(donorSearch.join(), inventoryCheck.join(), priority.join()));
    }

    private int priorityRank(EmergencyLevel level) {
        return switch (level) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 3;
            case LOW -> 4;
        };
    }

    @Override
    public void close() {
        workers.shutdown();
    }

    public record ProcessingResult(List<DonorMatchingService.MatchResult> matches, int availableUnits, int priorityRank) {
    }
}
