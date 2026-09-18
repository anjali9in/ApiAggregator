package store.artistictech.apiaggregator.model;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AggregationOptionsTest {

    @Test
    void fromRequestParams_parsesDelayOverrideForNamedService() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of("profileDelayMs", "500"));

        ServiceSimulationParams profile = options.forService("profile");
        assertThat(profile.delayMs()).isEqualTo(500L);
        assertThat(profile.fail()).isNull();
    }

    @Test
    void fromRequestParams_parsesFailOverrideForNamedService() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of("failOrders", "true"));

        ServiceSimulationParams orders = options.forService("orders");
        assertThat(orders.fail()).isTrue();
        assertThat(orders.delayMs()).isNull();
    }

    @Test
    void fromRequestParams_mergesDelayAndFailOverridesForSameService() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of(
                "billingDelayMs", "999",
                "failBilling", "true"));

        ServiceSimulationParams billing = options.forService("billing");
        assertThat(billing.delayMs()).isEqualTo(999L);
        assertThat(billing.fail()).isTrue();
    }

    @Test
    void fromRequestParams_supportsArbitraryServiceNamesGenerically() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of("failLoyalty", "true"));

        assertThat(options.forService("loyalty").fail()).isTrue();
    }

    @Test
    void fromRequestParams_ignoresUnrelatedParams() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of("unrelatedParam", "ignored"));

        assertThat(options.overrides()).isEmpty();
    }

    @Test
    void forService_returnsNoneWhenNoOverrideRegistered() {
        AggregationOptions options = AggregationOptions.fromRequestParams(Map.of());

        assertThat(options.forService("billing")).isEqualTo(ServiceSimulationParams.NONE);
    }
}
