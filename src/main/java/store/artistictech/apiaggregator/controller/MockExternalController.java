package store.artistictech.apiaggregator.controller;

import store.artistictech.apiaggregator.model.BillingStatus;
import store.artistictech.apiaggregator.model.Order;
import store.artistictech.apiaggregator.model.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mock")
public class MockExternalController {

    @GetMapping("/profile/{userId}")
    public UserProfile profile(@PathVariable String userId,
                               @RequestParam(defaultValue = "1200") long delayMs,
                               @RequestParam(defaultValue = "false") boolean fail) {
        simulateDelay(delayMs);
        if (fail) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Profile service is unavailable");
        }
        return new UserProfile(userId, "User-" + userId, "GOLD");
    }

    @GetMapping("/orders/{userId}")
    public List<Order> orders(@PathVariable String userId,
                              @RequestParam(defaultValue = "1500") long delayMs,
                              @RequestParam(defaultValue = "false") boolean fail) {
        simulateDelay(delayMs);
        if (fail) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Orders service is unavailable");
        }
        return List.of(
                new Order("ORD-" + userId + "-1", 149.90, "SHIPPED"),
                new Order("ORD-" + userId + "-2", 39.50, "PROCESSING"));
    }

    @GetMapping("/billing/{userId}")
    public BillingStatus billing(@PathVariable String userId,
                                 @RequestParam(defaultValue = "1000") long delayMs,
                                 @RequestParam(defaultValue = "false") boolean fail) {
        simulateDelay(delayMs);
        if (fail) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing service is unavailable");
        }

        boolean overdue = Math.abs(userId.hashCode()) % 2 == 0;
        String status = overdue ? "OVERDUE" : "CURRENT";
        return new BillingStatus(userId, status, overdue);
    }

    private void simulateDelay(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Request interrupted", ex);
        }
    }
}
