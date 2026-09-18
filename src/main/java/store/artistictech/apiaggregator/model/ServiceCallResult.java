package store.artistictech.apiaggregator.model;

public record ServiceCallResult<T>(boolean success, T data, String error, long durationMs) {

    public static <T> ServiceCallResult<T> success(T data, long durationMs) {
        return new ServiceCallResult<>(true, data, null, durationMs);
    }

    public static <T> ServiceCallResult<T> failure(String error, long durationMs) {
        return new ServiceCallResult<>(false, null, error, durationMs);
    }
}
