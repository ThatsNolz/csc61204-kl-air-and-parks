package cityair;

import cityair.controller.MonitorController;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import cityair.view.MonitorPanel;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static cityair.DomainTest.reading;
import static org.junit.jupiter.api.Assertions.*;

class SwingFlowTest {
    @TempDir Path folder;

    @Test void refresh_dashboardAndComparison_showReadingAndTimestamp() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            assertTrue(area(p, "dashboard").getText().contains("U.S. AQI 110"));
            assertTrue(area(p, "comparison").getText().contains("Recent live model hours"));
            assertTrue(area(p, "dashboard").getText().contains("PM10: Unavailable"));
            combo(p, "district").setSelectedItem(District.PERDANA);
            assertTrue(area(p, "dashboard").getText().contains("No verified data loaded"));
        });
    }
    @Test void advice_profileSwitch_rendersDifferentAdviceAndPark() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            String general = area(p, "advice").getText();
            combo(p, "profile").setSelectedItem(AdviceProfile.CHILDREN);
            assertNotEquals(general, area(p, "advice").getText());
            assertTrue(area(p, "advice").getText().contains("Titiwangsa Lake Park"));
        });
    }
    @Test void save_invalidThenValidObservation_updatesTheRealFormAndHistory() throws Exception {
        usePanel(p -> {
            button(p, "save").doClick(0);
            assertTrue(((JLabel) find(p, "status")).getText().contains("10 to 500"));
            area(p, "description").setText("Observed smoke near the entrance.");
            button(p, "save").doClick(0);
            assertTrue(area(p, "reportHistory").getText().contains("Observed smoke"));
            assertEquals("", area(p, "description").getText());
        });
    }
    @Test void subscribe_refreshAboveThreshold_displaysAlertBannerAndHistory() throws Exception {
        usePanel(p -> {
            ((JCheckBox) find(p, "subscription")).doClick(0);
            button(p, "refresh").doClick(0);
            assertTrue(area(p, "notifications").getText().contains("crossed 101"));
            assertTrue(((JLabel) find(p, "latestAlert")).getText().contains("Pollution alert"));
        });
    }

    private void usePanel(java.util.function.Consumer<MonitorPanel> work) throws Exception {
        var prefs = Preferences.userRoot().node("csc61204-test/" + UUID.randomUUID());
        try {
            SwingUtilities.invokeAndWait(() -> {
                var panel = new MonitorPanel(prefs);
                var monitor = new AlertMonitor(101);
                var service = new ReadingService(d -> reading(d, 110, 0), new ReadingCache(folder.resolve("cache")), monitor);
                panel.bind(new MonitorController(service, new ReportStore(folder), monitor, Runnable::run,
                        Runnable::run, Clock.systemUTC(), panel));
                work.accept(panel);
            });
        } finally { prefs.removeNode(); }
    }

    static Component find(Component component, String name) {
        if (name.equals(component.getName())) return component;
        if (component instanceof Container container) {
            for (var child : container.getComponents()) {
                var found = find(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static JTextArea area(MonitorPanel p, String n) { return (JTextArea) find(p, n); }
    private static JButton button(MonitorPanel p, String n) { return (JButton) find(p, n); }
    private static JComboBox<?> combo(MonitorPanel p, String n) { return (JComboBox<?>) find(p, n); }
}
