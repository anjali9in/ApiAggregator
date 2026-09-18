package store.artistictech.apiaggregator.model;

public record BillingStatus(String userId, String status, boolean overdue) {
}
