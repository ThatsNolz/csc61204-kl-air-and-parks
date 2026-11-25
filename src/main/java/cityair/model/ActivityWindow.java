package cityair.model;

import java.time.Duration;

/**
 * How long the planned activity lasts. Time outdoors is the second half of exposure: the same air
 * matters more over two hours than over twenty minutes.
 */
public enum ActivityWindow {
    /** Up to half an hour outdoors. */
    SHORT("Up to 30 min", Duration.ofMinutes(30), 0),
    /** About an hour outdoors. */
    MEDIUM("About 1 hour", Duration.ofHours(1), 1),
    /** Two hours or more outdoors. */
    LONG("2 hours or more", Duration.ofHours(2), 2);

    private final String label;
    private final Duration duration;
    private final int weight;

    ActivityWindow(String label, Duration duration, int weight) {
        this.label = label;
        this.duration = duration;
        this.weight = weight;
    }

    /** Returns the representative length of this window. */
    public Duration duration() { return duration; }

    /** Returns the exposure weight this window contributes to a plan. */
    public int weight() { return weight; }

    /** Returns the display label. */
    @Override public String toString() { return label; }
}
