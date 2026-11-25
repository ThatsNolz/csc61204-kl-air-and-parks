package cityair.model;

/**
 * How hard the planned activity is. Exertion matters because a person breathing harder draws more
 * air, and therefore more of whatever the air contains, in the same length of time.
 */
public enum ActivityIntensity {
    /** Walking, sitting in a park, light gardening. */
    LIGHT("Light", 0),
    /** Brisk walking, cycling at an easy pace, casual sport. */
    MODERATE("Moderate", 1),
    /** Running, competitive sport, sustained hard effort. */
    VIGOROUS("Vigorous", 2);

    private final String label;
    private final int weight;

    ActivityIntensity(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    /** Returns the exposure weight this intensity contributes to a plan. */
    public int weight() { return weight; }

    /** Returns the display label. */
    @Override public String toString() { return label; }
}
