package store.artistictech.apiaggregator.model;

/** Per-service simulation overrides parsed from request params; null means "use the service's own default". */
public record ServiceSimulationParams(Long delayMs, Boolean fail) {

    public static final ServiceSimulationParams NONE = new ServiceSimulationParams(null, null);

    public long delayMsOrDefault(long defaultDelayMs) {
        return delayMs == null ? defaultDelayMs : delayMs.longValue();
    }

    public boolean failOrDefault(boolean defaultFail) {
        return fail == null ? defaultFail : fail.booleanValue();
    }
}
