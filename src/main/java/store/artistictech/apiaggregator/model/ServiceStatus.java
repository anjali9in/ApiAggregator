package store.artistictech.apiaggregator.model;

public record ServiceStatus(boolean success, String error, long durationMs) {
}
