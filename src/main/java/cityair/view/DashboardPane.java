package cityair.view;

import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import java.awt.*;
import java.util.Locale;
import javax.swing.*;
import static cityair.view.DeskStyle.*;

/** Presents the current district result with an AQI summary and four measured quantities. */
final class DashboardPane extends JPanel {
    private static final String MISSING = "Unavailable";
    private static final int OVERVIEW_HEIGHT = 168;
    private final JLabel area = label("", BODY, Font.BOLD, MUTED);
    private final JLabel index = label("—", HERO, Font.BOLD, TEAL);
    private final JLabel category = label("No sample yet", BODY, Font.BOLD, TEXT);
    private final JLabel provenance = label("NOT LOADED", BODY, Font.BOLD, MUTED);
    private final JLabel pm25 = valueLabel(), pm10 = valueLabel(), no2 = valueLabel(), o3 = valueLabel();

    DashboardPane(JTextArea details) {
        super(new BorderLayout(GAP, GAP));
        setBackground(CANVAS);
        var overview = panel(new GridLayout(1, 2, GAP, GAP));
        overview.setPreferredSize(new Dimension(0, OVERVIEW_HEIGHT));
        overview.add(indexCard());
        overview.add(pollutantGrid());
        add(overview, BorderLayout.NORTH);
        add(scroll(details), BorderLayout.CENTER);
    }

    private JPanel indexCard() {
        var card = card(new BorderLayout());
        index.setName("aqiValue");
        provenance.setName("readingSource");
        var status = new JPanel(new GridLayout(0, 1));
        status.setOpaque(false);
        status.add(category);
        status.add(provenance);
        card.add(area, BorderLayout.NORTH);
        card.add(index, BorderLayout.CENTER);
        card.add(status, BorderLayout.SOUTH);
        return card;
    }

    private JPanel pollutantGrid() {
        var grid = panel(new GridLayout(2, 2, GAP, GAP));
        grid.add(metric("PM2.5", pm25));
        grid.add(metric("PM10", pm10));
        grid.add(metric("Nitrogen dioxide", no2));
        grid.add(metric("Ozone", o3));
        return grid;
    }

    void render(District district, Result result, String source) {
        area.setText(district + "  /  U.S. AQI");
        Reading r = result == null ? null : result.reading();
        provenance.setText(source);
        index.setText(r == null ? "—" : Integer.toString(r.aqi()));
        category.setText(r == null ? "No verified sample" : r.category().toString());
        pm25.setText(quantity(r == null ? null : r.pm25()));
        pm10.setText(quantity(r == null ? null : r.pm10()));
        no2.setText(quantity(r == null ? null : r.nitrogenDioxide()));
        o3.setText(quantity(r == null ? null : r.ozone()));
    }

    private static JPanel metric(String title, JLabel value) {
        var metric = card(new GridLayout(0, 1));
        value.setName(title.equals("PM10") ? "pm10Value" : title);
        metric.add(label(title, BODY, Font.BOLD, TEXT));
        metric.add(value);
        value.getAccessibleContext().setAccessibleName(title + " concentration");
        return metric;
    }

    private static JLabel valueLabel() { return label(MISSING, BODY, Font.PLAIN, MUTED); }

    private static String quantity(Double value) {
        return value == null ? MISSING : String.format(Locale.ENGLISH, "%.1f µg/m³", value);
    }
}
