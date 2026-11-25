package cityair.view;

import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.strategy.AdviceProfile;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Typed access to the user's saved choices. Invalid or tampered values fall back to safe defaults,
 * so a corrupted preference can never stop the application from starting.
 */
final class ViewPreferences {
    private static final String DISTRICT = "district", PROFILE = "profile", THRESHOLD = "alert.threshold", ALERT = "alert.";
    private final Preferences store;

    ViewPreferences(Preferences store) { this.store = Objects.requireNonNull(store); }

    District district() { return saved(District.class, DISTRICT, District.TITIWANGSA); }

    AdviceProfile profile() { return saved(AdviceProfile.class, PROFILE, AdviceProfile.GENERAL); }

    boolean subscribed(District item) { return store.getBoolean(ALERT + item.name(), false); }

    /** Saved alert threshold, or the default when the stored value is outside the allowed range. */
    int threshold() {
        int value = store.getInt(THRESHOLD, AlertMonitor.DEFAULT_THRESHOLD);
        boolean valid = value >= AlertMonitor.MINIMUM_THRESHOLD && value <= AlertMonitor.MAXIMUM_THRESHOLD;
        return valid ? value : AlertMonitor.DEFAULT_THRESHOLD;
    }

    void saveDistrict(District value) { store.put(DISTRICT, value.name()); }

    void saveProfile(AdviceProfile value) { store.put(PROFILE, value.name()); }

    void saveSubscription(District item, boolean enabled) { store.putBoolean(ALERT + item.name(), enabled); }

    void saveThreshold(int value) { store.putInt(THRESHOLD, value); }

    private <E extends Enum<E>> E saved(Class<E> type, String key, E fallback) {
        try {
            return Enum.valueOf(type, store.get(key, fallback.name()));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
