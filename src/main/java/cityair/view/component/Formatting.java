package cityair.view.component;

/**
 * The words and symbols the interface uses for information it does not have.
 *
 * <p>They are defined once so that a missing pollutant reads the same in a tile, a table and a
 * chart, and so the distinction between "the provider omitted this value" and "this row has no
 * value to show" is never blurred by two different phrasings.</p>
 */
public final class Formatting {
    /** Shown where the provider returned no value for a field it can supply. */
    public static final String ABSENT = "Unavailable";
    /** Shown in a table cell that has nothing to display, such as a district never refreshed. */
    public static final String DASH = "—";
    /** Shown before any request has been made in this session. */
    public static final String NOT_LOADED = "Not loaded";

    private Formatting() {}
}
