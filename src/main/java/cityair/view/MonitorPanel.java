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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static cityair.view.DeskStyle.*;

/** Swing view for the pilot. Construct and update on the event dispatch thread. */
public final class MonitorPanel extends JPanel implements MonitorController.View {
    private static final int WINDOW_WIDTH = 1040, WINDOW_HEIGHT = 810, TAB_KEY_BASE = KeyEvent.VK_1, FORM_HEIGHT = 228;
    private static final String NO_DATA = "No verified data loaded. Use Refresh district.";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm z", Locale.ENGLISH)
            .withZone(OpenMeteoClient.ZONE);
    private final JTabbedPane tabs = named(new JTabbedPane(), "tabs");
    private final JComboBox<District> district = named(new JComboBox<>(District.values()), "district");
    private final JComboBox<AdviceProfile> profile = named(new JComboBox<>(AdviceProfile.values()), "profile");
    private final JComboBox<Issue> issue = named(new JComboBox<>(Issue.values()), "issue");
    private final JComboBox<District> reportDistrict = named(new JComboBox<>(District.values()), "reportDistrict");
    private final JButton refresh = named(new JButton("Refresh district"), "refresh");
    private final JButton refreshAll = named(new JButton("Compare all areas"), "refreshAll");
    private final JButton save = named(new JButton("Save local report"), "save");
    private final JTextArea dashboard = area("dashboard"), advice = area("advice");
    private final JTextArea reportHistory = area("reportHistory"), notifications = area("notifications");
    private final JTextArea description = area("description");
    private final JLabel counter = label("", BODY, Font.PLAIN, MUTED);
    private final JLabel status = named(new JLabel("Ready. Choose a district to start."), "status");
    private final JLabel latestAlert = named(new JLabel("No notifications delivered in this session."), "latestAlert");
    private final JProgressBar progress = new JProgressBar();
    private final JCheckBox subscription = named(new JCheckBox("Enable notifications for selected district"), "subscription");
    private final DashboardPane overview = new DashboardPane(dashboard);
    private final MonitoringPane monitoring = new MonitoringPane();
    private final Map<District, ReadingService.Result> results = new EnumMap<>(District.class);
    private final Map<District, List<Reading>> trends = new EnumMap<>(District.class);
    private final Preferences preferences;
    private final boolean fixture;
    private MonitorController controller;

    /** Creates a window-sized panel with an injected settings store. */
    public MonitorPanel(Preferences preferences) {
        this(preferences, false);
    }

    /** Creates a view with explicit test-fixture provenance when running controlled demonstrations. */
    public MonitorPanel(Preferences preferences, boolean fixture) {
        super(new BorderLayout(GAP, GAP));
        this.preferences = preferences;
        this.fixture = fixture;
        setBackground(CANVAS);
        setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        add(toolbar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(feedback(), BorderLayout.SOUTH);
        addTabs();
        configureForm();
        shortcuts();
        selectSaved();
        reportHistory.setText("No reports on this device.");
        notifications.setText("No alerts in this session.");
        renderSelection();
    }

    /** Connects the view's actions after the controller is constructed. */
    public void bind(MonitorController controller) {
        this.controller = controller;
        refresh.addActionListener(e -> controller.refresh(selectedDistrict()));
        refreshAll.addActionListener(e -> controller.refreshAll());
        save.addActionListener(e -> saveReport());
        district.addActionListener(e -> changeDistrict());
        profile.addActionListener(e -> changeProfile());
        subscription.addActionListener(e -> changeSubscription());
        for (District item : District.values()) controller.subscribe(item, subscribed(item));
        controller.loadReports();
    }

    private JPanel toolbar() {
        var header = panel(new BorderLayout(GAP, GAP));
        var brand = new JPanel(new GridLayout(0, 1));
        brand.setBackground(NAVY);
        brand.setBorder(new EmptyBorder(GAP, PAD, GAP, PAD));
        brand.add(label("Kuala Lumpur Air & Parks", TITLE, Font.BOLD, Color.WHITE));
        brand.add(label("AIR QUALITY AND GREEN SPACE MONITOR  /  KUALA LUMPUR PILOT", BODY - 2, Font.PLAIN, PALE));
        header.add(brand, BorderLayout.NORTH);
        header.add(controls(), BorderLayout.CENTER);
        return header;
    }

    private JPanel controls() {
        var controls = panel(new BorderLayout(GAP, GAP));
        var buttons = panel(new GridLayout(1, 2, GAP, GAP));
        styleButton(refresh, true);
        styleButton(refreshAll, false);
        buttons.add(refreshAll);
        buttons.add(refresh);
        controls.add(labelled("Pilot area", district), BorderLayout.CENTER);
        controls.add(buttons, BorderLayout.EAST);
        return controls;
    }

    private JPanel feedback() {
        var feedback = card(new BorderLayout(GAP, 0));
        var lines = new JPanel(new GridLayout(2, 1));
        lines.setOpaque(false);
        lines.add(status);
        lines.add(latestAlert);
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.getAccessibleContext().setAccessibleName("Refreshing modeled air quality");
        feedback.add(lines, BorderLayout.CENTER);
        feedback.add(progress, BorderLayout.EAST);
        return feedback;
    }

    private void addTabs() {
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, BODY));
        tabs.addTab("Overview", section("Air quality overview", "Latest loaded estimate for the selected pilot area.", overview));
        tabs.addTab("Monitoring", section("Monitor and compare", "Nearby areas can share a CAMS grid cell. Use Compare all areas to populate the table.", monitoring));
        tabs.addTab("Advice & parks", section("Plan an outing", "Advice by audience and a verified park for the selected pilot area.", adviceTab()));
        tabs.addTab("Citizen reports", section("Record an observation", "Reports stay on this device and are not submitted to any authority.", reportTab()));
        tabs.addTab("Alerts & settings", section("Pollution alerts", "Manage notifications for the selected pilot area.", alertsTab()));
        tabs.addTab("Data & impact", section("Understand the data", "Source, limits and the pilot's connection to SDG 11.", aboutTab()));
        for (int i = 0; i < tabs.getTabCount(); i++) tabs.setMnemonicAt(i, TAB_KEY_BASE + i);
    }

    private void shortcuts() {
        var key = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, "refreshSelected");
        getActionMap().put("refreshSelected", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { refresh.doClick(0); }
        });
    }

    private JPanel adviceTab() {
        var panel = card(new BorderLayout(GAP, GAP));
        panel.add(labelled("Activity advice profile", profile), BorderLayout.NORTH);
        panel.add(scroll(advice), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reportTab() {
        var panel = panel(new BorderLayout(GAP, GAP));
        var form = card(new BorderLayout(GAP, GAP));
        form.setPreferredSize(new Dimension(0, FORM_HEIGHT));
        var selectors = panel(new GridLayout(1, 2, GAP, GAP));
        selectors.add(labelled("Report district", reportDistrict));
        selectors.add(labelled("Issue type", issue));
        form.add(selectors, BorderLayout.NORTH);
        form.add(labelled("Observation (" + CitizenReport.DESCRIPTION_RANGE + " characters). Avoid personal details.", scroll(description)), BorderLayout.CENTER);
        form.add(reportActions(), BorderLayout.SOUTH);
        panel.add(form, BorderLayout.NORTH);
        panel.add(labelled("Local report history", scroll(reportHistory)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reportActions() {
        var bar = panel(new BorderLayout(GAP, GAP));
        bar.setOpaque(false);
        bar.add(counter, BorderLayout.CENTER);
        styleButton(save, true);
        bar.add(save, BorderLayout.EAST);
        return bar;
    }

    private void configureForm() {
        description.setEditable(true);
        updateCount();
        description.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
        });
    }

    private void updateCount() {
        int length = description.getText().strip().length();
        counter.setText(length + " / " + CitizenReport.MAX_DESCRIPTION + " input units");
        counter.setForeground(length > CitizenReport.MAX_DESCRIPTION ? ERROR_TEXT : MUTED);
    }

    private JPanel alertsTab() {
        var panel = card(new BorderLayout(GAP, GAP));
        panel.add(subscription, BorderLayout.NORTH);
        panel.add(scroll(notifications), BorderLayout.CENTER);
        panel.add(new JLabel("Threshold: U.S. AQI " + AlertMonitor.DEFAULT_THRESHOLD + ". The last district and advice profile are saved."), BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane aboutTab() {
        var notes = area("sourceNotes");
        notes.setText("""
                SOURCE: Open-Meteo Air Quality API / Copernicus CAMS Global
                The readings are modeled estimates at representative coordinates. They are not readings from a park sensor or Malaysia's official Air Pollutant Index.

                HOW TO READ THE TIMESTAMPS
                Model time is the hour the estimate represents. Fetch time is when the app obtained it. Cached values retain both times. The trend only contains distinct hours fetched in this session.

                SDG 11.6 AND 11.7
                The pilot helps residents interpret dated air-quality information and find a verified public green space. It does not measure reduced pollution or guarantee that a park is accessible.

                DATA STORAGE AND KEYBOARD
                Reports and preferences remain on this device. Ctrl+R refreshes the selected area. Alt+1 through Alt+6 selects a tab on supported Swing look-and-feels.
                """);
        return scroll(notes);
    }

    private static JPanel labelled(String text, JComponent component) {
        var panel = panel(new BorderLayout(GAP, GAP));
        var label = label(text, BODY, Font.BOLD, TEXT);
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
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, BODY));
        field.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
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
        refreshAll.setEnabled(!value);
        district.setEnabled(!value);
        progress.setVisible(value);
        if (value) message("Retrieving modeled air quality…", false);
    }

    /** Prevents duplicate submissions while the previous report is being written. */
    @Override public void reportSaving(boolean value) {
        save.setEnabled(!value);
        description.setEditable(!value);
        reportDistrict.setEnabled(!value);
        issue.setEnabled(!value);
        if (value) message("Saving report on this device…", false);
    }

    /** Updates the district result, dashboard, advice and comparison. */
    @Override public void reading(District item, ReadingService.Result result, List<Reading> recent) {
        results.put(item, result);
        trends.put(item, List.copyOf(recent));
        renderSelection();
        showReadingStatus(item, result);
    }

    private void renderSelection() {
        var result = results.get(selectedDistrict());
        setText(dashboard, dashboardText(result));
        overview.render(selectedDistrict(), result, source(result));
        monitoring.render(selectedDistrict(), results, trends.getOrDefault(selectedDistrict(), List.of()), this::tableSource);
        renderAdvice(result);
        subscription.setSelected(subscribed(selectedDistrict()));
    }

    private String dashboardText(ReadingService.Result result) {
        if (result == null) return selectedDistrict() + "\n\n" + NO_DATA;
        if (result.reading() == null) return selectedDistrict() + "\n\nUNAVAILABLE\n" + result.message();
        var data = result.reading();
        return "Model time: " + time(data.validAt()) + "   |   Fetched: " + time(data.fetchedAt())
                + "\nSource: " + source(result) + "\n\n" + selectedProfile().strategy().advice(data.category())
                + "\n\nOpen-Meteo / Copernicus CAMS Global model. This is a regional estimate, not Malaysia's official API.";
    }

    private void showReadingStatus(District district, ReadingService.Result result) {
        boolean failed = result.state() == ReadingService.State.UNAVAILABLE;
        var message = fixture && result.state() == ReadingService.State.LIVE ? "Controlled response received." : result.message();
        message(district + " · " + message, failed);
        if (result.state() == ReadingService.State.CACHED) status.setForeground(AMBER);
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
        status.setForeground(error ? ERROR_TEXT : TEXT);
        status.setText((error ? "Notice: " : "") + message);
        if ("Report saved on this device.".equals(message)) description.setText("");
    }

    /** Displays session crossing history and a banner for a delivered alert. */
    @Override public void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert delivered) {
        var text = new StringBuilder();
        for (var alert : history) text.append(time(alert.validAt())).append(" · ").append(alert.district())
                .append(" · AQI ").append(alert.aqi()).append(" crossed ").append(alert.threshold()).append('\n');
        setText(notifications, text.isEmpty() ? "No alerts in this session." : text.toString());
        if (delivered != null) {
            latestAlert.setForeground(ERROR_TEXT);
            latestAlert.setText("Pollution alert for " + delivered.district() + ": U.S. AQI " + delivered.aqi());
        }
    }

    private String source(ReadingService.Result result) {
        if (result == null) return "Not loaded";
        return switch (result.state()) {
            case CACHED -> "CACHED (last verified response)";
            case LIVE -> fixture ? "TEST FIXTURE" : "LIVE API (modeled estimate)";
            case UNAVAILABLE -> "Unavailable";
        };
    }

    private String tableSource(ReadingService.Result result) {
        return switch (result.state()) {
            case LIVE -> fixture ? "TEST FIXTURE" : "Live model";
            case CACHED -> "Verified cache";
            case UNAVAILABLE -> "Unavailable";
        };
    }

    private static String time(Instant value) { return TIME.format(value); }

    private static void setText(JTextArea area, String text) {
        area.setText(text);
        area.setCaretPosition(0);
    }
}
