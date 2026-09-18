package store.artistictech.apiaggregator.model;

import java.util.Map;

/** Generic aggregate result: one entry per registered DownstreamService, keyed by its name. */
public record AggregatedResponse(
        String userId,
        boolean partialFailure,
        Map<String, Object> data,
        Map<String, ServiceStatus> services) {
}
