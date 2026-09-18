package store.artistictech.apiaggregator.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ServiceSimulationParamsTest {

    @Test
    void delayMsOrDefault_returnsOverrideWhenPresent() {
        ServiceSimulationParams params = new ServiceSimulationParams(500L, null);
        assertThat(params.delayMsOrDefault(1000)).isEqualTo(500L);
    }

    @Test
    void delayMsOrDefault_returnsDefaultWhenOverrideAbsent() {
        assertThat(ServiceSimulationParams.NONE.delayMsOrDefault(1000)).isEqualTo(1000L);
    }

    @Test
    void failOrDefault_returnsOverrideWhenPresent() {
        ServiceSimulationParams params = new ServiceSimulationParams(null, true);
        assertThat(params.failOrDefault(false)).isTrue();
    }

    @Test
    void failOrDefault_returnsDefaultWhenOverrideAbsent() {
        assertThat(ServiceSimulationParams.NONE.failOrDefault(false)).isFalse();
    }
}
