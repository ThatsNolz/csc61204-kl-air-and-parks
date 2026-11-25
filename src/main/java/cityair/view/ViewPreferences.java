package cityair.view;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.strategy.AdviceProfile;
import cityair.view.design.AuroraTheme;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Typed access to the user's saved choices, including appearance.
 *
 * <p>Every value is validated on the way out. A preference store can be edited by hand or left
 * behind by an older version, so an unknown name or an out-of-range number falls back to a safe
 * default rather than preventing the application from starting.</p>
 */
public final class ViewPreferences {
    private static final String DISTRICT = "district", PROFILE = "profile", THRESHOLD = "alert.threshold";
    private static final String ALERT = "alert.", THEME = "appearance.theme", MOTION = "appearance.reducedMotion";
    private static final String RAIL = "appearance.railCollapsed", INTENSITY = "plan.intensity", WINDOW = "plan.window";
    private final Preferences store;

    /** Creates typed access over a preference node. */
    public ViewPreferences(Preferences store) { this.store = Objects.requireNonNull(store); }

    /** The pilot area shown at startup. */
    public District district() { return saved(District.class, DISTRICT, District.TITIWANGSA); }

    /** The audience whose advice strategy is selected. */
    public AdviceProfile profile() { return saved(AdviceProfile.class, PROFILE, AdviceProfile.GENERAL); }

    /** The saved theme. */
    public AuroraTheme theme() { return saved(AuroraTheme.class, THEME, AuroraTheme.DARK); }

    /** Whether extended motion is turned off. */
    public boolean reducedMotion() { return store.getBoolean(MOTION, false); }

    /** Whether the navigation rail was left collapsed. */
    public boolean railCollapsed() { return store.getBoolean(RAIL, false); }

    /** The saved activity intensity for the planner. */
    public ActivityIntensity intensity() {
        return saved(ActivityIntensity.class, INTENSITY, ActivityIntensity.MODERATE);
    }

    /** The saved activity length for the planner. */
    public ActivityWindow window() { return saved(ActivityWindow.class, WINDOW, ActivityWindow.MEDIUM); }

    /** Whether alerts are enabled for one area. */
    public boolean subscribed(District item) { return store.getBoolean(ALERT + item.name(), false); }

    /** Saved alert threshold, or the default when the stored value is outside the allowed range. */
    public int threshold() {
        int value = store.getInt(THRESHOLD, AlertMonitor.DEFAULT_THRESHOLD);
        boolean valid = value >= AlertMonitor.MINIMUM_THRESHOLD && value <= AlertMonitor.MAXIMUM_THRESHOLD;
        return valid ? value : AlertMonitor.DEFAULT_THRESHOLD;
    }

    /** Stores the selected pilot area. */
    public void saveDistrict(District value) { store.put(DISTRICT, value.name()); }

    /** Stores the selected audience. */
    public void saveProfile(AdviceProfile value) { store.put(PROFILE, value.name()); }

    /** Stores the selected theme. */
    public void saveTheme(AuroraTheme value) { store.put(THEME, value.name()); }

    /** Stores the reduced-motion choice. */
    public void saveReducedMotion(boolean value) { store.putBoolean(MOTION, value); }

    /** Stores whether the navigation rail is collapsed. */
    public void saveRailCollapsed(boolean value) { store.putBoolean(RAIL, value); }

    /** Stores the planner's activity intensity. */
    public void saveIntensity(ActivityIntensity value) { store.put(INTENSITY, value.name()); }

    /** Stores the planner's activity length. */
    public void saveWindow(ActivityWindow value) { store.put(WINDOW, value.name()); }

    /** Stores whether one area delivers alerts. */
    public void saveSubscription(District item, boolean enabled) { store.putBoolean(ALERT + item.name(), enabled); }

    /** Stores the alert threshold. */
    public void saveThreshold(int value) { store.putInt(THRESHOLD, value); }

    private <E extends Enum<E>> E saved(Class<E> type, String key, E fallback) {
        try {
            return Enum.valueOf(type, store.get(key, fallback.name()));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
