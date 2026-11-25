package cityair.view;

import cityair.model.District;
import cityair.model.GuidelineLevel;
import cityair.model.Reading;
import cityair.service.InsightEngine;
import cityair.service.TrendSummary;
import cityair.view.chart.AqiDial;
import cityair.view.chart.BandScale;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Card;
import cityair.view.component.FlowGrid;
import cityair.view.component.MetricTile;
import cityair.view.component.Panels;
import cityair.view.component.Paragraph;
import cityair.view.component.Text;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * The overview: one dial for the headline index, the pollutants behind it, what the application
 * can say about them, and every pilot area at a glance.
 *
 * <p>The screen is ordered by how quickly a reader needs each part. The dial and its category
 * answer "is the air acceptable right now" in about a second; the tiles answer "because of what";
 * the statements answer "what does that mean for me"; and the area strip answers "is somewhere
 * nearby better". Provenance sits at the foot of the screen, always present and never competing
 * with the reading.</p>
 */
final class OverviewPane extends JPanel implements Themed {
    private static final int HERO_WIDTH = 490, HERO_HEIGHT = 330, DIAL_WIDTH = 250;
    private final AqiDial dial = new AqiDial(CategoryPalette::forIndex);
    private final BandScale scale = new BandScale(CategoryPalette::forIndex);
    private final AuroraLabel area = Text.caption("");
    private final AuroraLabel index = Text.named(new AuroraLabel("—", Type.numberStrong(Type.DISPLAY), Tone.INK), "aqiValue");
    private final AuroraLabel category = Text.named(new AuroraLabel("No verified sample",
            Type.strong(Type.HEADING), Tone.INK_MUTED), "aqiCategory");
    private final AuroraLabel provenance = Text.named(new AuroraLabel("NOT LOADED", Type.caption(), Tone.INK_MUTED),
            "readingSource");
    private final Map<GuidelineLevel, MetricTile> tiles = new EnumMap<>(GuidelineLevel.class);
    private final InsightList insights = new InsightList();
    private final DistrictStrip strip = new DistrictStrip();
    private final ParkCard park = new ParkCard("parkCard");
    private final Card hero = new Card(new BorderLayout(Metrics.SPACE_5, 0));
    private final AuroraLabel modelHour = new AuroraLabel(Formats.DASH, Type.number(Type.LABEL), Tone.INK);
    private final AuroraLabel received = new AuroraLabel(Formats.DASH, Type.text(Type.LABEL), Tone.INK_MUTED);
    private final AuroraLabel guidanceTitle = Text.caption("Guidance");
    private final Paragraph guidanceText = Text.named(Text.paragraph(""), "adviceSummary");

    /** Builds the screen around the shared provenance text area owned by the shell. */
    OverviewPane(JTextArea provenanceDetails) {
        super(new BorderLayout(0, Metrics.SPACE_5));
        setOpaque(false);
        add(top(), BorderLayout.NORTH);
        add(Panels.stack(Metrics.SPACE_5, pollutants(), strip, bottom(provenanceDetails)), BorderLayout.CENTER);
    }

    /** Selects a district when one of the area cards is chosen. */
    void onDistrictChosen(Consumer<District> action) { strip.onChoose(action); }

    /** Redraws every part of the screen from one snapshot. */
    void render(ViewState state) {
        Reading reading = state.reading();
        renderHeadline(state, reading);
        renderCategory(reading);
        renderTiles(state);
        insights.render(InsightEngine.insights(state.district(), state.readings(), state.history(),
                state.threshold(), state.now()));
        guidanceTitle.setText(("Guidance · " + state.profile()).toUpperCase(Locale.ENGLISH));
        guidanceText.setText(reading == null
                ? "Load a reading to see guidance for this audience."
                : state.profile().strategy().advice(reading.category()));
        park.render(state.district(), reading);
        strip.render(state);
    }

    private void renderHeadline(ViewState state, Reading reading) {
        area.setText((state.district() + "  ·  U.S. AQI").toUpperCase(Locale.ENGLISH));
        provenance.setText(state.source());
        provenance.setForeground(Skin.color(provenanceTone(state)));
        index.setText(reading == null ? "—" : Integer.toString(reading.aqi()));
        dial.setIndex(reading == null ? null : reading.aqi());
        dial.setThreshold(state.threshold());
        scale.setIndex(reading == null ? null : reading.aqi());
        modelHour.setText(reading == null ? Formats.DASH : Formats.shortTime(reading.validAt()) + " MYT");
        received.setText(reading == null ? Formats.DASH : Formats.since(reading.fetchedAt(), state.now()));
    }

    private Tone provenanceTone(ViewState state) {
        if (state.result() == null) return Tone.INK_FAINT;
        return switch (state.result().state()) {
            case CACHED -> Tone.CAUTION;
            case UNAVAILABLE -> Tone.DANGER;
            case LIVE -> Tone.INK_MUTED;
        };
    }

    private void renderCategory(Reading reading) {
        boolean known = reading != null;
        category.setText(known ? reading.category().toString() : "No verified sample");
        category.setForeground(known ? CategoryPalette.ink(reading.category()) : Skin.color(Tone.INK_MUTED));
        hero.accent(() -> known ? CategoryPalette.fill(reading.category()) : Skin.color(Tone.STROKE_STRONG));
        hero.repaint();
    }

    private void renderTiles(ViewState state) {
        Reading reading = state.reading();
        List<Integer> trend = TrendSummary.indices(state.history());
        for (var entry : tiles.entrySet()) {
            GuidelineLevel pollutant = entry.getKey();
            MetricTile tile = entry.getValue();
            Double value = reading == null ? null : pollutant.measure(reading);
            if (value == null) tile.clear(); else tile.present();
            tile.set(value == null ? Formats.MISSING : Formats.quantity(value), guidelineText(reading, pollutant));
            tile.setTrend(pollutant == GuidelineLevel.PM25 ? trend : List.of(),
                    () -> reading == null ? CategoryPalette.unknown() : CategoryPalette.forIndex(reading.aqi()));
        }
    }

    private static String guidelineText(Reading reading, GuidelineLevel pollutant) {
        if (reading == null) return "No verified value for this hour";
        Double percent = pollutant.percentOfLevel(reading);
        if (percent == null) return "Not supplied in this response";
        return String.format(Locale.ENGLISH, "%.0f%% of the WHO %s guideline (%.0f)",
                percent, pollutant.period(), pollutant.level());
    }

    private JComponent top() {
        var row = Panels.row(Metrics.SPACE_5);
        row.add(heroCard(), BorderLayout.WEST);
        row.add(Card.titled("What the readings show", insights), BorderLayout.CENTER);
        return row;
    }

    private JComponent heroCard() {
        hero.setPreferredSize(new Dimension(HERO_WIDTH, HERO_HEIGHT));
        hero.radius(Metrics.RADIUS_HERO).elevation(2);
        dial.setReadout(readout());
        var right = Panels.column(Metrics.SPACE_4);
        right.add(area, BorderLayout.NORTH);
        right.add(Panels.top(Panels.stack(Metrics.SPACE_5, scale, times())), BorderLayout.CENTER);
        right.add(provenance, BorderLayout.SOUTH);
        hero.add(Panels.fixedWidth(dialBlock(), DIAL_WIDTH), BorderLayout.WEST);
        hero.add(right, BorderLayout.CENTER);
        return hero;
    }

    /** The two times behind the figure: the hour it represents and when it was received. */
    private JComponent times() {
        var rows = Panels.rows(Metrics.SPACE_3);
        rows.add(timeRow("Model hour", modelHour));
        rows.add(timeRow("Received", received));
        return rows;
    }

    private static JComponent timeRow(String caption, AuroraLabel value) {
        var row = Panels.column(1);
        row.add(Text.caption(caption), BorderLayout.NORTH);
        row.add(value, BorderLayout.SOUTH);
        return row;
    }

    private JComponent readout() {
        index.setHorizontalAlignment(SwingConstants.CENTER);
        var unit = Text.micro("U.S. AQI");
        unit.setHorizontalAlignment(SwingConstants.CENTER);
        var stack = Panels.column(0);
        var middle = Panels.column(0);
        middle.add(index, BorderLayout.CENTER);
        middle.add(unit, BorderLayout.SOUTH);
        stack.add(middle, BorderLayout.CENTER);
        return stack;
    }

    /** The dial with its category written beneath, where a long category name still fits. */
    private JComponent dialBlock() {
        category.setHorizontalAlignment(SwingConstants.CENTER);
        var block = Panels.column(Metrics.SPACE_2);
        block.add(dial, BorderLayout.CENTER);
        block.add(category, BorderLayout.SOUTH);
        return block;
    }

    private JComponent pollutants() {
        var grid = Panels.of(new FlowGrid(MetricTile.minimumWidth(), GuidelineLevel.values().length, Metrics.SPACE_4));
        tiles.put(GuidelineLevel.PM25, tile(GuidelineLevel.PM25, "PM2.5 · fine particles", "pm25Value"));
        tiles.put(GuidelineLevel.PM10, tile(GuidelineLevel.PM10, "PM10 · coarse particles", "pm10Value"));
        tiles.put(GuidelineLevel.NITROGEN_DIOXIDE,
                tile(GuidelineLevel.NITROGEN_DIOXIDE, "NO₂ · nitrogen dioxide", "no2Value"));
        tiles.put(GuidelineLevel.OZONE, tile(GuidelineLevel.OZONE, "O₃ · ozone", "o3Value"));
        tiles.values().forEach(grid::add);
        return grid;
    }

    private MetricTile tile(GuidelineLevel pollutant, String caption, String name) {
        var tile = new MetricTile(caption, name);
        if (pollutant != GuidelineLevel.PM25) tile.withoutTrend();
        return tile;
    }

    private JComponent bottom(JTextArea provenanceDetails) {
        var row = Panels.columns(3, Metrics.SPACE_5);
        row.add(guidance());
        row.add(park);
        row.add(Card.titled("Source and timing", provenanceDetails));
        return row;
    }

    private JComponent guidance() {
        guidanceTitle.setText("Guidance");
        var body = Panels.column(Metrics.SPACE_3);
        body.add(guidanceText, BorderLayout.CENTER);
        body.add(Text.micro("General information for planning, not medical advice."), BorderLayout.SOUTH);
        var card = new Card(new BorderLayout(0, Metrics.SPACE_3));
        card.add(guidanceTitle, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
