package cityair.view;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import cityair.strategy.AdviceProfile;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything the screens need in order to draw themselves, gathered once per update.
 *
 * <p>The shell builds one of these and hands the same instance to every screen. Passing a single
 * immutable snapshot, rather than letting each screen reach back into the shell for pieces of
 * state, is what keeps the screens free of logic: a screen can only render what it was given, so
 * two screens can never disagree about which model hour is on display.</p>
 *
 * @param district the pilot area on screen
 * @param result the selected area's latest refresh outcome, or null before any refresh
 * @param source the long provenance label for that outcome
 * @param profile the selected audience
 * @param intensity the planner's selected activity intensity
 * @param window the planner's selected activity length
 * @param threshold the alert threshold in U.S. AQI
 * @param history the selected area's distinct model hours from this session, oldest first
 * @param results every area's latest refresh outcome
 * @param now the time the snapshot was taken
 * @param fixture true when readings come from a controlled test fixture rather than the provider
 */
public record ViewState(District district, Result result, String source, AdviceProfile profile,
                        ActivityIntensity intensity, ActivityWindow window, int threshold,
                        List<Reading> history, Map<District, Result> results, Instant now, boolean fixture) {

    /** Copies the collections so a screen cannot alter the shell's state. */
    public ViewState {
        Objects.requireNonNull(district);
        Objects.requireNonNull(profile);
        history = List.copyOf(history);
        results = Map.copyOf(results);
        Objects.requireNonNull(now);
    }

    /** The selected area's reading, or null when there is no verified data to show. */
    public Reading reading() { return result == null ? null : result.reading(); }

    /** True when the selected area has a verified reading. */
    public boolean hasReading() { return reading() != null; }

    /** Each area's latest verified reading, excluding areas with no data. */
    public Map<District, Reading> readings() {
        var found = new EnumMap<District, Reading>(District.class);
        results.forEach((area, outcome) -> {
            if (outcome != null && outcome.reading() != null) found.put(area, outcome.reading());
        });
        return found;
    }

    /** Each area's latest verified index, for the comparison chart and the area plot. */
    public Map<District, Integer> indices() {
        var found = new EnumMap<District, Integer>(District.class);
        readings().forEach((area, reading) -> found.put(area, reading.aqi()));
        return found;
    }
}
