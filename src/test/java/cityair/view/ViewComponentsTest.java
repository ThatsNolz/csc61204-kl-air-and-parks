package cityair.view;

import cityair.model.AirCategory;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.strategy.AdviceProfile;
import java.util.UUID;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Package-level tests for view helpers that hold logic: the scale, palette and saved settings. */
class ViewComponentsTest {
    private final Preferences store = Preferences.userRoot().node("csc61204-test/view/" + UUID.randomUUID());

    @AfterEach void clean() throws Exception { store.removeNode(); }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "50, 0.1666", "51, 0.1666", "100, 0.3333", "150, 0.5", "300, 0.8333", "500, 1.0", "800, 1.0"})
    void position_categoryBoundaries_mapToEqualSegments(int aqi, double expected) {
        assertEquals(expected, AqiScale.position(aqi), 0.001);
    }

    @ParameterizedTest @EnumSource(AirCategory.class)
    void fill_everyCategory_hasColourAndReadableText(AirCategory category) {
        assertAll(() -> assertNotNull(CategoryPalette.fill(category)),
                () -> assertNotEquals(CategoryPalette.fill(category), CategoryPalette.onFill(category)));
    }

    @Test void threshold_tamperedValue_fallsBackToDefault() {
        store.putInt("alert.threshold", 9000);
        assertEquals(AlertMonitor.DEFAULT_THRESHOLD, new ViewPreferences(store).threshold());
    }

    @Test void district_unknownSavedName_fallsBackToTitiwangsa() {
        store.put("district", "ATLANTIS");
        var settings = new ViewPreferences(store);
        assertAll(() -> assertEquals(District.TITIWANGSA, settings.district()),
                () -> assertEquals(AdviceProfile.GENERAL, settings.profile()));
    }

    @Test void saveThreshold_validValue_isReturnedNextTime() {
        new ViewPreferences(store).saveThreshold(150);
        assertEquals(150, new ViewPreferences(store).threshold());
    }
}
