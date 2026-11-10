package cityair.controller;

import cityair.model.CitizenReport;
import cityair.model.CitizenReport.Issue;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.observer.PollutionObserver;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Handles view actions and dispatches I/O away from Swing's event thread. */
public final class MonitorController {
    private static final Logger LOGGER = Logger.getLogger(MonitorController.class.getName());
    private final ReadingService readings;
    private final ReportStore reports;
    private final AlertMonitor alerts;
    private final Executor background;
    private final Executor ui;
    private final Clock clock;
    private final View view;
    private final PollutionObserver listener;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final AtomicBoolean saving = new AtomicBoolean();

    /** Output contract independent of concrete Swing controls. */
    public interface View {
        /** Presents busy feedback and prevents concurrent refreshes. */
        void loading(boolean value);
        /** Displays a refresh outcome and its recent model hours. */
        void reading(District district, ReadingService.Result result, List<Reading> recent);
        /** Displays the saved report list. */
        void reports(List<CitizenReport> entries);
        /** Locks or unlocks the report form while a save is pending. */
        default void reportSaving(boolean value) {}
        /** Shows a brief success or error message. */
        void message(String message, boolean error);
        /** Displays the alert history and latest delivered notification. */
        void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert delivered);
    }

    /** Supplies I/O dispatch, UI dispatch, clock and the view to update. */
    public MonitorController(ReadingService readings, ReportStore reports, AlertMonitor alerts,
                             Executor background, Executor ui, Clock clock, View view) {
        this.readings = Objects.requireNonNull(readings);
        this.reports = Objects.requireNonNull(reports);
        this.alerts = Objects.requireNonNull(alerts);
        this.background = Objects.requireNonNull(background);
        this.ui = Objects.requireNonNull(ui);
        this.clock = Objects.requireNonNull(clock);
        this.view = Objects.requireNonNull(view);
        listener = alert -> ui.execute(() -> view.alert(alerts.history(), alert));
    }

    /** Schedules retrieval and displays its result on the UI executor. */
    public void refresh(District district) {
        refresh(List.of(district));
    }

    /** Schedules a sequential comparison of all pilot areas without overlapping requests. */
    public void refreshAll() {
        refresh(List.of(District.values()));
    }

    private void refresh(List<District> districts) {
        if (!refreshing.compareAndSet(false, true)) return;
        view.loading(true);
        try {
            background.execute(() -> runRefresh(districts));
        } catch (RejectedExecutionException ex) {
            finishRefresh();
            view.message("Refresh unavailable while the application is closing.", true);
        }
    }

    private void runRefresh(List<District> districts) {
        try {
            for (District district : districts) {
                if (Thread.currentThread().isInterrupted()) break;
                fetch(district);
            }
        } finally {
            ui.execute(this::finishRefresh);
        }
    }

    private void fetch(District district) {
        try {
            var result = readings.refresh(district);
            var history = readings.history(district);
            ui.execute(() -> showReading(district, result, history));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Refresh failed unexpectedly", ex);
            ui.execute(() -> view.message("Could not refresh " + district + ".", true));
        }
    }

    private void finishRefresh() {
        refreshing.set(false);
        view.loading(false);
    }

    private void showReading(District district, ReadingService.Result result, List<Reading> history) {
        view.reading(district, result, history);
        view.alert(alerts.history(), null);
    }

    /** Validates input before scheduling local persistence. */
    public void saveReport(District district, Issue issue, String text) {
        if (saving.get()) return;
        try {
            var report = new CitizenReport(UUID.randomUUID(), district, issue, text, clock.instant());
            queueSave(report);
        } catch (IllegalArgumentException | NullPointerException ex) {
            view.message(ex instanceof IllegalArgumentException ? ex.getMessage() : "Choose a district and issue.", true);
        }
    }

    private void queueSave(CitizenReport report) {
        if (!saving.compareAndSet(false, true)) return;
        view.reportSaving(true);
        try {
            background.execute(() -> persist(report));
        } catch (RejectedExecutionException ex) {
            finishSave();
            view.message("Save unavailable while the application is closing.", true);
        }
    }

    /** Loads local report history on the background executor. */
    public void loadReports() { background.execute(() -> showReports(false)); }

    private void persist(CitizenReport report) {
        try {
            reports.save(report);
            showReports(true);
        } catch (IOException ex) {
            reportFailure(ex, "The report could not be saved. Check local storage permissions.");
        } finally {
            ui.execute(this::finishSave);
        }
    }

    private void finishSave() {
        saving.set(false);
        view.reportSaving(false);
    }

    private void showReports(boolean saved) {
        try {
            var entries = reports.list();
            ui.execute(() -> {
                view.reports(entries);
                if (saved) view.message("Report saved on this device.", false);
            });
        } catch (IOException ex) {
            reportFailure(ex, saved ? "Report saved, but history could not be reloaded." : "Local report history could not be read.");
        }
    }

    private void reportFailure(IOException ex, String message) {
        LOGGER.log(Level.WARNING, message, ex);
        ui.execute(() -> view.message(message, true));
    }

    /** Enables or disables desktop notifications for one district. */
    public void subscribe(District district, boolean enabled) {
        if (enabled) alerts.subscribe(district, listener);
        else alerts.unsubscribe(district, listener);
    }
}
