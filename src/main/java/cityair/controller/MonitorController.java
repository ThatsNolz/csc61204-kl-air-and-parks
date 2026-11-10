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

    /** Output contract independent of concrete Swing controls. */
    public interface View {
        /** Presents busy feedback and prevents concurrent refreshes. */
        void loading(boolean value);
        /** Displays a refresh outcome and its recent model hours. */
        void reading(District district, ReadingService.Result result, List<Reading> recent);
        /** Displays the saved report list. */
        void reports(List<CitizenReport> entries);
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
        view.loading(true);
        background.execute(() -> fetch(district));
    }

    private void fetch(District district) {
        try {
            var result = readings.refresh(district);
            var history = readings.history(district);
            ui.execute(() -> finish(district, result, history));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Refresh failed unexpectedly", ex);
            ui.execute(() -> refreshFailed());
        }
    }

    private void refreshFailed() {
        view.loading(false);
        view.message("The refresh could not be completed.", true);
    }

    private void finish(District district, ReadingService.Result result, List<Reading> history) {
        view.loading(false);
        view.reading(district, result, history);
        view.alert(alerts.history(), null);
    }

    /** Validates input before scheduling local persistence. */
    public void saveReport(District district, Issue issue, String text) {
        try {
            var report = new CitizenReport(UUID.randomUUID(), district, issue, text, clock.instant());
            background.execute(() -> persist(report));
        } catch (IllegalArgumentException | NullPointerException ex) {
            view.message(ex instanceof IllegalArgumentException ? ex.getMessage() : "Choose a district and issue.", true);
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
        }
    }

    private void showReports(boolean saved) {
        try {
            var entries = reports.list();
            ui.execute(() -> {
                view.reports(entries);
                if (saved) view.message("Report saved on this device.", false);
            });
        } catch (IOException ex) {
            reportFailure(ex, "Local report history could not be read.");
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
