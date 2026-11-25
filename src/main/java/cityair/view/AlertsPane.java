package cityair.view;

import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Badge;
import cityair.view.component.Card;
import cityair.view.component.DataTable;
import cityair.view.component.EmptyState;
import cityair.view.component.Panels;
import cityair.view.component.Scrolls;
import cityair.view.component.Stepper;
import cityair.view.component.Text;
import cityair.view.component.ToggleSwitch;
import cityair.view.design.Glyph;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

/**
 * The alert centre: which areas notify, at what index, and what has been delivered this session.
 *
 * <p>The threshold and the subscriptions are the two settings that decide whether the Observer
 * publishes anything, so they sit beside the history they produce. The screen states the rule the
 * monitor actually applies, including that an area already above the threshold is not alerted
 * again until it falls below and crosses on a fresh model hour, because an alert system that is
 * not understood is an alert system that gets turned off.</p>
 */
final class AlertsPane extends JPanel implements Themed {
    private static final int SETTINGS_WIDTH = 360;
    private static final int[] WIDTHS = {160, 180, 90, 110};
    private final Map<District, ToggleSwitch> switches = new EnumMap<>(District.class);
    private final Stepper threshold = Text.described(new Stepper(AlertMonitor.DEFAULT_THRESHOLD,
            AlertMonitor.MINIMUM_THRESHOLD, AlertMonitor.MAXIMUM_THRESHOLD), "threshold",
            "Alert threshold in U.S. AQI");
    private final DefaultTableModel model = DataTable.model("Model time (MYT)", "Area", "U.S. AQI", "Threshold");
    private final DataTable table = new DataTable(model, 2, 3);
    private final Badge state = new Badge("No alerts this session", Tone.INK_MUTED);
    private final AuroraLabel thresholdCategory = new AuroraLabel("", Type.strong(Type.CAPTION), Tone.INK_MUTED);
    private final CardLayout historyLayout = new CardLayout();
    private final JPanel historyBody = new JPanel(historyLayout);

    AlertsPane() {
        super(new BorderLayout(Metrics.SPACE_5, 0));
        setOpaque(false);
        add(settings(), BorderLayout.WEST);
        add(history(), BorderLayout.CENTER);
    }

    private JComponent settings() {
        var column = Panels.column(Metrics.SPACE_5);
        column.add(Panels.stack(Metrics.SPACE_5, subscriptions(), thresholdCard(), rule()), BorderLayout.NORTH);
        return Panels.fixedWidth(column, SETTINGS_WIDTH);
    }

    private JComponent subscriptions() {
        var toggles = Panels.rows(Metrics.SPACE_3);
        for (District area : District.values()) {
            var toggle = Text.named(new ToggleSwitch(area.toString()), "subscription." + area.name());
            switches.put(area, toggle);
            toggles.add(toggle);
        }
        return Card.titled("Notify me for", toggles);
    }

    private JComponent thresholdCard() {
        var body = Panels.column(Metrics.SPACE_3);
        var row = Panels.row(Metrics.SPACE_3);
        row.add(Panels.leading(threshold), BorderLayout.WEST);
        row.add(Panels.centred(thresholdCategory), BorderLayout.CENTER);
        body.add(row, BorderLayout.NORTH);
        body.add(Text.note("An alert fires when a fresh model hour reaches this U.S. AQI after being below it. "
                + "101 is the start of Unhealthy for sensitive groups. Saved on this device."),
                BorderLayout.CENTER);
        var card = Card.titled("Alert threshold (U.S. AQI)", body);
        card.accent(() -> Skin.color(Tone.DANGER));
        return card;
    }

    private JComponent rule() {
        var body = Panels.column(Metrics.SPACE_2);
        body.add(Text.note("One crossing produces one alert. An area that stays above the threshold is not "
                + "alerted again until it falls below it and crosses upward on a later model hour."),
                BorderLayout.CENTER);
        return Card.titled("How the monitor decides", body);
    }

    private JComponent history() {
        Text.described(table, "alertTable", "Alerts delivered in this session, newest first");
        table.widths(WIDTHS);
        historyBody.setOpaque(false);
        historyBody.add(Scrolls.of(table), "table");
        historyBody.add(EmptyState.of(Glyph.BELL, "No alerts delivered yet",
                "When a subscribed area crosses your threshold on a fresh model hour, the crossing is "
                        + "listed here and announced in the status bar."), "empty");
        return Card.titled("Alerts delivered in this session", state, historyBody);
    }

    /** Reports each switch change as (area, enabled). */
    void onSubscription(BiConsumer<District, Boolean> action) {
        switches.forEach((area, toggle) -> toggle.addActionListener(event -> action.accept(area, toggle.isSelected())));
    }

    /** Reports each threshold change. */
    void onThreshold(IntConsumer action) {
        threshold.addChangeListener(event -> action.accept((Integer) threshold.getValue()));
    }

    /** Applies a saved subscription without animating the switch. */
    void setSubscribed(District area, boolean enabled) { switches.get(area).setSelectedQuietly(enabled); }

    /** Applies a saved threshold and names the category it falls in. */
    void setThreshold(int value) {
        threshold.setValue(value);
        thresholdCategory.setText(cityair.model.AirCategory.from(value).toString());
    }

    /** Replaces the history rows with the monitor's snapshot. */
    void render(List<AlertMonitor.Alert> history) {
        model.setRowCount(0);
        for (var alert : history) {
            model.addRow(new Object[]{Formats.shortTime(alert.validAt()), alert.district(), alert.aqi(),
                    alert.threshold()});
        }
        boolean empty = history.isEmpty();
        historyLayout.show(historyBody, empty ? "empty" : "table");
        state.set(empty ? "No alerts this session" : history.size() + " delivered",
                () -> Skin.color(empty ? Tone.INK_MUTED : Tone.DANGER));
    }

    /** Re-reads the colours of the screen and its status badge. */
    @Override public void applyTheme() {
        state.applyTheme();
        repaint();
    }
}
