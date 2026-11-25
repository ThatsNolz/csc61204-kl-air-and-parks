package cityair.model;

import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

/**
 * Published World Health Organization air-quality guideline levels, used to give a concentration
 * a reference point instead of leaving the reader to judge a bare number.
 *
 * <p>The figures are the 2021 WHO global air quality guidelines. Three of them are 24-hour levels
 * and the ozone figure is an 8-hour level, which is stated on each constant and shown in the
 * interface, because the averaging period is part of the value. The application's own readings are
 * hourly modeled estimates for a single hour, so a comparison here is a sense of scale and not an
 * assessment of compliance; {@link #CAVEAT} states that wherever the comparison is shown.</p>
 */
public enum GuidelineLevel {
    /** Fine particulate matter, WHO 2021 24-hour guideline level. */
    PM25("PM2.5", 15, "24-hour", Reading::pm25),
    /** Coarse particulate matter, WHO 2021 24-hour guideline level. */
    PM10("PM10", 45, "24-hour", Reading::pm10),
    /** Nitrogen dioxide, WHO 2021 24-hour guideline level. */
    NITROGEN_DIOXIDE("NO₂", 25, "24-hour", Reading::nitrogenDioxide),
    /** Ozone, WHO 2021 8-hour guideline level. */
    OZONE("O₃", 100, "8-hour", Reading::ozone);

    /** Stated wherever a guideline comparison appears in the interface. */
    public static final String CAVEAT =
            "WHO 2021 guideline levels are averages over longer periods than this hourly estimate, "
                    + "so the comparison indicates scale only and is not a compliance assessment.";
    /** The published source of every level in this enumeration. */
    public static final URI SOURCE = URI.create("https://www.who.int/publications/i/item/9789240034228");
    private static final double PERCENT = 100;

    private final String label;
    private final double level;
    private final String period;
    private final transient Function<Reading, Double> measurement;

    GuidelineLevel(String label, double level, String period, Function<Reading, Double> measurement) {
        this.label = label;
        this.level = level;
        this.period = period;
        this.measurement = measurement;
    }

    /** Returns the guideline level in micrograms per cubic metre. */
    public double level() { return level; }

    /** Returns the averaging period the level is defined over, such as "24-hour". */
    public String period() { return period; }

    /** Returns the pollutant's short display name. */
    @Override public String toString() { return label; }

    /** Returns this pollutant's concentration in a reading, or null when the provider omitted it. */
    public Double measure(Reading reading) {
        return measurement.apply(Objects.requireNonNull(reading));
    }

    /**
     * Returns the concentration as a percentage of the guideline level, or null when the
     * provider did not supply the pollutant.
     */
    public Double percentOfLevel(Reading reading) {
        Double value = measure(reading);
        return value == null ? null : value / level * PERCENT;
    }
}
