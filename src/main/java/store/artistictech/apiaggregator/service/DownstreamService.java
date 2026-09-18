package store.artistictech.apiaggregator.service;

import java.util.concurrent.CompletableFuture;

import store.artistictech.apiaggregator.model.ServiceCallResult;

/**
 * Strategy contract for a single downstream dependency.
 * Add a new service by creating a new Spring bean implementing this interface;
 * {@link AggregationService} auto-discovers all beans of this type and requires no code changes.
 */
public interface DownstreamService<T> {

    /** Unique key used in the aggregated response and in request-param overrides (e.g. "profile" -> profileDelayMs, failProfile). */
    String name();

    CompletableFuture<ServiceCallResult<T>> fetch(String userId, store.artistictech.apiaggregator.model.ServiceSimulationParams params);
}
