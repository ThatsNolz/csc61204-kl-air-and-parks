package cityair.view;

import cityair.model.AirCategory;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import cityair.service.TrendSummary;
import cityair.view.chart.AreaPlot;
import cityair.view.chart.ComparisonBars;
import cityair.view.chart.TrendChart;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Card;
import cityair.view.component.DataTable;
import cityair.view.component.Panels;
import cityair.view.component.Scrolls;
import cityair.view.component.Text;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

/**
 * Live monitoring: the pilot areas compared, where they are in relation to each other, and the
 * model hours this session has collected for the selected area.
 *
 * <p>Every figure drawn here also appears as text. The comparison has a table, the chart has the
 * sample table beside it and a summary beneath it, and the area plot states its coordinates in its
 * accessible description. A chart is the fastest way to see a shape; the table is the only way to
 * read an exact value, and neither replaces the other.</p>
 */
final class MonitoringPane extends JPanel implements Themed {
    private static final int TREND_HEIGHT = 330, GEOGRAPHY_HEIGHT = 300, COMPARISON_WIDTH = 380;
    private static final int CATEGORY_COLUMN = 2;
    private static final int[] DISTRICT_WIDTHS = {190, 90, 240, 130, 160};
    private static final int[] SAMPLE_WIDTHS = {140, 80, 110, 110};
    private final DefaultTableModel districts = DataTable.model("Pilot area", "U.S. AQI", "Category", "Source",
            "Model time (MYT)");
    private final DefaultTableModel samples = DataTable.model("Model time (MYT)", "U.S. AQI", "PM2.5 (µg/m³)",
            "PM10 (µg/m³)");
    private final ComparisonBars comparison = new ComparisonBars(CategoryPalette::forIndex);
    private final AreaPlot plot = new AreaPlot(CategoryPalette::forIndex);
    private final TrendChart chart = new TrendChart(CategoryPalette::forIndex, MonitoringPane::describeSample);
    private final AuroraLabel samplesStat = statValue();
    private final AuroraLabel lowestStat = statValue();
    private final AuroraLabel highestStat = statValue();
    private final AuroraLabel averageStat = statValue();
    private final AuroraLabel changeStat = statValue();

    MonitoringPane() {
        super(new BorderLayout(0, Metrics.SPACE_5));
        setOpaque(false);
        add(Panels.stack(Metrics.SPACE_5, trend(), table(), geography()), BorderLayout.CENTER);
    }

    /** Selects a district when a marker on the area plot is chosen. */
    void onDistrictChosen(Consumer<District> action) { plot.onChoose(action); }

    /** Redraws the comparison, the plot, both tables and the chart from one snapshot. */
    void render(ViewState state) {
        comparison.setValues(state.indices(), state.district());
        plot.setValues(state.indices(), state.district());
        districts.setRowCount(0);
        for (District area : District.values()) {
            districts.addRow(districtRow(area, state.results().get(area), state.fixture()));
        }
        samples.setRowCount(0);
        state.history().stream().sorted(Comparator.comparing(Reading::validAt))
                .forEach(reading -> samples.addRow(sampleRow(reading)));
        chart.setHistory(state.district(), state.history(), state.threshold());
        renderSummary(TrendSummary.of(state.history()));
    }

    private void renderSummary(TrendSummary summary) {
        boolean has = summary.hasData();
        samplesStat.setText(has ? Integer.toString(summary.samples()) : Formats.DASH);
        lowestStat.setText(has ? Integer.toString(summary.lowest()) : Formats.DASH);
        highestStat.setText(has ? Integer.toString(summary.highest()) : Formats.DASH);
        averageStat.setText(has ? String.format(Locale.ENGLISH, "%.1f", summary.average()) : Formats.DASH);
        changeStat.setText(changeText(summary));
        changeStat.setForeground(Skin.color(changeTone(summary)));
    }

    private static String changeText(TrendSummary summary) {
        if (summary.samples() < 2) return Formats.DASH;
        return (summary.change() > 0 ? "+" : "") + summary.change();
    }

    private static Tone changeTone(TrendSummary summary) {
        return switch (summary.direction()) {
            case RISING -> Tone.CAUTION;
            case FALLING -> Tone.POSITIVE;
            case STEADY, UNKNOWN -> Tone.INK;
        };
    }

    /** The session chart, with every area's latest index beside it for context. */
    private JComponent trend() {
        Text.described(chart, "trendChart",
                "Session U.S. AQI chart; exact values are listed in the table below");
        var left = Panels.column(Metrics.SPACE_4);
        left.add(chart, BorderLayout.CENTER);
        left.add(summaryStrip(), BorderLayout.SOUTH);
        var row = Panels.row(Metrics.SPACE_5);
        row.add(Card.titled("Session trend for the selected area · distinct model hours only", left),
                BorderLayout.CENTER);
        row.add(Panels.fixedWidth(Card.titled("Latest verified index by area", Panels.top(comparison)),
                COMPARISON_WIDTH),
                BorderLayout.EAST);
        row.setPreferredSize(new Dimension(0, TREND_HEIGHT));
        return row;
    }

    /** The exact values behind the chart, beside where the areas are in relation to each other. */
    private JComponent geography() {
        var table = new DataTable(samples, 1, 2, 3).visibleRows(5);
        Text.described(table, "sampleTable", "Exact value of every model hour collected in this session");
        table.widths(SAMPLE_WIDTHS);
        var body = Panels.column(Metrics.SPACE_3);
        body.add(plot, BorderLayout.CENTER);
        body.add(Text.note("Placed from reference coordinates on an equal-distance projection. This is not a "
                + "map: no boundaries, streets or basemap are implied."), BorderLayout.SOUTH);
        var row = Panels.columns(2, Metrics.SPACE_5);
        row.add(Card.titled("Model hours collected in this session", Scrolls.of(table)));
        row.add(Card.titled("Relative position of the pilot areas", body));
        row.setPreferredSize(new Dimension(0, GEOGRAPHY_HEIGHT));
        return row;
    }

    private JComponent table() {
        var table = new DataTable(districts, 1).visibleRows(District.values().length);
        Text.described(table, "districtTable", "Every pilot area with its index, category and provenance");
        table.widths(DISTRICT_WIDTHS);
        table.renderer(CATEGORY_COLUMN, DataTable.pill(MonitoringPane::categoryTint));
        return Card.titled("District comparison", Scrolls.of(table));
    }

    /** The pill colour for a category cell, or null for a row with no verified category. */
    private static Color categoryTint(Object value) {
        return value instanceof AirCategory category ? CategoryPalette.fill(category) : null;
    }

    private JComponent summaryStrip() {
        var strip = Panels.columns(5, Metrics.SPACE_3);
        strip.add(stat("Samples", samplesStat));
        strip.add(stat("Lowest", lowestStat));
        strip.add(stat("Highest", highestStat));
        strip.add(stat("Average", averageStat));
        strip.add(stat("Change", changeStat));
        return strip;
    }

    private static JComponent stat(String caption, AuroraLabel value) {
        var column = Panels.column(2);
        column.add(Text.caption(caption), BorderLayout.NORTH);
        column.add(value, BorderLayout.CENTER);
        return column;
    }

    private static AuroraLabel statValue() {
        return new AuroraLabel(Formats.DASH, Type.numberStrong(Type.HEADING), Tone.INK);
    }

    private static Object[] districtRow(District area, Result result, boolean fixture) {
        if (result == null) {
            return new Object[]{area, Formats.DASH, Formats.DASH, "Not loaded", Formats.DASH};
        }
        Reading reading = result.reading();
        if (reading == null) {
            return new Object[]{area, Formats.DASH, Formats.DASH, Formats.MISSING, Formats.DASH};
        }
        return new Object[]{area, reading.aqi(), reading.category(), Formats.tableSource(result, fixture),
                Formats.shortTime(reading.validAt())};
    }

    private static Object[] sampleRow(Reading reading) {
        return new Object[]{Formats.shortTime(reading.validAt()), reading.aqi(),
                cell(reading.pm25()), cell(reading.pm10())};
    }

    private static Object cell(Double value) { return value == null ? Formats.DASH : value; }

    /** Tooltip and axis text for one plotted sample. */
    private static String describeSample(Reading reading, boolean detailed) {
        if (!detailed) return Formats.hour(reading.validAt());
        return Formats.shortTime(reading.validAt()) + "  ·  AQI " + reading.aqi()
                + "  ·  PM2.5 " + Formats.amount(reading.pm25());
    }

    /** Returns the sample rows, so the integration tests can read exact plotted values. */
    List<Object> sampleColumn(int column) {
        return java.util.stream.IntStream.range(0, samples.getRowCount())
                .mapToObj(row -> samples.getValueAt(row, column)).toList();
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
