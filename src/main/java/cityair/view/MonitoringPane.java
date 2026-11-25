package cityair.view;

import cityair.model.AirCategory;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import static cityair.view.Theme.*;

/** Monitoring screen: district comparison and the exact model times behind the session chart. */
final class MonitoringPane extends JPanel {
    private static final int DISTRICT_HEIGHT = 190, CATEGORY_COLUMN = 2;
    private static final int[] DISTRICT_WIDTHS = {200, 85, 250, 130, 165};
    private static final int[] SAMPLE_WIDTHS = {150, 75, 115, 115};
    private final DefaultTableModel districts = tableModel("Pilot area", "U.S. AQI", "Category", "Source", "Model time (MYT)");
    private final DefaultTableModel samples = tableModel("Model time (MYT)", "U.S. AQI", "PM2.5 (µg/m³)", "PM10 (µg/m³)");
    private final TrendChart chart = new TrendChart();

    MonitoringPane() {
        super(new BorderLayout(0, GAP));
        setOpaque(false);
        var table = tableFor(districts, "districtTable", DISTRICT_WIDTHS);
        table.getColumnModel().getColumn(CATEGORY_COLUMN).setCellRenderer(new CategoryRenderer());
        var comparison = titled("District comparison", scroll(table));
        comparison.setPreferredSize(new Dimension(0, DISTRICT_HEIGHT));
        add(comparison, BorderLayout.NORTH);
        var trend = panel(new GridLayout(1, 2, GAP, 0));
        trend.add(chart);
        trend.add(scroll(tableFor(samples, "sampleTable", SAMPLE_WIDTHS)));
        add(titled("AQI trend for the selected area · distinct model hours from this session", trend), BorderLayout.CENTER);
    }

    /** Refreshes both tables and the chart for the selected district. */
    void render(District selected, Map<District, Result> states, List<Reading> history,
                Function<Result, String> origin, int threshold) {
        districts.setRowCount(0);
        for (District d : District.values()) districts.addRow(districtRow(d, states.get(d), origin));
        samples.setRowCount(0);
        history.stream().sorted(Comparator.comparing(Reading::validAt)).forEach(x -> samples.addRow(sampleRow(x)));
        chart.setHistory(selected, history, threshold);
    }

    private static Object[] districtRow(District d, Result result, Function<Result, String> origin) {
        if (result == null) return new Object[]{d, Formats.DASH, Formats.DASH, "Not loaded", Formats.DASH};
        var reading = result.reading();
        if (reading == null) return new Object[]{d, Formats.DASH, Formats.DASH, Formats.MISSING, Formats.DASH};
        return new Object[]{d, reading.aqi(), reading.category(), origin.apply(result), Formats.shortTime(reading.validAt())};
    }

    private static Object[] sampleRow(Reading r) {
        return new Object[]{Formats.shortTime(r.validAt()), r.aqi(), value(r.pm25()), value(r.pm10())};
    }

    private static Object value(Double v) { return v == null ? Formats.DASH : v; }

    private static DefaultTableModel tableModel(String... headers) {
        return new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable tableFor(DefaultTableModel model, String name, int[] widths) {
        var view = new JTable(model);
        view.setName(name);
        view.getAccessibleContext().setAccessibleName(name.equals("districtTable") ? "District comparison" : "Session sample values");
        styleTable(view);
        for (int i = 0; i < widths.length; i++) view.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        return view;
    }

    /** Paints a category cell in its EPA colour; the text label keeps the meaning without colour. */
    private static final class CategoryRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                 boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean known = value instanceof AirCategory;
            setBackground(known ? CategoryPalette.fill((AirCategory) value) : table.getBackground());
            setForeground(known ? CategoryPalette.onFill((AirCategory) value) : TEXT);
            setFont(font(BODY, known ? Font.BOLD : Font.PLAIN));
            return this;
        }
    }
}
