package cityair.view;

import cityair.model.CityCatalog;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import cityair.strategy.AdviceProfile;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static cityair.view.Theme.*;

/** Overview screen: headline index on the AQI scale, pollutant cards, advice and a nearby park. */
final class OverviewPane extends JPanel {
    private static final int HERO_WIDTH = 420, HEADLINE_HEIGHT = 250, TEXT_WIDTH = 230;
    private final JLabel area = caption("");
    private final JLabel index = named(label("—", HERO, Font.BOLD, TEXT), "aqiValue");
    private final JLabel category = named(label("No verified sample", BODY, Font.BOLD, TEXT), "aqiCategory");
    private final JLabel provenance = named(label("NOT LOADED", SMALL, Font.BOLD, MUTED), "readingSource");
    private final AqiScale scale = new AqiScale();
    private final JLabel pm25 = metricValue("PM2.5"), pm10 = metricValue("pm10Value");
    private final JLabel no2 = metricValue("Nitrogen dioxide"), o3 = metricValue("Ozone");
    private final JLabel adviceTitle = caption("Advice");
    private final JLabel adviceText = wrapped("adviceSummary");
    private final JLabel park = wrapped("parkSummary");
    private final JPanel hero = card(new BorderLayout(0, GAP));
    private final DistrictStrip strip = new DistrictStrip();

    /** Builds the screen around the shared provenance text area. */
    OverviewPane(JTextArea details) {
        super(new BorderLayout());
        setOpaque(false);
        var stack = panel(new BorderLayout(0, GAP));
        stack.add(headline(), BorderLayout.NORTH);
        stack.add(guidance(details), BorderLayout.CENTER);
        stack.add(strip, BorderLayout.SOUTH);
        add(stack, BorderLayout.NORTH);
    }

    /** Selects a district when its card in the "All pilot areas" strip is clicked. */
    void onDistrictChosen(java.util.function.Consumer<District> action) { strip.onChoose(action); }

    /** Updates the strip with every district's latest result. */
    void renderAll(java.util.Map<District, Result> results, District selected) { strip.render(results, selected); }

    private JPanel headline() {
        var row = panel(new BorderLayout(GAP, 0));
        row.setPreferredSize(new Dimension(0, HEADLINE_HEIGHT));
        hero.setPreferredSize(new Dimension(HERO_WIDTH, HEADLINE_HEIGHT));
        var number = panel(new FlowLayout(FlowLayout.LEFT, PAD, 0));
        number.add(index);
        number.add(chip());
        var status = panel(new BorderLayout(0, 6));
        status.add(scale, BorderLayout.CENTER);
        status.add(provenance, BorderLayout.SOUTH);
        hero.add(area, BorderLayout.NORTH);
        hero.add(number, BorderLayout.CENTER);
        hero.add(status, BorderLayout.SOUTH);
        row.add(hero, BorderLayout.WEST);
        row.add(pollutants(), BorderLayout.CENTER);
        return row;
    }

    private JComponent chip() {
        category.setOpaque(true);
        category.setBorder(new EmptyBorder(4, 10, 4, 10));
        var holder = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.add(category);
        return holder;
    }

    private JPanel pollutants() {
        var grid = panel(new GridLayout(2, 2, GAP, GAP));
        grid.add(metric("PM2.5 · fine particles", pm25));
        grid.add(metric("PM10 · coarse particles", pm10));
        grid.add(metric("NO₂ · nitrogen dioxide", no2));
        grid.add(metric("O₃ · ozone", o3));
        return grid;
    }

    private JPanel guidance(JTextArea details) {
        var columns = panel(new GridLayout(1, 3, GAP, 0));
        var advice = card(new BorderLayout(0, GAP));
        advice.add(adviceTitle, BorderLayout.NORTH);
        advice.add(adviceText, BorderLayout.CENTER);
        columns.add(advice);
        columns.add(titled("Nearby green space", park));
        columns.add(titled("Source and timing", details));
        return columns;
    }

    /** Shows the selected district's result; {@code null} means nothing has been loaded yet. */
    void render(District district, Result result, String source, AdviceProfile profile) {
        Reading r = result == null ? null : result.reading();
        area.setText(district.toString().toUpperCase(java.util.Locale.ENGLISH) + "  ·  U.S. AQI");
        provenance.setText(source);
        provenance.setForeground(result != null && result.state() == cityair.service.ReadingService.State.CACHED ? AMBER : MUTED);
        index.setText(r == null ? "—" : Integer.toString(r.aqi()));
        scale.setIndex(r == null ? null : r.aqi());
        renderCategory(r);
        renderPollutants(r);
        adviceTitle.setText(("Advice · " + profile).toUpperCase(java.util.Locale.ENGLISH));
        adviceText.setText(html(r == null ? "Load a reading to see advice for this audience."
                : profile.strategy().advice(r.category())));
        park.setText(html(parkText(district)));
    }

    private void renderCategory(Reading r) {
        var fill = r == null ? PALE : CategoryPalette.fill(r.category());
        category.setText(r == null ? "No verified sample" : r.category().toString());
        category.setBackground(fill);
        category.setForeground(r == null ? MUTED : CategoryPalette.onFill(r.category()));
        hero.setBorder(r == null ? cardBorder(LINE) : accentBorder(fill));
    }

    private void renderPollutants(Reading r) {
        pm25.setText(Formats.quantity(r == null ? null : r.pm25()));
        pm10.setText(Formats.quantity(r == null ? null : r.pm10()));
        no2.setText(Formats.quantity(r == null ? null : r.nitrogenDioxide()));
        o3.setText(Formats.quantity(r == null ? null : r.ozone()));
    }

    private static String parkText(District district) {
        var text = new StringBuilder();
        for (CityCatalog.Park item : CityCatalog.nearby(district)) {
            text.append("<b>").append(item.name()).append("</b><br>").append(item.area())
                    .append("<br><font color='#5A707A'>Listed by ").append(item.source().getHost()).append("</font>");
        }
        return text.toString();
    }

    private static JPanel metric(String title, JLabel value) {
        var metric = card(new GridLayout(0, 1, 0, 4));
        metric.add(caption(title));
        metric.add(value);
        value.getAccessibleContext().setAccessibleName(title + " concentration");
        return metric;
    }

    private static JLabel metricValue(String name) {
        return named(label(Formats.MISSING, HEADING + 4, Font.BOLD, TEXT), name);
    }

    private static JLabel wrapped(String name) {
        var text = named(label("", BODY, Font.PLAIN, TEXT), name);
        text.setVerticalAlignment(SwingConstants.TOP);
        return text;
    }

    private static String html(String body) { return "<html><body style='width:" + TEXT_WIDTH + "px'>" + body + "</body></html>"; }
}
