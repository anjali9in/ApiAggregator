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

import store.artistictech.apiaggregator.model.BillingStatus;
import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;
import store.artistictech.apiaggregator.service.HttpJsonFetcher;

@ExtendWith(MockitoExtension.class)
class BillingDownstreamServiceTest {

    @Mock
    private HttpJsonFetcher fetcher;

    @Test
    void name_returnsBilling() {
        BillingDownstreamService service = new BillingDownstreamService(fetcher, 8080);
        assertThat(service.name()).isEqualTo("billing");
    }

    @Test
    void fetch_usesDefaultDelayAndFailWhenNoOverridesGiven() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.success(
                        new BillingStatus("u1", "CURRENT", false), 10)));

        BillingDownstreamService service = new BillingDownstreamService(fetcher, 8080);
        CompletableFuture<ServiceCallResult<BillingStatus>> result = service.fetch("u1", ServiceSimulationParams.NONE);

        assertThat(result.join().data().status()).isEqualTo("CURRENT");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("billing"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:8080/mock/billing/u1?delayMs=1000&fail=false");
    }

    @Test
    void fetch_appliesDelayAndFailOverridesToBuiltUrl() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.failure("down", 5)));

        BillingDownstreamService service = new BillingDownstreamService(fetcher, 9090);
        service.fetch("u2", new ServiceSimulationParams(25L, true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("billing"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:9090/mock/billing/u2?delayMs=25&fail=true");
    }
}
