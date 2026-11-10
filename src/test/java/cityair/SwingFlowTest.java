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
            assertEquals("110", ((JLabel)find(p, "aqiValue")).getText());
            assertEquals(110, table(p, "districtTable").getValueAt(0, 1));
            assertEquals(110, table(p, "sampleTable").getValueAt(0, 1));
            assertEquals("Unavailable", ((JLabel)find(p, "pm10Value")).getText());
            combo(p, "district").setSelectedItem(District.PERDANA);
            assertTrue(area(p, "dashboard").getText().contains("No verified data loaded"));
        });
    }

    @Test void refreshAll_emptyComparison_populatesEveryPilotArea() throws Exception {
        usePanel(p -> {
            var districts = table(p, "districtTable");
            assertEquals("Not loaded", districts.getValueAt(0, 3));
            button(p, "refreshAll").doClick(0);
            assertAll(() -> assertEquals(District.values().length, districts.getRowCount()),
                    () -> assertEquals(110, districts.getValueAt(2, 1)),
                    () -> assertFalse(districts.isCellEditable(0, 1)),
                    () -> assertEquals(1, table(p, "sampleTable").getRowCount()));
        });
    }

    @Test void refresh_chartRendering_keepsMissingValuesOutOfTheDataset() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            var chart = find(p, "trendChart");
            chart.setSize(340, 180);
            var image = new java.awt.image.BufferedImage(340, 180, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            try { chart.paint(g); } finally { g.dispose(); }
            assertAll(() -> assertEquals("—", table(p, "sampleTable").getValueAt(0, 3)),
                    () -> assertNotEquals(image.getRGB(0, 0), image.getRGB(50, 20)),
                    () -> assertTrue(chart.getAccessibleContext().getAccessibleName().contains("exact values")));
        });
    }

    @Test void shortcut_ctrlRefresh_usesTheSameSelectedDistrictAction() throws Exception {
        usePanel(p -> {
            combo(p, "district").setSelectedItem(District.PERDANA);
            p.getActionMap().get("refreshSelected").actionPerformed(new java.awt.event.ActionEvent(p, 0, "refresh"));
            assertEquals("110", ((JLabel)find(p, "aqiValue")).getText());
            assertEquals(110, table(p, "districtTable").getValueAt(2, 1));
        });
    }

    @Test void reading_controlledView_labelsTheFixtureAndNeverCallsItLive() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var p = new MonitorPanel(Preferences.userRoot().node("csc61204-test/fixture-label"), true);
            var sample = reading(District.TITIWANGSA, 42, 0);
            p.reading(District.TITIWANGSA, new ReadingService.Result(ReadingService.State.LIVE, sample, "Provider ok"), java.util.List.of(sample));
            assertEquals("TEST FIXTURE", ((JLabel)find(p, "readingSource")).getText());
            assertFalse(area(p, "dashboard").getText().contains("LIVE API"));
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
    private static JTable table(MonitorPanel p, String n) { return (JTable) find(p, n); }
}
