package store.artistictech.apiaggregator.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-request simulation overrides keyed by service name, parsed generically from request params
 * named "{service}DelayMs" and "fail{Service}". Adding a new DownstreamService automatically
 * supports its own override params with no changes here.
 */
public record AggregationOptions(Map<String, ServiceSimulationParams> overrides) {

    private static final String DELAY_SUFFIX = "DelayMs";
    private static final String FAIL_PREFIX = "fail";

    public static final AggregationOptions EMPTY = new AggregationOptions(Map.of());

    public ServiceSimulationParams forService(String serviceName) {
        return overrides.getOrDefault(serviceName, ServiceSimulationParams.NONE);
    }

    public static AggregationOptions fromRequestParams(Map<String, String> requestParams) {
        Map<String, Long> delayOverrides = new HashMap<>();
        Map<String, Boolean> failOverrides = new HashMap<>();

        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(DELAY_SUFFIX) && key.length() > DELAY_SUFFIX.length()) {
                String serviceName = decapitalize(key.substring(0, key.length() - DELAY_SUFFIX.length()));
                delayOverrides.put(serviceName, Long.valueOf(entry.getValue()));
            } else if (key.startsWith(FAIL_PREFIX) && key.length() > FAIL_PREFIX.length()) {
                String serviceName = decapitalize(key.substring(FAIL_PREFIX.length()));
                failOverrides.put(serviceName, Boolean.valueOf(entry.getValue()));
            }
        }

        Set<String> serviceNames = new HashSet<>();
        serviceNames.addAll(delayOverrides.keySet());
        serviceNames.addAll(failOverrides.keySet());

        Map<String, ServiceSimulationParams> overrides = new HashMap<>();
        for (String serviceName : serviceNames) {
            overrides.put(serviceName, new ServiceSimulationParams(
                    delayOverrides.get(serviceName), failOverrides.get(serviceName)));
        }
        return new AggregationOptions(overrides);
    }

    private static String decapitalize(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
