package store.artistictech.apiaggregator.service.downstream;

import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;
import store.artistictech.apiaggregator.model.UserProfile;
import store.artistictech.apiaggregator.service.DownstreamService;
import store.artistictech.apiaggregator.service.HttpJsonFetcher;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ProfileDownstreamService implements DownstreamService<UserProfile> {

    private static final long DEFAULT_DELAY_MS = 1200;

    private final HttpJsonFetcher fetcher;
    private final int serverPort;

    public ProfileDownstreamService(HttpJsonFetcher fetcher, @Value("${server.port:8080}") int serverPort) {
        this.fetcher = fetcher;
        this.serverPort = serverPort;
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public CompletableFuture<ServiceCallResult<UserProfile>> fetch(String userId, ServiceSimulationParams params) {
        long delayMs = params.delayMsOrDefault(DEFAULT_DELAY_MS);
        boolean fail = params.failOrDefault(false);
        String url = "http://localhost:" + serverPort + "/mock/profile/" + userId
                + "?delayMs=" + delayMs + "&fail=" + fail;
        return fetcher.fetch(name(), url, new TypeReference<UserProfile>() {
        });
    }
}
