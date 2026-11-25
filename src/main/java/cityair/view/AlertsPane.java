package cityair.view;

import cityair.model.District;
import cityair.observer.AlertMonitor;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import static cityair.view.Theme.*;

/** Alerts screen: one notification switch per district, the alert threshold and the session history. */
final class AlertsPane extends JPanel {
    private static final int SETTINGS_WIDTH = 330;
    private static final int[] WIDTHS = {170, 170, 90, 110};
    private final Map<District, JCheckBox> switches = new EnumMap<>(District.class);
    private final JSpinner threshold = named(new JSpinner(new SpinnerNumberModel(
            AlertMonitor.DEFAULT_THRESHOLD, AlertMonitor.MINIMUM_THRESHOLD, AlertMonitor.MAXIMUM_THRESHOLD, 1)), "threshold");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Model time (MYT)", "Area", "U.S. AQI", "Threshold"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = named(new JTable(model), "alertTable");

    AlertsPane() {
        super(new BorderLayout(GAP, 0));
        setOpaque(false);
        add(settings(), BorderLayout.WEST);
        styleTable(table);
        for (int i = 0; i < WIDTHS.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS[i]);
        add(titled("Alerts delivered in this session · newest first", scroll(table)), BorderLayout.CENTER);
    }

    private JPanel settings() {
        var column = panel(new BorderLayout(0, GAP));
        column.setPreferredSize(new Dimension(SETTINGS_WIDTH, 0));
        var toggles = panel(new GridLayout(0, 1, 0, 6));
        for (District item : District.values()) {
            var toggle = named(new JCheckBox(item.toString()), "subscription." + item.name());
            toggle.setFont(font(BODY, Font.PLAIN));
            toggle.setOpaque(false);
            switches.put(item, toggle);
            toggles.add(toggle);
        }
        var stack = panel(new BorderLayout(0, GAP));
        stack.add(titled("Notify me for", toggles), BorderLayout.NORTH);
        stack.add(thresholdCard(), BorderLayout.CENTER);
        column.add(stack, BorderLayout.NORTH);
        return column;
    }

    private JPanel thresholdCard() {
        threshold.setFont(font(HEADING, Font.BOLD));
        threshold.setPreferredSize(new Dimension(110, 34));
        var spinner = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinner.add(threshold);
        var body = panel(new BorderLayout(0, GAP));
        body.add(spinner, BorderLayout.NORTH);
        body.add(label("<html><body style='width:230px'>An alert fires when a fresh model hour reaches this U.S. AQI "
                + "after being below it. 101 is the start of <i>Unhealthy for sensitive groups</i>. "
                + "The setting is saved on this device.</body></html>", SMALL, Font.PLAIN, MUTED), BorderLayout.CENTER);
        var card = titled("Alert threshold (U.S. AQI)", body);
        card.setBorder(accentBorder(ERROR_TEXT));
        return card;
    }

    /** Reports each switch change as (district, enabled). */
    void onSubscription(BiConsumer<District, Boolean> action) {
        switches.forEach((item, toggle) -> toggle.addActionListener(e -> action.accept(item, toggle.isSelected())));
    }

    /** Reports each threshold change. */
    void onThreshold(IntConsumer action) {
        threshold.addChangeListener(e -> action.accept((Integer) threshold.getValue()));
    }

    void setSubscribed(District item, boolean enabled) { switches.get(item).setSelected(enabled); }

    void setThreshold(int value) { threshold.setValue(value); }

    /** Replaces the history rows with the monitor's snapshot. */
    void render(List<AlertMonitor.Alert> history) {
        model.setRowCount(0);
        for (var alert : history) {
            model.addRow(new Object[]{Formats.shortTime(alert.validAt()), alert.district(), alert.aqi(), alert.threshold()});
        }
    }
}
