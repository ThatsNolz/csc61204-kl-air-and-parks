package cityair.view;

import cityair.controller.MonitorController;
import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.CitizenReport;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import cityair.view.component.Atmosphere;
import cityair.view.component.Panels;
import cityair.view.component.ScrollableColumn;
import cityair.view.component.Scrolls;
import cityair.view.component.Text;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * The application shell and the Swing implementation of {@link MonitorController.View}.
 *
 * <p>It owns the controls that are shared between screens, the session state, and nothing else.
 * Each screen is a separate pane that receives one immutable {@link ViewState} and renders it, so
 * the shell contains no air-quality logic and a screen contains no state. Everything here runs on
 * the event dispatch thread; the controller is responsible for keeping work off it.</p>
 */
public final class MonitorPanel extends Atmosphere implements MonitorController.View {
    private static final int TAB_KEY_BASE = KeyEvent.VK_1;
    private static final String NO_DATA = "No verified data loaded. Use Refresh district.";
    private static final String SAVED = "Report saved on this device.";
    private final JTabbedPane tabs = Text.named(new JTabbedPane(JTabbedPane.LEFT), "tabs");
    private final NavigationRailUI rail = new NavigationRailUI();
    private final TopBar topBar = new TopBar();
    private final StatusBar statusBar = new StatusBar();
    private final JTextArea provenance = provenanceArea();
    private final JTextArea adviceDetail = Text.named(detailArea(), "advice");
    private final cityair.view.component.Dropdown<AdviceProfile> profile =
            Text.named(new cityair.view.component.Dropdown<>(AdviceProfile.values()), "profile");
    private final OverviewPane overview = new OverviewPane(provenance);
    private final MonitoringPane monitoring = new MonitoringPane();
    private final PlannerPane planner;
    private final ReportsPane reportsPane = new ReportsPane();
    private final AlertsPane alertsPane = new AlertsPane();
    private final SettingsPane settingsPane = new SettingsPane();
    private final Map<District, ReadingService.Result> results = new EnumMap<>(District.class);
    private final Map<District, List<Reading>> trends = new EnumMap<>(District.class);
    private final ViewPreferences settings;
    private final transient Clock clock;
    private final boolean fixture;
    private transient Instant lastSuccess;
    private transient MonitorController controller;

    /** Creates a window-sized panel with an injected settings store. */
    public MonitorPanel(Preferences preferences) {
        this(preferences, false, Clock.systemUTC());
    }

    /** Creates a view with explicit test-fixture provenance, for controlled demonstrations. */
    public MonitorPanel(Preferences preferences, boolean fixture) {
        this(preferences, fixture, Clock.systemUTC());
    }

    /** Creates a view with an injected clock, so elapsed-time text can be tested. */
    public MonitorPanel(Preferences preferences, boolean fixture, Clock clock) {
        super(new BorderLayout());
        this.settings = new ViewPreferences(preferences);
        this.fixture = fixture;
        this.clock = clock;
        this.planner = new PlannerPane(profile, adviceDetail);
        Skin.setTheme(settings.theme());
        Skin.setReducedMotion(settings.reducedMotion());
        assemble();
        restoreSettings();
        renderSelection();
        // Fields are constructed before the saved theme is known, so every component that stores
        // a colour re-reads it once the shell is assembled.
        Skin.refresh(this);
        javax.swing.SwingUtilities.invokeLater(this::scrollToTop);
    }

    private void assemble() {
        setPreferredSize(new Dimension(Metrics.WINDOW_WIDTH, Metrics.WINDOW_HEIGHT));
        setMinimumSize(new Dimension(Metrics.MIN_WINDOW_WIDTH, Metrics.MIN_WINDOW_HEIGHT));
        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        addScreens();
        shortcuts();
        responsiveRail();
    }

    /** Connects the view's actions once the controller exists. */
    public void bind(MonitorController controller) {
        this.controller = controller;
        topBar.refreshButton().addActionListener(event -> controller.refresh(selectedDistrict()));
        topBar.compareButton().addActionListener(event -> controller.refreshAll());
        topBar.districtSelector().addActionListener(event -> changeDistrict());
        topBar.themeButton().addActionListener(event -> toggleTheme());
        topBar.railButton().addActionListener(event -> setRailCollapsed(!rail.isCollapsed()));
        reportsPane.onSave(() -> controller.saveReport(reportsPane.district(), reportsPane.issue(),
                reportsPane.description()));
        overview.onDistrictChosen(this::selectDistrict);
        monitoring.onDistrictChosen(this::selectDistrict);
        profile.addActionListener(event -> changeProfile());
        bindSettings(controller);
        controller.changeThreshold(settings.threshold());
        for (District area : District.values()) controller.subscribe(area, settings.subscribed(area));
        controller.loadReports();
    }

    private void bindSettings(MonitorController controller) {
        alertsPane.onSubscription(this::changeSubscription);
        alertsPane.onThreshold(this::changeThreshold);
        planner.onIntensity(value -> {
            settings.saveIntensity(value);
            renderSelection();
        });
        planner.onWindow(value -> {
            settings.saveWindow(value);
            renderSelection();
        });
        settingsPane.onTheme(this::applyTheme);
        settingsPane.onReducedMotion(this::applyReducedMotion);
        settingsPane.onRailCollapsed(this::setRailCollapsed);
    }

    /** Requests the first reading, so a launched application is not an empty dashboard. */
    public void loadOnStart() {
        if (controller != null) controller.refresh(selectedDistrict());
    }

    private void addScreens() {
        tabs.setUI(rail);
        tabs.setOpaque(false);
        tabs.setFocusable(true);
        addMonitoringScreens();
        addResponseScreens();
        addReferenceScreens();
        for (int i = 0; i < tabs.getTabCount(); i++) tabs.setMnemonicAt(i, TAB_KEY_BASE + i);
        // Showing a screen starts it at its heading: without this the focus Swing hands to the
        // first control inside the screen scrolls the top of the screen out of view.
        tabs.addChangeListener(event -> javax.swing.SwingUtilities.invokeLater(this::scrollToTop));
    }

    private void addMonitoringScreens() {
        addScreen("Overview", "Air quality overview",
                "The latest verified estimate for the selected pilot area.", overview);
        addScreen("Live monitoring", "Monitor and compare",
                "Pilot areas side by side and the model hours collected in this session.", monitoring);
    }

    private void addResponseScreens() {
        addScreen("Activity planner", "Plan time outdoors",
                "Rule-based assessment of a planned activity, and all three advice strategies.", planner);
        addScreen("Citizen reports", "Record an observation",
                "Reports stay on this device and are not submitted to any authority.", reportsPane);
        addScreen("Alert centre", "Pollution alerts",
                "Choose which areas notify you and the index that triggers a crossing.", alertsPane);
    }

    private void addReferenceScreens() {
        addScreen("Data & impact", "Understand the data",
                "Source, limits, the U.S. AQI scale and the pilot's connection to SDG 11.", new AboutPane());
        addScreen("Settings", "Appearance and data",
                "Theme, motion and where this application stores information.", settingsPane);
    }

    private void addScreen(String tab, String title, String subtitle, JComponent content) {
        var screen = new ScrollableColumn(new BorderLayout(0, Metrics.SPACE_5));
        screen.setBorder(new EmptyBorder(Metrics.SPACE_6, Metrics.SPACE_7, Metrics.SPACE_6, Metrics.SPACE_7));
        var heading = Panels.column(2);
        heading.add(Text.title(title), BorderLayout.NORTH);
        heading.add(Text.muted(subtitle), BorderLayout.SOUTH);
        screen.add(heading, BorderLayout.NORTH);
        screen.add(content, BorderLayout.CENTER);
        var scroll = Scrolls.vertical(screen);
        scroll.setName(tab);
        tabs.addTab(tab, scroll);
        // The rail can collapse to icons, so the name and purpose live in the tooltip as well.
        tabs.setToolTipTextAt(tabs.getTabCount() - 1, tab + " · " + subtitle);
    }

    private void scrollToTop() {
        if (tabs.getSelectedComponent() instanceof javax.swing.JScrollPane scroll) {
            scroll.getVerticalScrollBar().setValue(0);
        }
    }

    private void shortcuts() {
        bind(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, "refreshSelected",
                () -> topBar.refreshButton().doClick(0));
        bind(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "refreshAll",
                () -> topBar.compareButton().doClick(0));
        bind(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, "toggleTheme", this::toggleTheme);
    }

    private void bind(int key, int modifiers, String name, Runnable action) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, modifiers), name);
        getActionMap().put(name, new AbstractAction() {
            /** Runs the action this control is bound to. */
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    private void responsiveRail() {
        addComponentListener(new ComponentAdapter() {
            /** Collapses or expands the navigation rail to suit the new width. */
            @Override public void componentResized(ComponentEvent event) {
                if (settings.railCollapsed()) return;
                rail.setCollapsed(getWidth() < Metrics.RAIL_COLLAPSE_WIDTH);
                topBar.showRail(rail.isCollapsed());
            }
        });
    }

    private void restoreSettings() {
        topBar.districtSelector().setSelectedItem(settings.district());
        profile.setSelectedItem(settings.profile());
        reportsPane.selectDistrict(selectedDistrict());
        alertsPane.setThreshold(settings.threshold());
        for (District area : District.values()) alertsPane.setSubscribed(area, settings.subscribed(area));
        planner.restore(settings.intensity(), settings.window());
        settingsPane.restore(settings.theme(), settings.reducedMotion(), settings.railCollapsed(), dataFolder());
        rail.setCollapsed(settings.railCollapsed());
        topBar.showTheme(Skin.theme());
        topBar.showRail(rail.isCollapsed());
        alertsPane.render(List.of());
    }

    private static Path dataFolder() {
        return Path.of(System.getProperty("user.home"), ".kl-air-and-parks");
    }

    private District selectedDistrict() { return (District) topBar.districtSelector().getSelectedItem(); }

    private AdviceProfile selectedProfile() { return (AdviceProfile) profile.getSelectedItem(); }

    private void selectDistrict(District area) { topBar.districtSelector().setSelectedItem(area); }

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

    private void changeSubscription(District area, boolean enabled) {
        settings.saveSubscription(area, enabled);
        controller.subscribe(area, enabled);
        message((enabled ? "Notifications enabled for " : "Notifications disabled for ") + area + ".", false);
    }

    private void changeThreshold(int value) {
        settings.saveThreshold(value);
        alertsPane.setThreshold(value);
        controller.changeThreshold(value);
        renderSelection();
        message("Alert threshold set to U.S. AQI " + value + ".", false);
    }

    private void toggleTheme() {
        applyTheme(Skin.theme() == AuroraTheme.DARK ? AuroraTheme.LIGHT : AuroraTheme.DARK);
    }

    private void applyTheme(AuroraTheme value) {
        Skin.setTheme(value);
        settings.saveTheme(value);
        settingsPane.restore(value, settings.reducedMotion(), settings.railCollapsed(), dataFolder());
        topBar.showTheme(value);
        Skin.refresh(this);
        message(value + " applied.", false);
    }

    private void applyReducedMotion(boolean value) {
        Skin.setReducedMotion(value);
        settings.saveReducedMotion(value);
        message(value ? "Motion reduced." : "Motion restored.", false);
    }

    private void setRailCollapsed(boolean collapsed) {
        settings.saveRailCollapsed(collapsed);
        rail.setCollapsed(collapsed);
        topBar.showRail(collapsed);
    }

    /** Disables the retrieval actions while a request is in flight. */
    @Override public void loading(boolean value) {
        topBar.refreshButton().setLoading(value);
        topBar.compareButton().setEnabled(!value);
        topBar.districtSelector().setEnabled(!value);
        statusBar.setBusy(value);
        if (value) message("Retrieving modeled air quality…", false);
    }

    /** Prevents duplicate submissions while the previous report is being written. */
    @Override public void reportSaving(boolean value) {
        reportsPane.setSaving(value);
        if (value) message("Saving report on this device…", false);
    }

    /** Records a refresh outcome and redraws every screen from it. */
    @Override public void reading(District area, ReadingService.Result result, List<Reading> recent) {
        results.put(area, result);
        trends.put(area, List.copyOf(recent));
        if (result.state() == ReadingService.State.LIVE) lastSuccess = clock.instant();
        renderSelection();
        showReadingStatus(area, result);
    }

    private void renderSelection() {
        var state = state();
        provenance.setText(provenanceText(state));
        overview.render(state);
        monitoring.render(state);
        planner.render(state);
        setCondition(state.hasReading() ? CategoryPalette.fill(state.reading().category()) : null);
        topBar.showState(Formats.state(state.result(), fixture), stateTone(state.result()), lastSuccess, state.now());
    }

    private ViewState state() {
        var district = selectedDistrict();
        var result = results.get(district);
        return new ViewState(district, result, Formats.source(result, fixture), selectedProfile(),
                settings.intensity(), settings.window(), settings.threshold(),
                trends.getOrDefault(district, List.of()), results, clock.instant(), fixture);
    }

    private static Tone stateTone(ReadingService.Result result) {
        if (result == null) return Tone.INK_MUTED;
        return switch (result.state()) {
            case LIVE -> Tone.POSITIVE;
            case CACHED -> Tone.CAUTION;
            case UNAVAILABLE -> Tone.DANGER;
        };
    }

    private String provenanceText(ViewState state) {
        if (state.result() == null) return state.district() + "\n\n" + NO_DATA;
        if (state.reading() == null) {
            return state.district() + "\n\nUNAVAILABLE\n" + state.result().message();
        }
        Reading data = state.reading();
        return "Source: " + state.source() + "\nModel time: " + Formats.time(data.validAt())
                + "\nFetched: " + Formats.time(data.fetchedAt())
                + "\n\nOpen-Meteo / Copernicus CAMS Global regional model, not Malaysia's official API.";
    }

    private void showReadingStatus(District area, ReadingService.Result result) {
        boolean failed = result.state() == ReadingService.State.UNAVAILABLE;
        String text = fixture && result.state() == ReadingService.State.LIVE
                ? "Controlled response received." : result.message();
        message(area + " · " + text, failed);
        if (result.state() == ReadingService.State.CACHED) {
            statusBar.statusLabel().setForeground(Skin.color(Tone.CAUTION));
        }
    }

    /** Updates the local report history. */
    @Override public void reports(List<CitizenReport> entries) { reportsPane.render(entries); }

    /** Displays a message using words, an icon and colour together. */
    @Override public void message(String text, boolean error) {
        statusBar.message(text, error);
        reportsPane.setInvalid(error && text.contains(CitizenReport.DESCRIPTION_RANGE));
        if (SAVED.equals(text)) reportsPane.clearDescription();
    }

    /** Displays the session crossing history and announces a delivered alert. */
    @Override public void alert(List<AlertMonitor.Alert> history, AlertMonitor.Alert delivered) {
        alertsPane.render(history);
        if (delivered != null) statusBar.showAlert(delivered);
    }

    private static JTextArea provenanceArea() {
        return Text.named(detailArea(), "dashboard");
    }

    private static JTextArea detailArea() {
        var field = new JTextArea();
        field.setEditable(false);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setOpaque(false);
        field.setFont(Type.text(Type.LABEL));
        field.setForeground(Skin.color(Tone.INK_MUTED));
        field.setBorder(new EmptyBorder(0, 0, 0, 0));
        field.setFocusable(false);
        ((javax.swing.text.DefaultCaret) field.getCaret())
                .setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        return field;
    }

    /** The planner's chosen intensity, exposed for the integration tests. */
    ActivityIntensity intensity() { return settings.intensity(); }

    /** The planner's chosen activity length, exposed for the integration tests. */
    ActivityWindow window() { return settings.window(); }
}
