package cityair.view;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.CityCatalog;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ActivityPlanner;
import cityair.service.ActivityPlanner.Plan;
import cityair.service.ActivityPlanner.Verdict;
import cityair.strategy.AdviceProfile;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Badge;
import cityair.view.component.Card;
import cityair.view.component.Panels;
import cityair.view.component.Paragraph;
import cityair.view.component.SegmentedControl;
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
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * The activity planner: choose who is going out, how hard and for how long, and see how that fits
 * the air quality currently being shown.
 *
 * <p>The verdict comes from {@link ActivityPlanner}, and the screen shows the arithmetic that
 * produced it rather than presenting a conclusion the reader has to trust. Beneath it, the same
 * reading is passed through all three advice strategies at once, so the difference between the
 * policies is visible instead of hidden behind the selector.</p>
 */
final class PlannerPane extends JPanel implements Themed {
    private static final int VERDICT_HEIGHT = 260, COMPARISON_HEIGHT = 210, PRECAUTION_WIDTH = 330;
    private final JTextArea advice;
    private final SegmentedControl<ActivityIntensity> intensity =
            new SegmentedControl<>(List.of(ActivityIntensity.values()), ActivityIntensity::toString);
    private final SegmentedControl<ActivityWindow> window =
            new SegmentedControl<>(List.of(ActivityWindow.values()), ActivityWindow::toString);
    private final AuroraLabel verdictLabel = Text.named(new AuroraLabel("No verified reading",
            Type.strong(Type.TITLE), Tone.INK), "planVerdict");
    private final Badge verdictBadge = new Badge("Not assessed", Tone.INK_MUTED);
    private final Paragraph verdictReason = Text.named(Text.note(""), "planReason");
    private final JPanel precautions = new JPanel();
    private final Map<AdviceProfile, Card> strategyCards = new EnumMap<>(AdviceProfile.class);
    private final Map<AdviceProfile, Paragraph> strategyText = new EnumMap<>(AdviceProfile.class);
    private final ParkCard park = new ParkCard("plannerParkCard");

    /** Builds the screen around the shell's audience selector and detail text area. */
    PlannerPane(JComboBox<AdviceProfile> profile, JTextArea advice) {
        super(new BorderLayout(0, Metrics.SPACE_5));
        this.advice = advice;
        setOpaque(false);
        add(Panels.stack(Metrics.SPACE_5, controls(profile), verdict(), comparison(), detail()),
                BorderLayout.CENTER);
    }

    /** Reports a change to the chosen activity intensity. */
    void onIntensity(Consumer<ActivityIntensity> action) { intensity.onChange(action); }

    /** Reports a change to the chosen activity length. */
    void onWindow(Consumer<ActivityWindow> action) { window.onChange(action); }

    /** Applies saved planner choices without reporting them as user changes. */
    void restore(ActivityIntensity savedIntensity, ActivityWindow savedWindow) {
        intensity.select(savedIntensity);
        window.select(savedWindow);
    }

    /** Redraws the plan, the strategy comparison and the detail text from one snapshot. */
    void render(ViewState state) {
        Reading reading = state.reading();
        advice.setText(detailText(state));
        renderPlan(state, reading);
        renderStrategies(state, reading);
        park.render(state.district(), reading);
    }

    private void renderPlan(ViewState state, Reading reading) {
        if (reading == null) {
            verdictLabel.setText("No verified reading");
            verdictLabel.setForeground(Skin.color(Tone.INK_MUTED));
            verdictBadge.set("Not assessed", () -> Skin.color(Tone.INK_MUTED));
            verdictReason.setText("Refresh " + state.district() + " to assess a plan against a verified model hour.");
            renderPrecautions(List.of());
            return;
        }
        Plan plan = ActivityPlanner.plan(reading.category(), state.intensity(), state.window(), state.profile());
        verdictLabel.setText(plan.verdict().toString());
        verdictLabel.setForeground(Skin.color(verdictTone(plan.verdict())));
        verdictBadge.set(reading.category().toString(), () -> CategoryPalette.fill(reading.category()));
        verdictReason.setText(plan.reason() + " " + plan.guidance());
        renderPrecautions(plan.precautions());
        describePlan(state, plan);
    }

    private void describePlan(ViewState state, Plan plan) {
        getAccessibleContext().setAccessibleDescription(plan.verdict() + " for a " + state.intensity()
                + " activity of " + state.window() + " as " + state.profile() + ". " + plan.reason());
    }

    private static Tone verdictTone(Verdict verdict) {
        return switch (verdict) {
            case SUITABLE -> Tone.POSITIVE;
            case ACCEPTABLE -> Tone.INK;
            case MODIFY -> Tone.CAUTION;
            case RESCHEDULE -> Tone.DANGER;
        };
    }

    private void renderPrecautions(List<String> steps) {
        precautions.removeAll();
        if (steps.isEmpty()) {
            precautions.add(Text.micro("No additional precautions apply to this plan."));
        }
        for (String step : steps) {
            precautions.add(bullet(step));
            precautions.add(Panels.gap(Metrics.SPACE_2));
        }
        precautions.revalidate();
        precautions.repaint();
    }

    private static JComponent bullet(String step) {
        var row = Panels.row(Metrics.SPACE_2);
        row.setAlignmentX(LEFT_ALIGNMENT);
        var dot = new AuroraLabel("•", Type.strong(Type.BODY), Tone.ACCENT);
        row.add(dot, BorderLayout.WEST);
        row.add(Text.note(step), BorderLayout.CENTER);
        return row;
    }

    private void renderStrategies(ViewState state, Reading reading) {
        for (AdviceProfile audience : AdviceProfile.values()) {
            String body = reading == null
                    ? "No verified reading yet."
                    : audience.strategy().advice(reading.category());
            strategyText.get(audience).setText(body);
            strategyCards.get(audience).setSelected(audience == state.profile());
        }
    }

    private JComponent controls(JComboBox<AdviceProfile> profile) {
        var row = Panels.columns(3, Metrics.SPACE_4);
        row.add(field("Audience", profile, "Each audience applies its own advice strategy."));
        row.add(field("Effort", intensity, "Harder effort draws more air in the same time."));
        row.add(field("Time outdoors", window, "Longer sessions increase total exposure."));
        var card = Card.titled("Plan an activity", row);
        card.setPreferredSize(new Dimension(0, 150));
        return card;
    }

    private static JComponent field(String caption, JComponent control, String note) {
        var column = Panels.column(Metrics.SPACE_2);
        column.add(Text.caption(caption), BorderLayout.NORTH);
        // The control fills the column rather than keeping its preferred width, so a narrow
        // window shortens the segments instead of cutting the last option off.
        column.add(Panels.top(control), BorderLayout.CENTER);
        column.add(Text.micro(note), BorderLayout.SOUTH);
        return column;
    }

    private JComponent verdict() {
        precautions.setLayout(new BoxLayout(precautions, BoxLayout.Y_AXIS));
        precautions.setOpaque(false);
        var headline = Panels.row(Metrics.SPACE_3);
        headline.add(verdictLabel, BorderLayout.CENTER);
        headline.add(verdictBadge, BorderLayout.EAST);
        var left = Panels.column(Metrics.SPACE_3);
        left.add(headline, BorderLayout.NORTH);
        left.add(verdictReason, BorderLayout.CENTER);
        left.add(Text.note(ActivityPlanner.BASIS), BorderLayout.SOUTH);
        var row = Panels.row(Metrics.SPACE_5);
        row.add(left, BorderLayout.CENTER);
        row.add(precautionsCard(), BorderLayout.EAST);
        var card = Card.titled("Assessment for this plan", row);
        card.setPreferredSize(new Dimension(0, VERDICT_HEIGHT));
        return card;
    }

    private JComponent precautionsCard() {
        var column = Panels.column(Metrics.SPACE_3);
        column.add(Text.caption("Practical steps"), BorderLayout.NORTH);
        column.add(Panels.top(precautions), BorderLayout.CENTER);
        return Panels.fixedWidth(column, PRECAUTION_WIDTH);
    }

    private JComponent comparison() {
        var row = Panels.columns(AdviceProfile.values().length, Metrics.SPACE_4);
        for (AdviceProfile audience : AdviceProfile.values()) {
            var text = Text.note("");
            var card = Card.titled(audience.toString(), text);
            strategyCards.put(audience, card);
            strategyText.put(audience, text);
            row.add(card);
        }
        var card = Card.titled("The same reading through all three advice strategies", row);
        card.setPreferredSize(new Dimension(0, COMPARISON_HEIGHT));
        return card;
    }

    private JComponent detail() {
        var row = Panels.columns(2, Metrics.SPACE_5);
        row.add(Card.titled("Selected guidance and nearby park", advice));
        row.add(park);
        return row;
    }

    private static String detailText(ViewState state) {
        Reading reading = state.reading();
        var text = new StringBuilder(state.profile() + " · " + state.district() + "\n");
        if (reading == null) {
            text.append("No verified reading. Check local guidance before planning.");
        } else {
            text.append(state.source()).append(" · ").append(Formats.time(reading.validAt()))
                    .append(" · ").append(reading.category()).append("\n")
                    .append(state.profile().strategy().advice(reading.category()));
        }
        for (CityCatalog.Park item : CityCatalog.nearby(state.district())) {
            text.append("\n\nNearby green space: ").append(item.name()).append(" (").append(item.area())
                    .append(")\nSource: ").append(item.source());
        }
        return text.append("\n\nDistrict model context only. This guidance is informational.").toString();
    }

    /** Returns the districts offered by the catalogue, used by the shell for accessibility text. */
    static List<District> areas() { return List.of(District.values()); }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
