package store.artistictech.apiaggregator.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import store.artistictech.apiaggregator.model.AggregatedResponse;
import store.artistictech.apiaggregator.model.AggregationOptions;
import store.artistictech.apiaggregator.service.AggregationService;

@RestController
@RequestMapping("/api")
public class AggregationController {

    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    // Generic param map keeps this endpoint agnostic to how many downstream services are registered.
    @GetMapping("/aggregate/{userId}")
    public AggregatedResponse aggregate(@PathVariable String userId, @RequestParam Map<String, String> allParams) {
        AggregationOptions options = AggregationOptions.fromRequestParams(allParams);
        return aggregationService.aggregate(userId, options);
    }
}
