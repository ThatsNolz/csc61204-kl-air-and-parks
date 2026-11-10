package cityair.observer;

/** Listener for a newly crossed pollution threshold. */
@FunctionalInterface
public interface PollutionObserver {
    /** Receives the alert for a single crossing. */
    void onAlert(AlertMonitor.Alert alert);
}
