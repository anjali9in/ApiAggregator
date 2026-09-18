package store.artistictech.apiaggregator.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import store.artistictech.apiaggregator.model.AggregatedResponse;
import store.artistictech.apiaggregator.model.AggregationOptions;
import store.artistictech.apiaggregator.model.ServiceCallResult;
import store.artistictech.apiaggregator.model.ServiceSimulationParams;

class AggregationServiceTest {

    @Test
    void aggregate_mergesAllSuccessfulResultsWithNoPartialFailure() {
        DownstreamService<String> profile = mockService("profile", ServiceCallResult.success("profile-data", 10));
        DownstreamService<String> orders = mockService("orders", ServiceCallResult.success("orders-data", 20));

        AggregationService service = new AggregationService(List.of(profile, orders));
        AggregatedResponse response = service.aggregate("u1", AggregationOptions.EMPTY);

        assertThat(response.userId()).isEqualTo("u1");
        assertThat(response.partialFailure()).isFalse();
        assertThat(response.data()).containsEntry("profile", "profile-data");
        assertThat(response.data()).containsEntry("orders", "orders-data");
        assertThat(response.services().get("profile").success()).isTrue();
        assertThat(response.services().get("orders").success()).isTrue();
    }

    @Test
    void aggregate_flagsPartialFailureButKeepsSuccessfulServiceData() {
        DownstreamService<String> profile = mockService("profile", ServiceCallResult.success("profile-data", 10));
        DownstreamService<String> billing = mockService("billing", ServiceCallResult.failure("timeout", 30));

        AggregationService service = new AggregationService(List.of(profile, billing));
        AggregatedResponse response = service.aggregate("u2", AggregationOptions.EMPTY);

        assertThat(response.partialFailure()).isTrue();
        assertThat(response.data()).containsEntry("profile", "profile-data");
        assertThat(response.data()).doesNotContainKey("billing");
        assertThat(response.services().get("billing").success()).isFalse();
        assertThat(response.services().get("billing").error()).isEqualTo("timeout");
    }

    @Test
    void aggregate_returnsEmptyResultWhenNoServicesAreRegistered() {
        AggregationService service = new AggregationService(List.of());
        AggregatedResponse response = service.aggregate("u3", AggregationOptions.EMPTY);

        assertThat(response.partialFailure()).isFalse();
        assertThat(response.data()).isEmpty();
        assertThat(response.services()).isEmpty();
    }

    @Test
    void aggregate_passesPerServiceOverrideFromOptionsToMatchingService() {
        DownstreamService<String> profile = mockService("profile", ServiceCallResult.success("d", 1));

        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of("profileDelayMs", "999"));
        AggregationService service = new AggregationService(List.of(profile));
        service.aggregate("u1", options);

        ArgumentCaptor<ServiceSimulationParams> captor = ArgumentCaptor.forClass(ServiceSimulationParams.class);
        verify(profile).fetch(eq("u1"), captor.capture());
        assertThat(captor.getValue().delayMs()).isEqualTo(999L);
    }

    @SuppressWarnings("unchecked")
    private DownstreamService<String> mockService(String name, ServiceCallResult<String> result) {
        DownstreamService<String> service = mock(DownstreamService.class);
        when(service.name()).thenReturn(name);
        when(service.fetch(any(), any())).thenReturn(CompletableFuture.completedFuture(result));
        return service;
    }
}
