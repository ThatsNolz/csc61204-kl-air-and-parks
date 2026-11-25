package cityair.controller;

import cityair.observer.AlertMonitor;
import cityair.observer.PollutionObserver;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Concrete Observer: receives a threshold crossing from {@link AlertMonitor}, which may run on a
 * background thread, and forwards it to the view on the UI executor with the current history.
 */
final class AlertBridge implements PollutionObserver {
    private final AlertMonitor monitor;
    private final Executor ui;
    private final MonitorController.View view;

    AlertBridge(AlertMonitor monitor, Executor ui, MonitorController.View view) {
        this.monitor = Objects.requireNonNull(monitor);
        this.ui = Objects.requireNonNull(ui);
        this.view = Objects.requireNonNull(view);
    }

    /** Hands the alert to the UI thread; Swing components must not be touched from the worker. */
    @Override public void onAlert(AlertMonitor.Alert alert) {
        ui.execute(() -> view.alert(monitor.history(), alert));
    }
}
