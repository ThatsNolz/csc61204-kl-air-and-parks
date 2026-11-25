package cityair.service;

import cityair.model.Reading;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Summary statistics for the model hours collected in one session.
 *
 * <p>This is arithmetic over the readings the application actually holds, so it belongs beside the
 * reading service rather than inside a chart. Keeping it here means the figures under the chart,
 * the sentence in the insights panel and the values in the table are all computed once, from the
 * same list, and can be unit tested without a user interface.</p>
 *
 * @param samples number of distinct model hours summarised
 * @param lowest lowest index in the window
 * @param highest highest index in the window
 * @param average mean index, rounded to one decimal place
 * @param change difference between the newest and the oldest index
 * @param span time covered, from the oldest model hour to the newest
 * @param latest the newest reading in the window
 */
public record TrendSummary(int samples, int lowest, int highest, double average, int change,
                           Duration span, Reading latest) {
    /** A window with no readings in it. */
    public static final TrendSummary EMPTY = new TrendSummary(0, 0, 0, 0, 0, Duration.ZERO, null);
    private static final double ROUNDING = 10d;

    /** Direction of travel across the window. */
    public enum Direction {
        /** The newest index is higher than the oldest, so conditions worsened. */
        RISING,
        /** The newest index is lower than the oldest, so conditions improved. */
        FALLING,
        /** The newest and oldest indices are equal. */
        STEADY,
        /** Fewer than two model hours, so no direction can be claimed. */
        UNKNOWN
    }

    /** Summarises a list of readings in any order; an empty list returns {@link #EMPTY}. */
    public static TrendSummary of(List<Reading> readings) {
        Objects.requireNonNull(readings);
        if (readings.isEmpty()) return EMPTY;
        List<Reading> ordered = readings.stream().sorted(Comparator.comparing(Reading::validAt)).toList();
        Reading first = ordered.get(0);
        Reading last = ordered.get(ordered.size() - 1);
        OptionalDouble mean = ordered.stream().mapToInt(Reading::aqi).average();
        return new TrendSummary(ordered.size(),
                ordered.stream().mapToInt(Reading::aqi).min().orElse(0),
                ordered.stream().mapToInt(Reading::aqi).max().orElse(0),
                Math.round(mean.orElse(0) * ROUNDING) / ROUNDING,
                last.aqi() - first.aqi(),
                Duration.between(first.validAt(), last.validAt()), last);
    }

    /** Returns the direction of travel, which is unknown until two model hours exist. */
    public Direction direction() {
        if (samples < 2) return Direction.UNKNOWN;
        if (change > 0) return Direction.RISING;
        return change < 0 ? Direction.FALLING : Direction.STEADY;
    }

    /** Returns true when at least one model hour has been collected. */
    public boolean hasData() { return samples > 0; }

    /** Returns the model hour of the newest reading, or null when the window is empty. */
    public Instant newestHour() { return latest == null ? null : latest.validAt(); }

    /** Returns the indices in the window, oldest first, for a sparkline. */
    public static List<Integer> indices(List<Reading> readings) {
        return readings.stream().sorted(Comparator.comparing(Reading::validAt)).map(Reading::aqi).toList();
    }
}
