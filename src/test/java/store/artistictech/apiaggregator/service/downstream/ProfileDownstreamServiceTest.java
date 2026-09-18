package store.artistictech.apiaggregator.service.downstream;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;
import store.artistictech.apiaggregator.model.UserProfile;
import store.artistictech.apiaggregator.service.HttpJsonFetcher;

@ExtendWith(MockitoExtension.class)
class ProfileDownstreamServiceTest {

    @Mock
    private HttpJsonFetcher fetcher;

    @Test
    void name_returnsProfile() {
        ProfileDownstreamService service = new ProfileDownstreamService(fetcher, 8080);
        assertThat(service.name()).isEqualTo("profile");
    }

    @Test
    void fetch_usesDefaultDelayAndFailWhenNoOverridesGiven() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.success(new UserProfile("u1", "User-u1", "GOLD"), 10)));

        ProfileDownstreamService service = new ProfileDownstreamService(fetcher, 8080);
        CompletableFuture<ServiceCallResult<UserProfile>> result = service.fetch("u1", ServiceSimulationParams.NONE);

        assertThat(result.join().data().userId()).isEqualTo("u1");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("profile"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:8080/mock/profile/u1?delayMs=1200&fail=false");
    }

    @Test
    void fetch_appliesDelayAndFailOverridesToBuiltUrl() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.failure("down", 5)));

        ProfileDownstreamService service = new ProfileDownstreamService(fetcher, 9090);
        service.fetch("u2", new ServiceSimulationParams(50L, true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("profile"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:9090/mock/profile/u2?delayMs=50&fail=true");
    }
}
