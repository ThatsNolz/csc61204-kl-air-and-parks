package cityair;

import cityair.controller.MonitorController;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import cityair.view.MonitorPanel;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Skin;
import java.awt.Component;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static cityair.DomainTest.reading;
import static org.junit.jupiter.api.Assertions.*;

class SwingFlowTest {
    @TempDir Path folder;

    @AfterEach void resetAppearance() {
        Skin.setTheme(AuroraTheme.DARK);
        Skin.setReducedMotion(false);
    }

    @Test void refresh_dashboardAndComparison_showReadingAndTimestamp() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            assertEquals("110", label(p, "aqiValue").getText());
            assertEquals(110, table(p, "districtTable").getValueAt(0, 1));
            assertEquals(110, table(p, "sampleTable").getValueAt(0, 1));
            assertEquals("Unavailable", label(p, "pm10Value").getText());
            combo(p, "district").setSelectedItem(District.PERDANA);
            assertTrue(text(p, "dashboard").contains("No verified data loaded"));
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
            chart.setSize(340, 200);
            var image = new java.awt.image.BufferedImage(340, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            try { chart.paint(g); } finally { g.dispose(); }
            assertAll(() -> assertEquals("—", table(p, "sampleTable").getValueAt(0, 3)),
                    () -> assertNotEquals(image.getRGB(0, 0), image.getRGB(200, 100)),
                    () -> assertTrue(chart.getAccessibleContext().getAccessibleDescription().contains("table")));
        });
    }

    @Test void shortcut_ctrlRefresh_usesTheSameSelectedDistrictAction() throws Exception {
        usePanel(p -> {
            combo(p, "district").setSelectedItem(District.PERDANA);
            p.getActionMap().get("refreshSelected").actionPerformed(new java.awt.event.ActionEvent(p, 0, "refresh"));
            assertEquals("110", label(p, "aqiValue").getText());
            assertEquals(110, table(p, "districtTable").getValueAt(2, 1));
        });
    }

    @Test void reading_controlledView_labelsTheFixtureAndNeverCallsItLive() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var p = new MonitorPanel(Preferences.userRoot().node("csc61204-test/fixture-label"), true);
            var sample = reading(District.TITIWANGSA, 42, 0);
            p.reading(District.TITIWANGSA, new ReadingService.Result(ReadingService.State.LIVE, sample, "Provider ok"),
                    java.util.List.of(sample));
            assertEquals("TEST FIXTURE", label(p, "readingSource").getText());
            assertFalse(text(p, "dashboard").contains("LIVE API"));
        });
    }

    @Test void advice_profileSwitch_rendersDifferentAdviceAndPark() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            String general = text(p, "advice");
            combo(p, "profile").setSelectedItem(AdviceProfile.CHILDREN);
            assertNotEquals(general, text(p, "advice"));
            assertTrue(text(p, "advice").contains("Titiwangsa Lake Park"));
        });
    }

    @Test void save_invalidThenValidObservation_updatesTheRealFormAndHistory() throws Exception {
        usePanel(p -> {
            button(p, "save").doClick(0);
            assertTrue(label(p, "status").getText().contains("10 to 500"));
            area(p, "description").setText("Observed smoke near the entrance.");
            button(p, "save").doClick(0);
            assertEquals("Observed smoke near the entrance.", table(p, "reportTable").getValueAt(0, 3));
            assertEquals("", area(p, "description").getText());
        });
    }

    @Test void subscribe_refreshAboveThreshold_displaysAlertBannerAndHistory() throws Exception {
        usePanel(p -> {
            ((JCheckBox) find(p, "subscription.TITIWANGSA")).doClick(0);
            button(p, "refresh").doClick(0);
            assertAll(() -> assertEquals(110, table(p, "alertTable").getValueAt(0, 2)),
                    () -> assertEquals(101, table(p, "alertTable").getValueAt(0, 3)),
                    () -> assertTrue(label(p, "latestAlert").getText().contains("Pollution alert")));
        });
    }

    @Test void reportFilter_selectedDistrict_showsOnlyMatchingReports() throws Exception {
        usePanel(p -> {
            area(p, "description").setText("Dust from roadworks near the lake.");
            button(p, "save").doClick(0);
            combo(p, "reportDistrict").setSelectedItem(District.PERDANA);
            area(p, "description").setText("Smoke drifting over the garden.");
            button(p, "save").doClick(0);
            combo(p, "reportFilter").setSelectedItem(District.PERDANA);
            var reports = table(p, "reportTable");
            assertAll(() -> assertEquals(1, reports.getRowCount()),
                    () -> assertEquals("Smoke drifting over the garden.", reports.getValueAt(0, 3)));
        });
    }

    @Test void threshold_loweredBeforeRefresh_deliversAlertForModerateReading() throws Exception {
        usePanel(p -> {
            ((JCheckBox) find(p, "subscription.TITIWANGSA")).doClick(0);
            ((JSpinner) find(p, "threshold")).setValue(100);
            button(p, "refresh").doClick(0);
            assertEquals(100, table(p, "alertTable").getValueAt(0, 3));
        });
    }

    @Test void advice_comparisonRow_appliesEveryStrategyToTheSameReading() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            var summary = text(p, "adviceSummary");
            assertAll(() -> assertTrue(summary.contains("Reduce prolonged") || summary.contains("Routine")),
                    () -> assertEquals("Unhealthy for sensitive groups", label(p, "aqiCategory").getText()));
        });
    }

    @Test void planner_verdictAndReason_followTheSelectedReadingAndAudience() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            String general = label(p, "planVerdict").getText();
            String reason = text(p, "planReason");
            combo(p, "profile").setSelectedItem(AdviceProfile.CHILDREN);
            assertAll(() -> assertFalse(general.isBlank()),
                    () -> assertTrue(reason.contains("exposure score"), reason),
                    () -> assertTrue(text(p, "planReason").contains("Children")),
                    () -> assertNotEquals(reason, text(p, "planReason")));
        });
    }

    @Test void insights_afterRefresh_describeTheThresholdAndThePollutants() throws Exception {
        usePanel(p -> {
            button(p, "refresh").doClick(0);
            String insights = find(p, "insights").getAccessibleContext().getAccessibleDescription();
            assertAll(() -> assertTrue(insights.contains("threshold"), insights),
                    () -> assertTrue(insights.contains("WHO"), insights));
        });
    }

    @Test void theme_toggle_switchesBothWaysAndIsSaved() throws Exception {
        usePanel(p -> {
            assertEquals(AuroraTheme.DARK, Skin.theme());
            button(p, "themeToggle").doClick(0);
            assertEquals(AuroraTheme.LIGHT, Skin.theme());
            assertTrue(label(p, "status").getText().contains("Aurora Light"));
            p.getActionMap().get("toggleTheme").actionPerformed(new java.awt.event.ActionEvent(p, 0, "theme"));
            assertEquals(AuroraTheme.DARK, Skin.theme());
        });
    }

    @Test void reducedMotion_whenSwitchedOn_appliesToEveryAnimation() throws Exception {
        usePanel(p -> {
            ((JCheckBox) find(p, "reducedMotion")).doClick(0);
            assertAll(() -> assertTrue(Skin.reducedMotion()),
                    () -> assertEquals(0, Skin.duration(400)),
                    () -> assertTrue(label(p, "status").getText().contains("Motion reduced")));
        });
    }

    @Test void navigation_everyScreen_isReachableAndRendersWithoutError() throws Exception {
        usePanel(p -> {
            var tabs = (JTabbedPane) find(p, "tabs");
            button(p, "refresh").doClick(0);
            assertEquals(7, tabs.getTabCount());
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setSelectedIndex(i);
                assertNotNull(tabs.getComponentAt(i));
                assertTrue(tabs.getComponentAt(i).isVisible(), "screen " + i + " is not visible");
            }
        });
    }

    private void usePanel(java.util.function.Consumer<MonitorPanel> work) throws Exception {
        var prefs = Preferences.userRoot().node("csc61204-test/" + UUID.randomUUID());
        try {
            SwingUtilities.invokeAndWait(() -> {
                var panel = new MonitorPanel(prefs);
                var monitor = new AlertMonitor(101);
                var service = new ReadingService(d -> reading(d, 110, 0), new ReadingCache(folder.resolve("cache")),
                        monitor);
                panel.bind(new MonitorController(service, new ReportStore(folder), monitor, Runnable::run,
                        Runnable::run, Clock.systemUTC(), panel));
                work.accept(panel);
            });
        } finally { prefs.removeNode(); }
    }

    static Component find(Component component, String name) {
        return Components.find(component, name);
    }

    /** Reads rendered text from a label or any text component, so a widget change cannot hide it. */
    static String text(Component root, String name) {
        Component found = find(root, name);
        if (found instanceof JLabel label) return label.getText();
        if (found instanceof JTextComponent field) return field.getText();
        throw new AssertionError(name + " is not a text component: " + found);
    }

    private static JTextComponent area(MonitorPanel p, String n) { return (JTextComponent) find(p, n); }
    private static JLabel label(MonitorPanel p, String n) { return (JLabel) find(p, n); }
    private static AbstractButton button(MonitorPanel p, String n) { return (AbstractButton) find(p, n); }
    private static JComboBox<?> combo(MonitorPanel p, String n) { return (JComboBox<?>) find(p, n); }
    private static JTable table(MonitorPanel p, String n) { return (JTable) find(p, n); }
}
