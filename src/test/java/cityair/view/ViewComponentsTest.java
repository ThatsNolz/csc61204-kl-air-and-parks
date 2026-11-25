package cityair.view;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.AirCategory;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.strategy.AdviceProfile;
import cityair.view.chart.AqiDial;
import cityair.view.chart.AreaPlot;
import cityair.view.chart.BandScale;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import java.awt.Color;
import java.util.UUID;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Package-level tests for the view helpers that hold logic: scales, palette and saved settings. */
class ViewComponentsTest {
    private final Preferences store = Preferences.userRoot().node("csc61204-test/view/" + UUID.randomUUID());

    @AfterEach void clean() throws Exception {
        store.removeNode();
        Skin.setTheme(AuroraTheme.DARK);
        Skin.setReducedMotion(false);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "50, 0.1666", "51, 0.1666", "100, 0.3333", "150, 0.5", "300, 0.8333", "500, 1.0", "800, 1.0"})
    void position_categoryBoundaries_mapToEqualSegments(int aqi, double expected) {
        assertEquals(expected, BandScale.position(aqi), 0.001);
    }

    @ParameterizedTest @EnumSource(AirCategory.class)
    void fill_everyCategory_hasColourAndReadableTextInBothThemes(AirCategory category) {
        Skin.setTheme(AuroraTheme.LIGHT);
        Color light = CategoryPalette.fill(category);
        Skin.setTheme(AuroraTheme.DARK);
        assertAll(() -> assertNotNull(light),
                () -> assertNotNull(CategoryPalette.fill(category)),
                () -> assertNotEquals(CategoryPalette.fill(category), CategoryPalette.onFill(category)));
    }

    @ParameterizedTest @EnumSource(Tone.class)
    void color_everyRole_isDefinedByBothThemes(Tone tone) {
        assertAll(() -> assertNotNull(AuroraTheme.DARK.color(tone)),
                () -> assertNotNull(AuroraTheme.LIGHT.color(tone)));
    }

    @Test void dial_sharesTheScaleModel_withTheBandScale() {
        assertAll(() -> assertEquals(BandScale.position(101), AqiDial.fraction(101), 1e-9),
                () -> assertEquals(0.0, AqiDial.fraction(-5), 1e-9),
                () -> assertEquals(1.0, AqiDial.fraction(900), 1e-9));
    }

    @Test void distanceKm_pilotAreas_matchTheirReferenceCoordinates() {
        double titiwangsaToCentre = AreaPlot.distanceKm(District.TITIWANGSA, District.CITY_CENTRE);
        assertAll(() -> assertEquals(0, AreaPlot.distanceKm(District.PERDANA, District.PERDANA), 1e-9),
                () -> assertTrue(titiwangsaToCentre > 2 && titiwangsaToCentre < 4,
                        "about 2.4 km apart, was " + titiwangsaToCentre),
                () -> assertEquals(titiwangsaToCentre,
                        AreaPlot.distanceKm(District.CITY_CENTRE, District.TITIWANGSA), 1e-9));
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

    @Test void appearance_unknownSavedTheme_fallsBackToAuroraDark() {
        store.put("appearance.theme", "SOLARIZED");
        store.put("plan.intensity", "EXTREME");
        var settings = new ViewPreferences(store);
        assertAll(() -> assertEquals(AuroraTheme.DARK, settings.theme()),
                () -> assertEquals(ActivityIntensity.MODERATE, settings.intensity()),
                () -> assertEquals(ActivityWindow.MEDIUM, settings.window()),
                () -> assertFalse(settings.reducedMotion()));
    }

    @Test void saveThreshold_validValue_isReturnedNextTime() {
        new ViewPreferences(store).saveThreshold(150);
        assertEquals(150, new ViewPreferences(store).threshold());
    }

    @Test void saveAppearance_everySetting_survivesANewInstance() {
        var settings = new ViewPreferences(store);
        settings.saveTheme(AuroraTheme.LIGHT);
        settings.saveReducedMotion(true);
        settings.saveRailCollapsed(true);
        settings.saveIntensity(ActivityIntensity.VIGOROUS);
        settings.saveWindow(ActivityWindow.LONG);
        var reopened = new ViewPreferences(store);
        assertAll(() -> assertEquals(AuroraTheme.LIGHT, reopened.theme()),
                () -> assertTrue(reopened.reducedMotion()),
                () -> assertTrue(reopened.railCollapsed()),
                () -> assertEquals(ActivityIntensity.VIGOROUS, reopened.intensity()),
                () -> assertEquals(ActivityWindow.LONG, reopened.window()));
    }

    @Test void reducedMotion_whenRequested_removesEveryAnimationDuration() {
        Skin.setReducedMotion(true);
        assertAll(() -> assertEquals(0, Skin.duration(600)),
                () -> assertTrue(Skin.reducedMotion()));
        Skin.setReducedMotion(false);
        assertEquals(600, Skin.duration(600));
    }
}
