package store.artistictech.apiaggregator.service;

import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.UserProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HttpJsonFetcherTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetch_returnsSuccessResultForValidJsonResponse() throws Exception {
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"userId\":\"u1\",\"name\":\"User-u1\",\"tier\":\"GOLD\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        HttpJsonFetcher fetcher = new HttpJsonFetcher(new ObjectMapper(), 2000);
        CompletableFuture<ServiceCallResult<UserProfile>> future = fetcher.fetch(
                "profile", "http://localhost:" + port + "/ok", new TypeReference<UserProfile>() {
                });

        ServiceCallResult<UserProfile> result = future.get(3, TimeUnit.SECONDS);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(new UserProfile("u1", "User-u1", "GOLD"));
        assertThat(result.error()).isNull();
    }

    @Test
    void fetch_returnsFailureResultForNon2xxStatus() throws Exception {
        server.createContext("/broken", exchange -> exchange.sendResponseHeaders(503, -1));
        server.start();

        HttpJsonFetcher fetcher = new HttpJsonFetcher(new ObjectMapper(), 2000);
        CompletableFuture<ServiceCallResult<UserProfile>> future = fetcher.fetch(
                "profile", "http://localhost:" + port + "/broken", new TypeReference<UserProfile>() {
                });

        ServiceCallResult<UserProfile> result = future.get(3, TimeUnit.SECONDS);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("503");
    }

    @Test
    void fetch_returnsFailureResultForInvalidJsonBody() throws Exception {
        server.createContext("/badjson", exchange -> {
            byte[] body = "not-json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        HttpJsonFetcher fetcher = new HttpJsonFetcher(new ObjectMapper(), 2000);
        CompletableFuture<ServiceCallResult<UserProfile>> future = fetcher.fetch(
                "profile", "http://localhost:" + port + "/badjson", new TypeReference<UserProfile>() {
                });

        ServiceCallResult<UserProfile> result = future.get(3, TimeUnit.SECONDS);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Invalid JSON");
    }

    @Test
    void fetch_returnsFailureResultWhenDefaultTimeoutIsExceeded() throws Exception {
        server.createContext("/slow", exchange -> {
            sleepQuietly(1000);
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        HttpJsonFetcher fetcher = new HttpJsonFetcher(new ObjectMapper(), 100);
        CompletableFuture<ServiceCallResult<UserProfile>> future = fetcher.fetch(
                "profile", "http://localhost:" + port + "/slow", new TypeReference<UserProfile>() {
                });

        ServiceCallResult<UserProfile> result = future.get(3, TimeUnit.SECONDS);

        assertThat(result.success()).isFalse();
    }

    @Test
    void fetch_perCallTimeoutOverridesDefaultTimeout() throws Exception {
        server.createContext("/medium", exchange -> {
            sleepQuietly(300);
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        HttpJsonFetcher fetcher = new HttpJsonFetcher(new ObjectMapper(), 5000);
        CompletableFuture<ServiceCallResult<UserProfile>> future = fetcher.fetch(
                "profile", "http://localhost:" + port + "/medium", new TypeReference<UserProfile>() {
                }, 100L);

        ServiceCallResult<UserProfile> result = future.get(3, TimeUnit.SECONDS);

        assertThat(result.success()).isFalse();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
