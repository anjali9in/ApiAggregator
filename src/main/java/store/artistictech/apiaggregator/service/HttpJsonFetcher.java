package store.artistictech.apiaggregator.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import store.artistictech.apiaggregator.model.ServiceCallResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared async HTTP-to-JSON fetch logic used by every {@link DownstreamService}, with per-call timeout and failure mapping. */
@Component
public class HttpJsonFetcher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long defaultTimeoutMs;

    public HttpJsonFetcher(ObjectMapper objectMapper,
                           @Value("${aggregator.timeout-ms:1800}") long defaultTimeoutMs) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public <T> CompletableFuture<ServiceCallResult<T>> fetch(String serviceName,
                                                             String url,
                                                             TypeReference<T> typeReference) {
        return fetch(serviceName, url, typeReference, null);
    }

    public <T> CompletableFuture<ServiceCallResult<T>> fetch(String serviceName,
                                                             String url,
                                                             TypeReference<T> typeReference,
                                                             Long timeoutMsOverride) {
        long startTime = System.nanoTime();
        long timeoutMs = timeoutMsOverride != null ? timeoutMsOverride : defaultTimeoutMs;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IllegalStateException(
                                serviceName + " service returned HTTP " + response.statusCode()));
                    }

                    try {
                        T data = objectMapper.readValue(response.body(), typeReference);
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        return ServiceCallResult.success(data, durationMs);
                    } catch (IOException ex) {
                        throw new CompletionException(
                                new IllegalStateException("Invalid JSON payload from " + serviceName + " service", ex));
                    }
                })
                .exceptionally(ex -> {
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    Throwable root = rootCause(ex);
                    String message = root.getMessage() == null ? "Unknown error while calling " + serviceName : root.getMessage();
                    return ServiceCallResult.failure(message, durationMs);
                });
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
