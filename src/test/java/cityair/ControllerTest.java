package cityair;

import cityair.api.ApiFailure;
import cityair.controller.MonitorController;
import cityair.model.CitizenReport;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static cityair.DomainTest.reading;
import static cityair.model.District.TITIWANGSA;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    @TempDir Path folder;
    private final AlertMonitor alerts = new AlertMonitor(101);
    private final View view = new View();

    @Test void refresh_queuedWorker_keepsViewBusyUntilCompleted() {
        var queue = new ArrayDeque<Runnable>();
        var ctl = controller(d -> reading(d, 101, 0), queue::add);
        ctl.refresh(TITIWANGSA);
        assertTrue(view.busy);
        assertNull(view.result);
        queue.remove().run();
        assertAll(() -> assertFalse(view.busy), () -> assertEquals(101, view.result.reading().aqi()));
    }
    @Test void refresh_unavailableApi_displaysUnavailableWithoutIndex() {
        controller(d -> { throw new ApiFailure(ApiFailure.Kind.SERVER, "Temporary outage.", null); }, Runnable::run)
                .refresh(TITIWANGSA);
        assertEquals(ReadingService.State.UNAVAILABLE, view.result.state());
    }
    @Test void refresh_unexpectedGatewayFailure_restoresControls() {
        controller(d -> { throw new IllegalStateException("fixture"); }, Runnable::run).refresh(TITIWANGSA);
        assertAll(() -> assertFalse(view.busy), () -> assertTrue(view.error), () -> assertNotNull(view.message));
    }
    @Test void refreshAll_queuedTwice_makesOneOrderedBatch() {
        var queue = new ArrayDeque<Runnable>();
        var visits = new java.util.ArrayList<District>();
        var ctl = controller(d -> { visits.add(d); return reading(d, 55, 0); }, queue::add);
        ctl.refreshAll();
        ctl.refresh(TITIWANGSA);
        assertTrue(view.busy);
        assertEquals(1, queue.size());
        queue.remove().run();
        assertAll(() -> assertEquals(List.of(District.values()), visits), () -> assertFalse(view.busy));
    }
    @Test void refreshAll_oneDistrictFails_deliversOtherDistricts() {
        var ctl = controller(d -> {
            if (d == TITIWANGSA) throw new IllegalStateException("controlled crash");
            return reading(d, 55, 0);
        }, Runnable::run);
        ctl.refreshAll();
        assertAll(() -> assertFalse(view.busy), () -> assertEquals(2, view.reads),
                () -> assertEquals(District.PERDANA, view.result.reading().district()));
    }
    @Test void refresh_workerRejects_releasesBusyControls() {
        controller(d -> reading(d, 55, 0), r -> { throw new java.util.concurrent.RejectedExecutionException(); })
                .refresh(TITIWANGSA);
        assertAll(() -> assertFalse(view.busy), () -> assertTrue(view.error));
    }
    @Test void saveReport_validInput_displaysPersistedEntry() {
        controller(d -> reading(d, 40, 0), Runnable::run)
                .saveReport(TITIWANGSA, CitizenReport.Issue.DUST, "Dust observed at the entrance.");
        assertEquals(1, view.entries.size());
    }
    @Test void saveReport_invalidDescription_doesNotWriteFile() {
        controller(d -> reading(d, 40, 0), Runnable::run).saveReport(TITIWANGSA, CitizenReport.Issue.DUST, "short");
        assertAll(() -> assertTrue(view.error), () -> assertFalse(Files.exists(folder.resolve("reports.json"))));
    }
    @Test void saveReport_missingDistrict_displaysFriendlyError() {
        controller(d -> reading(d, 40, 0), Runnable::run).saveReport(null, CitizenReport.Issue.DUST, "Valid length.");
        assertEquals("Choose a district and issue.", view.message);
    }
    @Test void saveReport_whileAnotherSaveIsPending_persistsOnlyOneObservation() {
        var queue = new ArrayDeque<Runnable>();
        var ctl = controller(d -> reading(d, 55, 0), queue::add);
        ctl.saveReport(TITIWANGSA, CitizenReport.Issue.SMOKE, "First report near the park.");
        ctl.saveReport(TITIWANGSA, CitizenReport.Issue.SMOKE, "Second button click.");
        assertAll(() -> assertTrue(view.saving), () -> assertEquals(1, queue.size()));
        queue.remove().run();
        assertAll(() -> assertFalse(view.saving), () -> assertEquals(1, view.entries.size()));
    }
    @Test void saveReport_workerRejects_unlocksFormWithClearMessage() {
        controller(d -> reading(d, 55, 0), r -> { throw new java.util.concurrent.RejectedExecutionException(); })
                .saveReport(TITIWANGSA, CitizenReport.Issue.DUST, "Dust observed at the entrance.");
        assertAll(() -> assertFalse(view.saving), () -> assertTrue(view.error));
    }
    @Test void subscribe_crossing_dispatchesAlertToView() {
        var ctl = controller(d -> reading(d, 110, 0), Runnable::run);
        ctl.subscribe(TITIWANGSA, true);
        ctl.refresh(TITIWANGSA);
        ctl.subscribe(TITIWANGSA, false);
        assertEquals(110, view.delivered.aqi());
    }
    @Test void loadReports_corruptStorage_displaysFriendlyError() throws Exception {
        Files.writeString(folder.resolve("reports.json"), "{broken");
        controller(d -> reading(d, 40, 0), Runnable::run).loadReports();
        assertEquals("Local report history could not be read.", view.message);
    }

    private MonitorController controller(cityair.api.AirQualityGateway gateway, java.util.concurrent.Executor worker) {
        return new MonitorController(new ReadingService(gateway, new ReadingCache(folder.resolve("cache")), alerts),
                new ReportStore(folder), alerts, worker, Runnable::run, Clock.systemUTC(), view);
    }

    private static final class View implements MonitorController.View {
        boolean busy, error, saving;
        int reads;
        String message;
        List<CitizenReport> entries = List.of();
        ReadingService.Result result;
        AlertMonitor.Alert delivered;
        public void loading(boolean value) { busy = value; }
        public void reading(District d, ReadingService.Result r, List<Reading> recent) { result = r; reads++; }
        public void reportSaving(boolean value) { saving = value; }
        public void reports(List<CitizenReport> entries) { this.entries = entries; }
        public void message(String m, boolean error) { message = m; this.error = error; }
        public void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert alert) { if (alert != null) delivered = alert; }
    }
}
