package cityair.model;

/** U.S. AQI categories for the Open-Meteo us_aqi field. */
public enum AirCategory {
    GOOD("Good", 50),
    MODERATE("Moderate", 100),
    SENSITIVE("Unhealthy for sensitive groups", 150),
    UNHEALTHY("Unhealthy", 200),
    VERY_UNHEALTHY("Very unhealthy", 300),
    HAZARDOUS("Hazardous", Integer.MAX_VALUE);

    private final String label;
    private final int upperBound;

    AirCategory(String label, int upperBound) {
        this.label = label;
        this.upperBound = upperBound;
    }

    /** Classifies a non-negative U.S. AQI, including values above 500. */
    public static AirCategory from(int index) {
        if (index < 0) throw new IllegalArgumentException("AQI must be non-negative.");
        for (AirCategory category : values()) {
            if (index <= category.upperBound) return category;
        }
        throw new AssertionError("AQI has no category");
    }

    /** Returns the display label. */
    @Override public String toString() { return label; }
}
