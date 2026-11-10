package cityair.model;

/** Pilot locations represented by approximate reference coordinates, not station locations. */
public enum District {
    TITIWANGSA("Titiwangsa", 3.177, 101.706),
    CITY_CENTRE("City Centre", 3.1556, 101.7150),
    PERDANA("Perdana / Tasik Perdana", 3.1453, 101.6887);

    private final String label;
    private final double latitude;
    private final double longitude;

    District(String label, double latitude, double longitude) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Returns the approximate latitude of the pilot location. */
    public double latitude() { return latitude; }

    /** Returns the approximate longitude of the pilot location. */
    public double longitude() { return longitude; }

    /** Returns the human-readable pilot district name. */
    @Override public String toString() { return label; }
}
