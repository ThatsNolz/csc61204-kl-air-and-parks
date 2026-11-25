package cityair.view;

import cityair.controller.MonitorController;
import cityair.model.CitizenReport;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import static cityair.view.Theme.*;

/**
 * Application shell and the Swing implementation of {@link MonitorController.View}. It owns the
 * shared controls and session state and delegates each screen's layout to a dedicated pane.
 * Construct and update on the event dispatch thread.
 */
public final class MonitorPanel extends JPanel implements MonitorController.View {
    private static final int WINDOW_WIDTH = 1240, WINDOW_HEIGHT = 820, RAIL_WIDTH = 196, TAB_KEY_BASE = KeyEvent.VK_1;
    private static final String NO_DATA = "No verified data loaded. Use Refresh district.";
    private static final String SAVED = "Report saved on this device.";
    private final JTabbedPane tabs = named(new JTabbedPane(JTabbedPane.LEFT), "tabs");
    private final JComboBox<District> district = named(new JComboBox<>(District.values()), "district");
    private final JComboBox<AdviceProfile> profile = named(new JComboBox<>(AdviceProfile.values()), "profile");
    private final JButton refresh = named(new JButton("Refresh district"), "refresh");
    private final JButton refreshAll = named(new JButton("Compare all areas"), "refreshAll");
    private final JTextArea dashboard = text("dashboard"), advice = text("advice");
    private final JLabel status = named(label("Ready. Choose a district to start.", BODY, Font.PLAIN, TEXT), "status");
    private final JLabel latestAlert = named(label("No notifications delivered in this session.", BODY, Font.PLAIN, MUTED), "latestAlert");
    private final JProgressBar progress = new JProgressBar();
    private final OverviewPane overview = new OverviewPane(dashboard);
    private final MonitoringPane monitoring = new MonitoringPane();
    private final AdvicePane advicePane = new AdvicePane(profile, advice);
    private final ReportsPane reportsPane = new ReportsPane();
    private final AlertsPane alertsPane = new AlertsPane();
    private final Map<District, ReadingService.Result> results = new EnumMap<>(District.class);
    private final Map<District, List<Reading>> trends = new EnumMap<>(District.class);
    private final ViewPreferences settings;
    private final boolean fixture;
    private MonitorController controller;

    /** Creates a window-sized panel with an injected settings store. */
    public MonitorPanel(Preferences preferences) {
        this(preferences, false);
    }

    /** Creates a view with explicit test-fixture provenance when running controlled demonstrations. */
    public MonitorPanel(Preferences preferences, boolean fixture) {
        super(new BorderLayout());
        this.settings = new ViewPreferences(preferences);
        this.fixture = fixture;
        setBackground(CANVAS);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        add(header(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);
        addScreens();
        shortcuts();
        restoreSettings();
        renderSelection();
    }

    /** Connects the view's actions after the controller is constructed. */
    public void bind(MonitorController controller) {
        this.controller = controller;
        refresh.addActionListener(e -> controller.refresh(selectedDistrict()));
        refreshAll.addActionListener(e -> controller.refreshAll());
        reportsPane.onSave(() -> controller.saveReport(reportsPane.district(), reportsPane.issue(), reportsPane.description()));
        district.addActionListener(e -> changeDistrict());
        overview.onDistrictChosen(district::setSelectedItem);
        profile.addActionListener(e -> changeProfile());
        alertsPane.onSubscription(this::changeSubscription);
        alertsPane.onThreshold(this::changeThreshold);
        controller.changeThreshold(settings.threshold());
        for (District item : District.values()) controller.subscribe(item, settings.subscribed(item));
        controller.loadReports();
    }

    private JPanel header() {
        var header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        header.add(brand(), BorderLayout.WEST);
        header.add(controls(), BorderLayout.CENTER);
        return header;
    }

    private static JPanel brand() {
        var brand = new JPanel(new GridLayout(0, 1));
        brand.setBackground(RAIL);
        brand.setPreferredSize(new Dimension(RAIL_WIDTH, 0));
        brand.setBorder(new EmptyBorder(GAP, PAD, GAP, PAD));
        brand.add(label("KL Air & Parks", HEADING + 2, Font.BOLD, Color.WHITE));
        brand.add(label("SDG 11 pilot", SMALL, Font.PLAIN, ON_DARK));
        return brand;
    }

    private JPanel controls() {
        var bar = panel(new BorderLayout(GAP, 0));
        bar.setBorder(new EmptyBorder(GAP, PAD, GAP, PAD));
        var picker = panel(new BorderLayout(GAP, 0));
        picker.add(caption("Pilot area"), BorderLayout.WEST);
        styleCombo(district);
        district.setPreferredSize(new Dimension(260, 36));
        picker.add(district, BorderLayout.CENTER);
        var buttons = panel(new GridLayout(1, 2, GAP, 0));
        styleButton(refreshAll, false);
        styleButton(refresh, true);
        buttons.add(refreshAll);
        buttons.add(refresh);
        bar.add(picker, BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private JPanel statusBar() {
        var bar = new JPanel(new BorderLayout(GAP, 0));
        bar.setBackground(SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, LINE), new EmptyBorder(8, PAD, 8, PAD)));
        latestAlert.setOpaque(true);
        latestAlert.setBackground(SURFACE);
        latestAlert.setBorder(new EmptyBorder(4, 10, 4, 10));
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.getAccessibleContext().setAccessibleName("Refreshing modeled air quality");
        bar.add(status, BorderLayout.CENTER);
        var right = panel(new BorderLayout(GAP, 0));
        right.add(latestAlert, BorderLayout.CENTER);
        right.add(progress, BorderLayout.EAST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void addScreens() {
        tabs.setUI(new NavigationRailUI());
        tabs.setFont(font(BODY, Font.BOLD));
        tabs.setBackground(CANVAS);
        tabs.addTab("Overview", screen("Air quality overview", "Latest verified estimate for the selected pilot area.", overview));
        tabs.addTab("Monitoring", screen("Monitor and compare", "Nearby areas can share a CAMS grid cell. Compare all areas fills the table.", monitoring));
        tabs.addTab("Advice & parks", screen("Plan an outing", "Advice for each audience and a verified park for the selected area.", advicePane));
        tabs.addTab("Citizen reports", screen("Record an observation", "Reports stay on this device and are not submitted to any authority.", reportsPane));
        tabs.addTab("Alerts & settings", screen("Pollution alerts", "Choose which areas notify you and the U.S. AQI that triggers an alert.", alertsPane));
        tabs.addTab("Data & impact", screen("Understand the data", "Source, limits and the pilot's connection to SDG 11.", new AboutPane()));
        for (int i = 0; i < tabs.getTabCount(); i++) tabs.setMnemonicAt(i, TAB_KEY_BASE + i);
    }

    private void shortcuts() {
        var key = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, "refreshSelected");
        getActionMap().put("refreshSelected", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { refresh.doClick(0); }
        });
    }

    private void restoreSettings() {
        district.setSelectedItem(settings.district());
        profile.setSelectedItem(settings.profile());
        reportsPane.selectDistrict(selectedDistrict());
        alertsPane.setThreshold(settings.threshold());
        for (District item : District.values()) alertsPane.setSubscribed(item, settings.subscribed(item));
    }

    private District selectedDistrict() { return (District) district.getSelectedItem(); }
    private AdviceProfile selectedProfile() { return (AdviceProfile) profile.getSelectedItem(); }

    private void changeDistrict() {
        settings.saveDistrict(selectedDistrict());
        reportsPane.selectDistrict(selectedDistrict());
        renderSelection();
        message("Showing " + selectedDistrict() + ".", false);
    }

    private void changeProfile() {
        settings.saveProfile(selectedProfile());
        renderSelection();
    }

    private void changeSubscription(District item, boolean enabled) {
        settings.saveSubscription(item, enabled);
        controller.subscribe(item, enabled);
        message((enabled ? "Notifications enabled for " : "Notifications disabled for ") + item + ".", false);
    }

    private void changeThreshold(int value) {
        settings.saveThreshold(value);
        controller.changeThreshold(value);
        renderSelection();
        message("Alert threshold set to U.S. AQI " + value + ".", false);
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
        reportsPane.setSaving(value);
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
        var source = Formats.source(result, fixture);
        dashboard.setText(dashboardText(result, source));
        dashboard.setCaretPosition(0);
        overview.render(selectedDistrict(), result, source, selectedProfile());
        overview.renderAll(results, selectedDistrict());
        monitoring.render(selectedDistrict(), results, trends.getOrDefault(selectedDistrict(), List.of()),
                r -> Formats.tableSource(r, fixture), settings.threshold());
        advicePane.render(selectedDistrict(), result, source, selectedProfile());
    }

    private String dashboardText(ReadingService.Result result, String source) {
        if (result == null) return selectedDistrict() + "\n\n" + NO_DATA;
        if (result.reading() == null) return selectedDistrict() + "\n\nUNAVAILABLE\n" + result.message();
        var data = result.reading();
        return "Source: " + source + "\nModel time: " + Formats.time(data.validAt()) + "\nFetched: "
                + Formats.time(data.fetchedAt()) + "\n\nOpen-Meteo / Copernicus CAMS Global regional model, "
                + "not Malaysia's official API.";
    }

    private void showReadingStatus(District item, ReadingService.Result result) {
        boolean failed = result.state() == ReadingService.State.UNAVAILABLE;
        var text = fixture && result.state() == ReadingService.State.LIVE ? "Controlled response received." : result.message();
        message(item + " · " + text, failed);
        if (result.state() == ReadingService.State.CACHED) status.setForeground(AMBER);
    }

    /** Updates the local report history. */
    @Override public void reports(List<CitizenReport> entries) { reportsPane.render(entries); }

    /** Displays a message using text and contrast rather than colour alone. */
    @Override public void message(String text, boolean error) {
        status.setForeground(error ? ERROR_TEXT : TEXT);
        status.setText((error ? "Notice: " : "") + text);
        if (SAVED.equals(text)) reportsPane.clearDescription();
    }

    /** Displays session crossing history and a banner for a delivered alert. */
    @Override public void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert delivered) {
        alertsPane.render(history);
        if (delivered == null) return;
        latestAlert.setBackground(ERROR_FILL);
        latestAlert.setForeground(ERROR_TEXT);
        latestAlert.setFont(font(BODY, Font.BOLD));
        latestAlert.setText("Pollution alert for " + delivered.district() + ": U.S. AQI " + delivered.aqi()
                + " (threshold " + delivered.threshold() + ")");
    }
}
