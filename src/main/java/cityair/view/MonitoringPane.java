package cityair.view;

import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import static cityair.view.DeskStyle.*;

/** Presents comparable snapshots and the exact model times behind a session chart. */
final class MonitoringPane extends JPanel {
    private static final String DASH = "—";
    private static final int DISTRICT_HEIGHT = 172;
    private static final int[] DISTRICT_WIDTHS = {200, 85, 250, 130, 165};
    private static final int[] SAMPLE_WIDTHS = {175, 75, 125, 120};
    private final DefaultTableModel districts = tableModel("Pilot area", "U.S. AQI", "Category", "Source", "Model time (MYT)");
    private final DefaultTableModel samples = tableModel("Model time (MYT)", "U.S. AQI", "PM2.5 (µg/m³)", "PM10 (µg/m³)");
    private final TrendChart chart = new TrendChart();

    MonitoringPane() {
        super(new BorderLayout(GAP, GAP));
        setBackground(CANVAS);
        var districtBlock = block("District comparison", scroll(tableFor(districts, "districtTable", DISTRICT_WIDTHS)));
        districtBlock.setPreferredSize(new Dimension(0, DISTRICT_HEIGHT));
        add(districtBlock, BorderLayout.NORTH);
        var trend = panel(new GridLayout(1, 2, GAP, GAP));
        trend.add(chart);
        trend.add(scroll(tableFor(samples, "sampleTable", SAMPLE_WIDTHS)));
        add(block("AQI trend · distinct model hours from this session", trend), BorderLayout.CENTER);
    }

    void render(District selected, Map<District, Result> states, List<Reading> history, Function<Result, String> origin) {
        districts.setRowCount(0);
        for (District d : District.values()) districts.addRow(districtRow(d, states.get(d), origin));
        samples.setRowCount(0);
        history.stream().sorted(Comparator.comparing(Reading::validAt)).forEach(x -> samples.addRow(sampleRow(x)));
        chart.setHistory(selected, history);
    }

    private static Object[] districtRow(District d, Result result, Function<Result, String> origin) {
        if (result == null) return new Object[]{d, DASH, DASH, "Not loaded", DASH};
        var reading = result.reading();
        if (reading == null) return new Object[]{d, DASH, DASH, "Unavailable", DASH};
        return new Object[]{d, reading.aqi(), reading.category(), origin.apply(result), TrendChart.time(reading.validAt())};
    }

    private static Object[] sampleRow(Reading r) {
        return new Object[]{TrendChart.time(r.validAt()), r.aqi(), value(r.pm25()), value(r.pm10())};
    }

    private static Object value(Double v) { return v == null ? DASH : v; }

    private static DefaultTableModel tableModel(String... headers) {
        return new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable tableFor(DefaultTableModel model, String name, int[] widths) {
        var view = new JTable(model);
        view.setName(name);
        view.getAccessibleContext().setAccessibleName(name.equals("districtTable") ? "District comparison" : "Session sample values");
        table(view);
        for (int i = 0; i < widths.length; i++) view.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        return view;
    }

    private static JPanel block(String title, JComponent component) {
        var block = card(new BorderLayout(GAP, GAP));
        block.add(label(title, BODY, Font.BOLD, TEXT), BorderLayout.NORTH);
        block.add(component, BorderLayout.CENTER);
        return block;
    }
}
