package store.artistictech.apiaggregator.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ServiceCallResultTest {

    @Test
    void success_producesSuccessfulResultWithData() {
        ServiceCallResult<String> result = ServiceCallResult.success("payload", 42);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("payload");
        assertThat(result.error()).isNull();
        assertThat(result.durationMs()).isEqualTo(42);
    }

    @Test
    void failure_producesFailedResultWithError() {
        ServiceCallResult<String> result = ServiceCallResult.failure("boom", 10);

        assertThat(result.success()).isFalse();
        assertThat(result.data()).isNull();
        assertThat(result.error()).isEqualTo("boom");
        assertThat(result.durationMs()).isEqualTo(10);
    }
}
