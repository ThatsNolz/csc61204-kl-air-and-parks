package cityair.view;

import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import cityair.service.TrendSummary;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Badge;
import cityair.view.component.Card;
import cityair.view.component.FlowGrid;
import cityair.view.component.Formatting;
import cityair.view.component.Panels;
import cityair.view.component.Sparkline;
import cityair.view.component.Text;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Every pilot area as a card: its index, its category in words, how far it is from the selected
 * area and what it has done during this session.
 *
 * <p>The cards are the application's area switcher as well as its comparison. Each one is
 * focusable and activates with Enter or Space, so switching area never requires a pointer, and an
 * area with no verified reading says so rather than showing an empty figure.</p>
 */
final class DistrictStrip extends JPanel implements Themed {
    private static final int CARD_WIDTH = 250, CARD_HEIGHT = 128;
    private final Map<District, AreaCard> cards = new EnumMap<>(District.class);
    private transient Consumer<District> chooser = district -> { };

    DistrictStrip() {
        super(new BorderLayout(0, Metrics.SPACE_3));
        setOpaque(false);
        add(Text.caption("All pilot areas · select to switch"), BorderLayout.NORTH);
        var row = Panels.of(new FlowGrid(CARD_WIDTH, District.values().length, Metrics.SPACE_4));
        for (District area : District.values()) {
            var card = new AreaCard(area, () -> chooser.accept(area));
            cards.put(area, card);
            row.add(card);
        }
        add(row, BorderLayout.CENTER);
    }

    /** Reports the area whose card was chosen. */
    void onChoose(Consumer<District> action) { chooser = Objects.requireNonNull(action); }

    /** Updates every card from one snapshot. */
    void render(ViewState state) {
        cards.forEach((area, card) -> card.render(state.results().get(area), area == state.district(),
                state.district(), state.results(), area == state.district() ? state.history() : List.of()));
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    /** One area: index, category, distance from the selected area and this session's samples. */
    private static final class AreaCard extends Card {
        private final transient District area;
        private final AuroraLabel value;
        private final Badge category = new Badge(Formatting.NOT_LOADED, Tone.INK_MUTED);
        private final AuroraLabel relation = Text.micro(" ");
        private final Sparkline spark = new Sparkline();

        AreaCard(District area, Runnable choose) {
            super(new BorderLayout(Metrics.SPACE_3, 0));
            this.area = area;
            this.value = Text.named(new AuroraLabel("—", Type.numberStrong(Type.TITLE), Tone.INK),
                    "stripAqi." + area.name());
            padded(Metrics.SPACE_4);
            interactive("Show " + area, choose);
            add(details(), BorderLayout.CENTER);
            add(figure(), BorderLayout.EAST);
            setPreferredSize(new java.awt.Dimension(CARD_WIDTH, CARD_HEIGHT));
        }

        private JComponent details() {
            var stack = Panels.column(Metrics.SPACE_2);
            stack.add(new AuroraLabel(area.toString(), Type.strong(Type.BODY), Tone.INK), BorderLayout.NORTH);
            stack.add(Panels.leading(category), BorderLayout.CENTER);
            stack.add(relation, BorderLayout.SOUTH);
            return stack;
        }

        private JComponent figure() {
            var column = Panels.column(Metrics.SPACE_2);
            column.add(value, BorderLayout.NORTH);
            spark.setPreferredSize(new java.awt.Dimension(76, 30));
            column.add(spark, BorderLayout.SOUTH);
            return column;
        }

        void render(Result result, boolean selected, District current, Map<District, Result> all, List<Reading> history) {
            Reading reading = result == null ? null : result.reading();
            setSelected(selected);
            accent(() -> reading == null ? Skin.color(Tone.STROKE_STRONG) : CategoryPalette.fill(reading.category()));
            value.setText(reading == null ? "—" : Integer.toString(reading.aqi()));
            value.setForeground(Skin.color(reading == null ? Tone.INK_FAINT : Tone.INK));
            renderCategory(result, reading);
            relation.setText(relationText(reading, selected, current, all));
            spark.setValues(TrendSummary.indices(history),
                    () -> reading == null ? CategoryPalette.unknown() : CategoryPalette.fill(reading.category()));
            describe(reading);
        }

        private void renderCategory(Result result, Reading reading) {
            if (reading == null) {
                category.set(result == null ? Formatting.NOT_LOADED : Formatting.ABSENT,
                        () -> Skin.color(Tone.INK_FAINT));
                return;
            }
            category.set(reading.category().toString(), () -> CategoryPalette.fill(reading.category()));
        }

        private String relationText(Reading reading, boolean selected, District current, Map<District, Result> all) {
            String distance = String.format(java.util.Locale.ENGLISH, "%.1f km from %s",
                    cityair.view.chart.AreaPlot.distanceKm(area, current), current);
            if (selected) return "Selected area";
            Result currentResult = all.get(current);
            if (reading == null || currentResult == null || currentResult.reading() == null) return distance;
            int difference = reading.aqi() - currentResult.reading().aqi();
            String comparison = difference == 0 ? "same index" : Math.abs(difference)
                    + (difference > 0 ? " higher" : " lower");
            return distance + " · " + comparison;
        }

        private void describe(Reading reading) {
            getAccessibleContext().setAccessibleDescription(area + ": "
                    + (reading == null ? "no verified reading" : "U.S. AQI " + reading.aqi() + ", " + reading.category())
                    + ". " + relation.getText() + ". " + spark.describe());
        }
    }
}
