package store.artistictech.apiaggregator.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import store.artistictech.apiaggregator.model.AggregatedResponse;
import store.artistictech.apiaggregator.model.AggregationOptions;
import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceStatus;

/**
 * Fans out to every registered {@link DownstreamService}, waits for all of them via
 * {@link CompletableFuture#allOf}, and merges results. Adding a new downstream dependency
 * only requires a new DownstreamService bean; this class needs no changes to scale.
 */
@Service
public class AggregationService {

    private final List<DownstreamService<?>> downstreamServices;

    public AggregationService(List<DownstreamService<?>> downstreamServices) {
        this.downstreamServices = downstreamServices;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AggregatedResponse aggregate(String userId, AggregationOptions options) {
        Map<String, CompletableFuture<ServiceCallResult<Object>>> futuresByName = new LinkedHashMap<>();

        for (DownstreamService<?> service : downstreamServices) {
            CompletableFuture<ServiceCallResult<Object>> future =
                    (CompletableFuture) service.fetch(userId, options.forService(service.name()));
            futuresByName.put(service.name(), future);
        }

        CompletableFuture.allOf(futuresByName.values().toArray(CompletableFuture[]::new)).join();

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, ServiceStatus> statuses = new LinkedHashMap<>();
        boolean partialFailure = false;

        for (Map.Entry<String, CompletableFuture<ServiceCallResult<Object>>> entry : futuresByName.entrySet()) {
            ServiceCallResult<Object> result = entry.getValue().join();
            statuses.put(entry.getKey(), new ServiceStatus(result.success(), result.error(), result.durationMs()));
            if (result.success()) {
                data.put(entry.getKey(), result.data());
            } else {
                partialFailure = true;
            }
        }

        return new AggregatedResponse(userId, partialFailure, data, statuses);
    }
}
