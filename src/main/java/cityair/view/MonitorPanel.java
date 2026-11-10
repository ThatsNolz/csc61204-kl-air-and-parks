package cityair.view;

import cityair.api.OpenMeteoClient;
import cityair.controller.MonitorController;
import cityair.model.CitizenReport;
import cityair.model.CitizenReport.Issue;
import cityair.model.CityCatalog;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Swing view for the pilot. Construct and update on the event dispatch thread. */
public final class MonitorPanel extends JPanel implements MonitorController.View {
    private static final int GAP = 12, PADDING = 20, FONT_SIZE = 15, TITLE_SIZE = 25;
    private static final int WINDOW_WIDTH = 920, WINDOW_HEIGHT = 680;
    private static final Color INK = new Color(20, 40, 49), ERROR = new Color(158, 38, 37);
    private static final String NO_DATA = "No verified data loaded. Use Refresh district.";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm z", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private final JTabbedPane tabs = named(new JTabbedPane(), "tabs");
    private final JComboBox<District> district = named(new JComboBox<>(District.values()), "district");
    private final JComboBox<AdviceProfile> profile = named(new JComboBox<>(AdviceProfile.values()), "profile");
    private final JComboBox<Issue> issue = named(new JComboBox<>(Issue.values()), "issue");
    private final JComboBox<District> reportDistrict = named(new JComboBox<>(District.values()), "reportDistrict");
    private final JButton refresh = named(new JButton("Refresh district"), "refresh");
    private final JButton save = named(new JButton("Save local report"), "save");
    private final JTextArea dashboard = area("dashboard"), comparison = area("comparison"), advice = area("advice");
    private final JTextArea reportHistory = area("reportHistory"), notifications = area("notifications");
    private final JTextArea description = area("description");
    private final JLabel status = named(new JLabel("Ready. Choose a district to start."), "status");
    private final JCheckBox subscription = named(new JCheckBox("Enable notifications for selected district"), "subscription");
    private final Map<District, ReadingService.Result> results = new EnumMap<>(District.class);
    private final Map<District, List<Reading>> trends = new EnumMap<>(District.class);
    private final Preferences preferences;
    private MonitorController controller;

    /** Creates a window-sized panel with an injected settings store. */
    public MonitorPanel(Preferences preferences) {
        super(new BorderLayout(GAP, GAP));
        this.preferences = preferences;
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        add(toolbar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        addTabs();
        description.setEditable(true);
        selectSaved();
        reportHistory.setText("No reports on this device.");
        notifications.setText("No alerts in this session.");
        renderSelection();
    }

    /** Connects the view's actions after the controller is constructed. */
    public void bind(MonitorController controller) {
        this.controller = controller;
        refresh.addActionListener(e -> controller.refresh(selectedDistrict()));
        save.addActionListener(e -> saveReport());
        district.addActionListener(e -> changeDistrict());
        profile.addActionListener(e -> changeProfile());
        subscription.addActionListener(e -> changeSubscription());
        for (District item : District.values()) controller.subscribe(item, subscribed(item));
        controller.loadReports();
    }

    private JPanel toolbar() {
        var header = new JPanel(new BorderLayout(GAP, GAP));
        var title = new JLabel("Kuala Lumpur Air & Parks");
        title.setFont(title.getFont().deriveFont(Font.BOLD, (float) TITLE_SIZE));
        header.add(title, BorderLayout.NORTH);
        var controls = new JPanel(new BorderLayout(GAP, GAP));
        controls.add(labelled("Pilot district", district), BorderLayout.CENTER);
        controls.add(refresh, BorderLayout.EAST);
        header.add(controls, BorderLayout.CENTER);
        return header;
    }

    private void addTabs() {
        tabs.addTab("Dashboard", new JScrollPane(dashboard));
        tabs.addTab("District monitoring", new JScrollPane(comparison));
        tabs.addTab("Advice & parks", adviceTab());
        tabs.addTab("Citizen reports", reportTab());
        tabs.addTab("Alerts & settings", alertsTab());
    }

    private JPanel adviceTab() {
        var panel = new JPanel(new BorderLayout(GAP, GAP));
        panel.add(labelled("Activity advice profile", profile), BorderLayout.NORTH);
        panel.add(new JScrollPane(advice), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reportTab() {
        var panel = new JPanel(new GridLayout(2, 1, GAP, GAP));
        var form = new JPanel(new BorderLayout(GAP, GAP));
        var selectors = new JPanel(new GridLayout(1, 2, GAP, GAP));
        selectors.add(labelled("Report district", reportDistrict));
        selectors.add(labelled("Issue type", issue));
        form.add(selectors, BorderLayout.NORTH);
        form.add(labelled("Observation (10–500 characters). Avoid personal information.", new JScrollPane(description)), BorderLayout.CENTER);
        form.add(save, BorderLayout.SOUTH);
        panel.add(form);
        panel.add(labelled("Local report history", new JScrollPane(reportHistory)));
        return panel;
    }

    private JPanel alertsTab() {
        var panel = new JPanel(new BorderLayout(GAP, GAP));
        panel.add(subscription, BorderLayout.NORTH);
        panel.add(new JScrollPane(notifications), BorderLayout.CENTER);
        panel.add(new JLabel("Threshold: U.S. AQI " + AlertMonitor.DEFAULT_THRESHOLD + ". The last district and advice profile are saved."), BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel labelled(String text, JComponent component) {
        var panel = new JPanel(new BorderLayout(GAP, GAP));
        var label = new JLabel(text);
        label.setLabelFor(component instanceof JScrollPane scroll ? scroll.getViewport().getView() : component);
        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static <T extends JComponent> T named(T control, String name) {
        control.setName(name);
        control.getAccessibleContext().setAccessibleName(name);
        return control;
    }

    private static JTextArea area(String name) {
        var field = named(new JTextArea(), name);
        field.setEditable(false);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, FONT_SIZE));
        field.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));
        field.setForeground(INK);
        return field;
    }

    private District selectedDistrict() { return (District) district.getSelectedItem(); }
    private AdviceProfile selectedProfile() { return (AdviceProfile) profile.getSelectedItem(); }

    private void selectSaved() {
        district.setSelectedItem(savedEnum(District.class, "district", District.TITIWANGSA));
        profile.setSelectedItem(savedEnum(AdviceProfile.class, "profile", AdviceProfile.GENERAL));
        reportDistrict.setSelectedItem(selectedDistrict());
    }

    private <E extends Enum<E>> E savedEnum(Class<E> type, String key, E fallback) {
        try {
            return Enum.valueOf(type, preferences.get(key, fallback.name()));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private boolean subscribed(District item) { return preferences.getBoolean("alert." + item.name(), false); }

    private void changeDistrict() {
        preferences.put("district", selectedDistrict().name());
        reportDistrict.setSelectedItem(selectedDistrict());
        renderSelection();
        message("Showing " + selectedDistrict() + ".", false);
    }

    private void changeProfile() {
        preferences.put("profile", selectedProfile().name());
        renderAdvice(results.get(selectedDistrict()));
    }

    private void changeSubscription() {
        District item = selectedDistrict();
        boolean enabled = subscription.isSelected();
        preferences.putBoolean("alert." + item.name(), enabled);
        controller.subscribe(item, enabled);
        message((enabled ? "Notifications enabled for " : "Notifications disabled for ") + item + ".", false);
    }

    private void saveReport() {
        controller.saveReport((District) reportDistrict.getSelectedItem(), (Issue) issue.getSelectedItem(), description.getText());
    }

    /** Disables refresh and district selection during an active network request. */
    @Override public void loading(boolean value) {
        refresh.setEnabled(!value);
        district.setEnabled(!value);
        if (value) message("Retrieving modeled air quality…", false);
    }

    /** Updates the district result, dashboard, advice and comparison. */
    @Override public void reading(District item, ReadingService.Result result, List<Reading> recent) {
        results.put(item, result);
        trends.put(item, List.copyOf(recent));
        renderSelection();
        message(result.message(), result.state() != ReadingService.State.LIVE);
    }

    private void renderSelection() {
        var result = results.get(selectedDistrict());
        setText(dashboard, dashboardText(result));
        renderComparison();
        renderAdvice(result);
        subscription.setSelected(subscribed(selectedDistrict()));
    }

    private String dashboardText(ReadingService.Result result) {
        if (result == null) return selectedDistrict() + "\n\n" + NO_DATA;
        if (result.reading() == null) return selectedDistrict() + "\n\nUNAVAILABLE\n" + result.message();
        var data = result.reading();
        return data.district() + "\n\nU.S. AQI " + data.aqi() + " · " + data.category()
                + "\nSource: " + source(result) + "\nModel time: " + time(data.validAt())
                + "\nFetched: " + time(data.fetchedAt()) + "\n\nPM2.5: " + pollutant(data.pm25())
                + "\nPM10: " + pollutant(data.pm10()) + "\nNitrogen dioxide: " + pollutant(data.nitrogenDioxide())
                + "\nOzone: " + pollutant(data.ozone()) + "\n\n" + selectedProfile().strategy().advice(data.category())
                + "\n\nOpen-Meteo / Copernicus CAMS Global model. Not Malaysia's official API index or a local sensor.";
    }

    private void renderComparison() {
        var text = new StringBuilder("Latest loaded district values\n\n");
        for (District item : District.values()) text.append(item).append(" — ").append(comparisonLine(item)).append('\n');
        text.append("\nRecent live model hours in this session for ").append(selectedDistrict()).append("\n");
        var recent = trends.getOrDefault(selectedDistrict(), List.of());
        if (recent.isEmpty()) text.append("No recent live observations.");
        for (Reading item : recent) text.append(time(item.validAt())).append(" · U.S. AQI ").append(item.aqi()).append('\n');
        setText(comparison, text.append("\nNearby districts can share one modeled grid cell.").toString());
    }

    private String comparisonLine(District item) {
        var result = results.get(item);
        if (result == null || result.reading() == null) return "No verified data loaded";
        return "U.S. AQI " + result.reading().aqi() + " · " + source(result) + " · " + time(result.reading().validAt());
    }

    private void renderAdvice(ReadingService.Result result) {
        var text = new StringBuilder(selectedProfile() + " · " + selectedDistrict() + "\n\n");
        if (result == null || result.reading() == null) text.append("No verified reading. Check local guidance before planning.");
        else text.append(source(result)).append(" · ").append(time(result.reading().validAt())).append("\n")
                .append(selectedProfile().strategy().advice(result.reading().category()));
        for (CityCatalog.Park park : CityCatalog.nearby(selectedDistrict())) {
            text.append("\n\nNearby green space: ").append(park.name()).append("\n").append(park.area())
                    .append("\nSource: ").append(park.source());
        }
        setText(advice, text.append("\n\nDistrict model context only. This guidance is informational.").toString());
    }

    /** Updates the local report history. */
    @Override public void reports(List<CitizenReport> entries) {
        var text = new StringBuilder();
        for (var entry : entries) text.append(time(entry.createdAt())).append(" · ").append(entry.district())
                .append(" · ").append(entry.issue()).append("\n").append(entry.description()).append("\n\n");
        setText(reportHistory, text.isEmpty() ? "No reports on this device." : text.toString());
    }

    /** Displays a message using text and contrast rather than colour alone. */
    @Override public void message(String message, boolean error) {
        status.setForeground(error ? ERROR : INK);
        status.setText((error ? "Notice: " : "") + message);
        if ("Report saved on this device.".equals(message)) description.setText("");
    }

    /** Displays session crossing history and a banner for a delivered alert. */
    @Override public void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert delivered) {
        var text = new StringBuilder();
        for (var alert : history) text.append(time(alert.validAt())).append(" · ").append(alert.district())
                .append(" · AQI ").append(alert.aqi()).append(" crossed ").append(alert.threshold()).append('\n');
        setText(notifications, text.isEmpty() ? "No alerts in this session." : text.toString());
        if (delivered != null) message("Pollution alert for " + delivered.district() + ": U.S. AQI " + delivered.aqi(), true);
    }

    private static String source(ReadingService.Result result) {
        return result.state() == ReadingService.State.CACHED ? "CACHED (last verified response)" : "LIVE API (modeled estimate)";
    }

    private static String time(Instant value) { return TIME.format(value); }

    private static String pollutant(Double value) {
        return value == null ? "Unavailable" : String.format(Locale.ENGLISH, "%.1f µg/m³", value);
    }

    private static void setText(JTextArea area, String text) {
        area.setText(text);
        area.setCaretPosition(0);
    }
}
