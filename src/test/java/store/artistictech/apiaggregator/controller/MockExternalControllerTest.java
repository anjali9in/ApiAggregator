package store.artistictech.apiaggregator.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import store.artistictech.apiaggregator.model.BillingStatus;
import store.artistictech.apiaggregator.model.Order;
import store.artistictech.apiaggregator.model.UserProfile;

class MockExternalControllerTest {

    private final MockExternalController controller = new MockExternalController();

    @Test
    void profile_returnsUserProfileWhenNotFailing() {
        UserProfile profile = controller.profile("u1", 1, false);
        assertThat(profile).isEqualTo(new UserProfile("u1", "User-u1", "GOLD"));
    }

    @Test
    void profile_throwsServiceUnavailableWhenFailFlagIsTrue() {
        assertThatThrownBy(() -> controller.profile("u1", 1, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Profile service is unavailable");
    }

    @Test
    void orders_returnsTwoOrdersForGivenUser() {
        List<Order> orders = controller.orders("u2", 1, false);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).orderId()).isEqualTo("ORD-u2-1");
    }

    @Test
    void orders_throwsServiceUnavailableWhenFailFlagIsTrue() {
        assertThatThrownBy(() -> controller.orders("u2", 1, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Orders service is unavailable");
    }

    @Test
    void billing_returnsConsistentStatusForSameUserId() {
        BillingStatus first = controller.billing("stable-user", 1, false);
        BillingStatus second = controller.billing("stable-user", 1, false);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void billing_throwsServiceUnavailableWhenFailFlagIsTrue() {
        assertThatThrownBy(() -> controller.billing("u3", 1, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Billing service is unavailable");
    }
}
