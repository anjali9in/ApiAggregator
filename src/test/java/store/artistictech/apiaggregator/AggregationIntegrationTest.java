package store.artistictech.apiaggregator;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import store.artistictech.apiaggregator.model.AggregatedResponse;
import store.artistictech.apiaggregator.model.ServiceStatus;

/** End-to-end checks over the real HTTP stack: success, partial failure, and timeout handling. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AggregationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void aggregate_returnsAllServicesWhenEverythingSucceeds() {
        String url = "/api/aggregate/u-int-1?profileDelayMs=10&ordersDelayMs=10&billingDelayMs=10";

        AggregatedResponse response = restTemplate.getForObject(url, AggregatedResponse.class);

        assertThat(response.partialFailure()).isFalse();
        assertThat(response.data()).containsKeys("profile", "orders", "billing");
        assertThat(response.services().values()).allMatch(ServiceStatus::success);
    }

    @Test
    void aggregate_reportsPartialFailureWhenOneServiceFails() {
        String url = "/api/aggregate/u-int-2?profileDelayMs=10&ordersDelayMs=10&billingDelayMs=10&failOrders=true";

        AggregatedResponse response = restTemplate.getForObject(url, AggregatedResponse.class);

        assertThat(response.partialFailure()).isTrue();
        assertThat(response.data()).containsKeys("profile", "billing");
        assertThat(response.data()).doesNotContainKey("orders");
        assertThat(response.services().get("orders").success()).isFalse();
    }

    @Test
    void aggregate_marksSlowServiceAsFailedWhenItExceedsTimeout() {
        String url = "/api/aggregate/u-int-3?profileDelayMs=10&ordersDelayMs=5000&billingDelayMs=10";

        AggregatedResponse response = restTemplate.getForObject(url, AggregatedResponse.class);

        assertThat(response.partialFailure()).isTrue();
        assertThat(response.services().get("orders").success()).isFalse();
        assertThat(response.data()).containsKeys("profile", "billing");
    }
}
