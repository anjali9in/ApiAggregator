package store.artistictech.apiaggregator.service.downstream;

import java.util.List;
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

import store.artistictech.apiaggregator.model.Order;
import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;
import store.artistictech.apiaggregator.service.HttpJsonFetcher;

@ExtendWith(MockitoExtension.class)
class OrdersDownstreamServiceTest {

    @Mock
    private HttpJsonFetcher fetcher;

    @Test
    void name_returnsOrders() {
        OrdersDownstreamService service = new OrdersDownstreamService(fetcher, 8080);
        assertThat(service.name()).isEqualTo("orders");
    }

    @Test
    void fetch_usesDefaultDelayAndFailWhenNoOverridesGiven() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.success(
                        List.of(new Order("ORD-1", 10.0, "SHIPPED")), 10)));

        OrdersDownstreamService service = new OrdersDownstreamService(fetcher, 8080);
        CompletableFuture<ServiceCallResult<List<Order>>> result = service.fetch("u1", ServiceSimulationParams.NONE);

        assertThat(result.join().data()).hasSize(1);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("orders"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:8080/mock/orders/u1?delayMs=1500&fail=false");
    }

    @Test
    void fetch_appliesDelayAndFailOverridesToBuiltUrl() {
        when(fetcher.fetch(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(ServiceCallResult.failure("down", 5)));

        OrdersDownstreamService service = new OrdersDownstreamService(fetcher, 9090);
        service.fetch("u2", new ServiceSimulationParams(75L, true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(fetcher).fetch(eq("orders"), urlCaptor.capture(), any());
        assertThat(urlCaptor.getValue()).isEqualTo("http://localhost:9090/mock/orders/u2?delayMs=75&fail=true");
    }
}
