package cityair.service;

import cityair.model.District;
import cityair.model.GuidelineLevel;
import cityair.model.Reading;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Turns the readings the application holds into short factual statements.
 *
 * <p>Every statement is produced by an explicit rule over data that is present: a comparison
 * against a published guideline level, a difference between two stored indices, an age computed
 * from two timestamps. Nothing here is inferred, modelled or predicted, and the engine never
 * speaks about readings it does not have. It is deliberately free of Swing so the rules can be
 * tested directly, and so the view only has to choose how to present a sentence it is handed.</p>
 */
public final class InsightEngine {
    /** How prominently a statement should be shown. */
    public enum Level {
        /** Context with no action implied. */
        NEUTRAL,
        /** A favourable observation. */
        POSITIVE,
        /** Worth noticing before planning time outdoors. */
        CAUTION,
        /** A breach of the reader's own alert threshold. */
        CRITICAL
    }

    /**
     * One statement.
     *
     * @param level how prominently to present it
     * @param title short heading
     * @param detail the statement itself, written for a resident
     */
    public record Insight(Level level, String title, String detail) {
        /** Validates that a statement carries both a heading and a body. */
        public Insight {
            Objects.requireNonNull(level);
            if (title == null || title.isBlank() || detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("An insight needs a title and a detail.");
            }
        }
    }

    private static final int MAXIMUM_INSIGHTS = 4;
    private static final double GUIDELINE_ATTENTION = 100;

    private InsightEngine() {}

    /**
     * Builds up to four statements about the selected area.
     *
     * @param selected the area on screen
     * @param latest each area's most recent verified reading, which may be empty
     * @param history the selected area's model hours from this session
     * @param threshold the reader's alert threshold
     * @param now the current time, injected so the result is testable
     */
    public static List<Insight> insights(District selected, Map<District, Reading> latest,
                                         List<Reading> history, int threshold, Instant now) {
        Objects.requireNonNull(selected);
        Reading reading = latest.get(selected);
        if (reading == null) return List.of(noData());
        var found = new ArrayList<Insight>();
        addIfPresent(found, threshold(reading, threshold));
        addIfPresent(found, trend(TrendSummary.of(history)));
        addIfPresent(found, dominantPollutant(reading));
        addIfPresent(found, comparison(selected, latest));
        addIfPresent(found, freshness(reading, now));
        return found.size() > MAXIMUM_INSIGHTS ? found.subList(0, MAXIMUM_INSIGHTS) : found;
    }

    private static Insight noData() {
        return new Insight(Level.NEUTRAL, "No verified reading",
                "Refresh this area to retrieve a modeled estimate. Observations appear here once data arrives.");
    }

    private static Insight threshold(Reading reading, int threshold) {
        int difference = reading.aqi() - threshold;
        if (difference >= 0) {
            return new Insight(Level.CRITICAL, "Above your alert threshold",
                    "U.S. AQI " + reading.aqi() + " is " + difference + " points at or above your threshold of "
                            + threshold + " for this model hour.");
        }
        if (difference > -20) {
            return new Insight(Level.CAUTION, "Close to your alert threshold",
                    "U.S. AQI " + reading.aqi() + " is " + -difference + " points below your threshold of "
                            + threshold + ".");
        }
        return new Insight(Level.POSITIVE, "Below your alert threshold",
                "U.S. AQI " + reading.aqi() + " is " + -difference + " points below your threshold of "
                        + threshold + ".");
    }

    private static Insight trend(TrendSummary summary) {
        if (summary.samples() < 2) return null;
        String window = describeSpan(summary.span());
        return switch (summary.direction()) {
            case RISING -> new Insight(Level.CAUTION, "Rising in this session",
                    "The index rose " + summary.change() + " points across " + summary.samples()
                            + " model hours" + window + ".");
            case FALLING -> new Insight(Level.POSITIVE, "Falling in this session",
                    "The index fell " + -summary.change() + " points across " + summary.samples()
                            + " model hours" + window + ".");
            default -> new Insight(Level.NEUTRAL, "Steady in this session",
                    "The index is unchanged across " + summary.samples() + " model hours" + window + ".");
        };
    }

    private static String describeSpan(Duration span) {
        long hours = span.toHours();
        if (hours <= 0) return "";
        return ", covering " + hours + (hours == 1 ? " hour" : " hours");
    }

    private static Insight dominantPollutant(Reading reading) {
        GuidelineLevel worst = null;
        double highest = 0;
        for (GuidelineLevel pollutant : GuidelineLevel.values()) {
            Double percent = pollutant.percentOfLevel(reading);
            if (percent != null && percent > highest) {
                highest = percent;
                worst = pollutant;
            }
        }
        return worst == null ? null : pollutantInsight(worst, highest);
    }

    private static Insight pollutantInsight(GuidelineLevel pollutant, double percent) {
        String text = String.format(Locale.ENGLISH, "%s is at %.0f%% of its WHO 2021 %s guideline level of %.0f "
                + "micrograms per cubic metre.", pollutant, percent, pollutant.period(), pollutant.level());
        return new Insight(percent >= GUIDELINE_ATTENTION ? Level.CAUTION : Level.NEUTRAL,
                "Leading pollutant: " + pollutant, text + " " + GuidelineLevel.CAVEAT);
    }

    private static Insight comparison(District selected, Map<District, Reading> latest) {
        District other = cleanestOther(selected, latest);
        if (other == null) return null;
        int difference = latest.get(selected).aqi() - latest.get(other).aqi();
        if (difference <= 0) return null;
        return new Insight(Level.NEUTRAL, "Cleaner nearby area",
                other + " was " + difference + " points lower than " + selected
                        + " at each area's most recent verified model hour.");
    }

    private static District cleanestOther(District selected, Map<District, Reading> latest) {
        District best = null;
        for (var entry : latest.entrySet()) {
            boolean better = best == null || entry.getValue().aqi() < latest.get(best).aqi();
            if (entry.getKey() != selected && better) best = entry.getKey();
        }
        return best;
    }

    private static Insight freshness(Reading reading, Instant now) {
        long hours = Duration.between(reading.validAt(), now).toHours();
        if (hours < 2) return null;
        return new Insight(Level.CAUTION, "Model hour is not recent",
                "The hour shown is " + hours + " hours old. Refresh to request the provider's latest estimate.");
    }

    private static void addIfPresent(List<Insight> found, Insight insight) {
        if (insight != null) found.add(insight);
    }
}
