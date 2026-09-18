# Asynchronous API Aggregator (Java)

This project is a Spring Boot REST backend that receives one request and aggregates data from three mocked slow downstream services:
- user profile
- recent orders
- billing status

It demonstrates:
- non-blocking downstream calls with `CompletableFuture`
- concurrent fan-out/fan-in with `CompletableFuture.allOf()`
- timeout protection with `.orTimeout(...)`
- partial failure handling so one failed service does not fail the whole response
- a pluggable, scalable design for adding more downstream services without touching the aggregation engine

## Run

```bash
mvn spring-boot:run
```

## Main endpoint

```text
GET /api/aggregate/{userId}
```

Response shape (generic, not hardcoded to 3 services):

```json
{
  "userId": "u-100",
  "partialFailure": false,
  "data": {
    "profile": { "...": "..." },
    "orders": [ { "...": "..." } ],
    "billing": { "...": "..." }
  },
  "services": {
    "profile": { "success": true, "error": null, "durationMs": 1203 },
    "orders": { "success": true, "error": null, "durationMs": 1502 },
    "billing": { "success": true, "error": null, "durationMs": 1001 }
  }
}
```

### Query params for simulation

Delay/failure overrides are parsed generically as `{serviceName}DelayMs` and `fail{ServiceName}`, so they automatically work for any registered service, not just these three:

- `profileDelayMs` (default `1200`, from `ProfileDownstreamService`)
- `ordersDelayMs` (default `1500`, from `OrdersDownstreamService`)
- `billingDelayMs` (default `1000`, from `BillingDownstreamService`)
- `failProfile` (default `false`)
- `failOrders` (default `false`)
- `failBilling` (default `false`)

## Adding a new downstream service

The aggregator is designed to scale to more services without modifying `AggregationService`, `AggregationController`, or `AggregatedResponse`:

1. Create a new class implementing `DownstreamService<T>` (see `service/downstream/ProfileDownstreamService.java` for a template) and annotate it `@Component`. Give it a unique `name()`.
2. Use the injected `HttpJsonFetcher` to make the async call; it already handles timeouts (`.orTimeout(...)`) and failure mapping.
3. (For this demo) add a matching mocked endpoint in `MockExternalController`.

Spring auto-discovers all `DownstreamService` beans and injects them as a `List<DownstreamService<?>>` into `AggregationService`, which fans out to all of them concurrently via `CompletableFuture.allOf(...)`. The new service's data appears under `data.{name}` and its status under `services.{name}` automatically, and its `{name}DelayMs`/`fail{Name}` query params work out of the box.

## Examples

All succeed:

```bash
curl "http://localhost:8080/api/aggregate/u-100"
```

Profile service fails (partial failure):

```bash
curl "http://localhost:8080/api/aggregate/u-100?failProfile=true"
```

Orders service times out (`ordersDelayMs` > `aggregator.timeout-ms`):

```bash
curl "http://localhost:8080/api/aggregate/u-100?ordersDelayMs=5000"
```


## Code Flow

1. Client sends `GET /api/aggregate/{userId}`.
2. `AggregationController` passes all raw query params to `AggregationOptions.fromRequestParams(...)`, which generically extracts `{name}DelayMs`/`fail{Name}` overrides for whatever services happen to be registered.
3. `AggregationService` is injected with `List<DownstreamService<?>>` — every `@Component` implementing `DownstreamService` (currently `ProfileDownstreamService`, `OrdersDownstreamService`, `BillingDownstreamService`) — auto-discovered by Spring, in no particular fixed count.
4. For each registered service, `AggregationService` calls `service.fetch(userId, options.forService(service.name()))`, starting all downstream calls concurrently. Each implementation builds its own URL and delegates to the shared `HttpJsonFetcher`, returning a `CompletableFuture<ServiceCallResult<T>>`.
5. Each call has `.orTimeout(aggregator.timeout-ms, MILLISECONDS)` applied inside `HttpJsonFetcher`.
6. `CompletableFuture.allOf(...)` is built dynamically from all futures and used as a fan-in barrier; `.join()` waits until every future completes.
7. Each future's result is read with `.join()` (already completed at this point) and merged into a `Map<String, Object> data` and `Map<String, ServiceStatus> services`, keyed by service name.
8. Failures are captured per service and converted into `ServiceCallResult.failure(...)`, so one failure does not fail the whole aggregate response.
9. API response includes merged business data (`data`) plus per-service status metadata (`services`: `success`, `error`, `durationMs`).

## Multithreading and Concurrency Notes

This implementation performs concurrent downstream I/O using Java `HttpClient.sendAsync(...)` and `CompletableFuture`. All registered downstream requests are in flight at the same time, regardless of how many services are configured, which reduces total latency versus sequential calls.

`CompletableFuture.allOf(...).join()` does block the current request-handling thread until every registered future finishes, but only after the asynchronous work has already been started for all of them. In other words, I/O is non-blocking at the HTTP client layer, while aggregation waits at one synchronization point before constructing the final response.

Timeout handling is per downstream call, not global for the whole aggregate operation. If a service exceeds `aggregator.timeout-ms`, that future is completed exceptionally and mapped to a failure result in `.exceptionally(...)`; other services continue and can still succeed.

On executors: this code does not provide a custom `Executor` to `CompletableFuture` or `HttpClient`. Execution therefore relies on the runtime defaults used by `HttpClient` and completion-stage processing. If you need stricter control over throughput/isolation, you can configure a dedicated `Executor` and wire it into your async client strategy.

In summary, this project demonstrates concurrent fan-out/fan-in aggregation with resilient partial-failure behavior and per-call timeout protection using `CompletableFuture`.

## Notes

- The `aggregator.timeout-ms` property defines the maximum time the aggregator will wait for each downstream service before considering it as timed out.
- Partial failures are handled gracefully, meaning that if one service fails or times out, the aggregator will still return the data from the other services.
- The use of `CompletableFuture` allows for non-blocking, concurrent calls to multiple services, improving the overall responsiveness of the API aggregator.
- `CompletableFuture.allOf(...).join()` is the synchronization point where the request thread waits for all in-flight async calls to complete, no matter how many services are registered.
- The response includes service-level diagnostics in `services` (`success`, `error`, `durationMs`) so timeouts/failures are visible without losing successful data from other services.
- A custom `Executor` is optional and can be introduced later if you need tighter resource control under high load.