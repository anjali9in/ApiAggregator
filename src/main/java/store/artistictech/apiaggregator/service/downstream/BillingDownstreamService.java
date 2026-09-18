package store.artistictech.apiaggregator.service.downstream;

import store.artistictech.apiaggregator.model.BillingStatus;
import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;
import store.artistictech.apiaggregator.service.DownstreamService;
import store.artistictech.apiaggregator.service.HttpJsonFetcher;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class BillingDownstreamService implements DownstreamService<BillingStatus> {

    private static final long DEFAULT_DELAY_MS = 1000;

    private final HttpJsonFetcher fetcher;
    private final int serverPort;

    public BillingDownstreamService(HttpJsonFetcher fetcher, @Value("${server.port:8080}") int serverPort) {
        this.fetcher = fetcher;
        this.serverPort = serverPort;
    }

    @Override
    public String name() {
        return "billing";
    }

    @Override
    public CompletableFuture<ServiceCallResult<BillingStatus>> fetch(String userId, ServiceSimulationParams params) {
        long delayMs = params.delayMsOrDefault(DEFAULT_DELAY_MS);
        boolean fail = params.failOrDefault(false);
        String url = "http://localhost:" + serverPort + "/mock/billing/" + userId
                + "?delayMs=" + delayMs + "&fail=" + fail;
        return fetcher.fetch(name(), url, new TypeReference<BillingStatus>() {
        });
    }
}
