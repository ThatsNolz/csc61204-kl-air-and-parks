package cityair.service;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.AirCategory;
import cityair.strategy.AdviceProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decides how well a planned outdoor activity fits the air quality currently being shown.
 *
 * <p>The decision is a published rule table, not a model and not a prediction. Three inputs the
 * reader chooses, the intensity of the activity, how long it lasts and which audience it is for,
 * are combined with the air-quality category into one exposure score, and the score selects a
 * verdict. One rule overrides the score: the two worst categories always recommend rescheduling,
 * however light and short the plan is.</p>
 *
 * <table><caption>Exposure score</caption>
 * <tr><td>category</td><td>its position in the U.S. AQI scale, counted from zero, doubled</td></tr>
 * <tr><td>intensity</td><td>light 0, moderate 1, vigorous 2</td></tr>
 * <tr><td>duration</td><td>up to 30 minutes 0, about an hour 1, two hours or more 2</td></tr>
 * <tr><td>audience</td><td>1 for children and older adults, 0 for the general public</td></tr>
 * </table>
 *
 * <p>The audience's own {@link cityair.strategy.AdviceStrategy} supplies the guidance sentence, so
 * the planner extends the existing Strategy implementation rather than duplicating its policy.
 * Everything produced here is general information for planning, not medical advice.</p>
 */
public final class ActivityPlanner {
    /** Stated wherever a plan is shown. */
    public static final String BASIS =
            "Rule-based guidance from the published category, your chosen activity and audience. "
                    + "No prediction or machine learning is involved, and this is not medical advice.";
    private static final int SUITABLE_LIMIT = 2, ACCEPTABLE_LIMIT = 5, MODIFY_LIMIT = 8;
    private static final int SENSITIVE_AUDIENCE_WEIGHT = 1;

    /** How well the plan fits the conditions. */
    public enum Verdict {
        /** Conditions suit the plan as described. */
        SUITABLE("Good to go"),
        /** The plan is reasonable with the usual checks. */
        ACCEPTABLE("Reasonable with care"),
        /** The plan should be made shorter or easier. */
        MODIFY("Shorten or ease the plan"),
        /** The plan should move indoors or to another time. */
        RESCHEDULE("Choose another time or indoors");

        private final String label;

        Verdict(String label) { this.label = label; }

        /** Returns the display label. */
        @Override public String toString() { return label; }
    }

    /**
     * The result of planning one activity.
     *
     * @param verdict how well the plan fits
     * @param score the exposure score the rules produced
     * @param reason the arithmetic behind the verdict, written out for the reader
     * @param guidance the audience strategy's advice for this category
     * @param precautions practical steps, which may be empty when none apply
     */
    public record Plan(Verdict verdict, int score, String reason, String guidance, List<String> precautions) {
        /** Keeps the precaution list immutable and the text present. */
        public Plan {
            Objects.requireNonNull(verdict);
            Objects.requireNonNull(reason);
            Objects.requireNonNull(guidance);
            precautions = List.copyOf(precautions);
        }
    }

    private ActivityPlanner() {}

    /** Plans an activity for a verified category. */
    public static Plan plan(AirCategory category, ActivityIntensity intensity, ActivityWindow window,
                            AdviceProfile audience) {
        Objects.requireNonNull(category);
        Objects.requireNonNull(intensity);
        Objects.requireNonNull(window);
        Objects.requireNonNull(audience);
        int score = score(category, intensity, window, audience);
        Verdict verdict = verdict(category, score);
        return new Plan(verdict, score, reason(category, intensity, window, audience, score),
                audience.strategy().advice(category), precautions(category, intensity, window, verdict));
    }

    /** Returns the exposure score for a plan, exposed so the interface can explain the verdict. */
    public static int score(AirCategory category, ActivityIntensity intensity, ActivityWindow window,
                            AdviceProfile audience) {
        return category.ordinal() * 2 + intensity.weight() + window.weight() + audienceWeight(audience);
    }

    private static int audienceWeight(AdviceProfile audience) {
        return audience == AdviceProfile.GENERAL ? 0 : SENSITIVE_AUDIENCE_WEIGHT;
    }

    private static Verdict verdict(AirCategory category, int score) {
        if (category.ordinal() >= AirCategory.VERY_UNHEALTHY.ordinal()) return Verdict.RESCHEDULE;
        if (score <= SUITABLE_LIMIT) return Verdict.SUITABLE;
        if (score <= ACCEPTABLE_LIMIT) return Verdict.ACCEPTABLE;
        return score <= MODIFY_LIMIT ? Verdict.MODIFY : Verdict.RESCHEDULE;
    }

    private static String reason(AirCategory category, ActivityIntensity intensity, ActivityWindow window,
                                 AdviceProfile audience, int score) {
        return "Category " + category + " contributes " + category.ordinal() * 2 + ", " + intensity
                + " effort " + intensity.weight() + ", " + window.toString().toLowerCase(java.util.Locale.ENGLISH)
                + " " + window.weight() + ", " + audience + " " + audienceWeight(audience)
                + ". Total exposure score " + score + ".";
    }

    private static List<String> precautions(AirCategory category, ActivityIntensity intensity,
                                            ActivityWindow window, Verdict verdict) {
        var steps = new ArrayList<String>();
        if (verdict == Verdict.RESCHEDULE) steps.add("Move the activity indoors, or wait for a lower index.");
        if (verdict == Verdict.MODIFY) steps.add("Cut the time outdoors or lower the effort by one level.");
        if (intensity != ActivityIntensity.LIGHT && category.ordinal() >= AirCategory.MODERATE.ordinal()) {
            steps.add("Plan rest breaks instead of continuous hard effort.");
        }
        if (window != ActivityWindow.SHORT) steps.add("Check the index again before a long session starts.");
        if (category.ordinal() >= AirCategory.SENSITIVE.ordinal()) {
            steps.add("Prefer a route away from heavy traffic, and follow official local guidance.");
        }
        return steps;
    }
}
