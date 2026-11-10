package cityair;

import cityair.model.*;
import cityair.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    static final Instant AT = Instant.parse("2026-11-10T07:00:00Z");
    static Reading reading(District district, int index, int offset) {
        return new Reading(district, AT.plusSeconds(offset), AT, index, 15.0, null, 25.0, 30.0);
    }

    @ParameterizedTest @CsvSource({"0,GOOD","50,GOOD","51,MODERATE","100,MODERATE",
            "101,SENSITIVE","150,SENSITIVE","151,UNHEALTHY","200,UNHEALTHY","201,VERY_UNHEALTHY","300,VERY_UNHEALTHY","301,HAZARDOUS","550,HAZARDOUS"})
    void from_aqiBoundary_selectsCorrectCategory(int aqi, AirCategory expected) {
        assertEquals(expected, AirCategory.from(aqi));
    }
    @Test void from_negativeAqi_throws() { assertThrows(IllegalArgumentException.class, () -> AirCategory.from(-1)); }
    @Test void reading_missingPollutant_preservesUnavailable() {
        var data = reading(District.TITIWANGSA, 82, 0);
        assertAll(() -> assertNull(data.pm10()), () -> assertEquals(15.0, data.pm25()),
                () -> assertEquals(AirCategory.MODERATE, data.category()));
    }
    @Test void reading_invalidPollutant_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Reading(District.TITIWANGSA, AT, AT, 55, -1.0, null, null, null));
    }
    @Test void reading_nonFinitePollutant_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Reading(District.TITIWANGSA, AT, AT, 55, Double.NaN, null, null, null));
    }
    @Test void advice_sensitiveCategory_variesByProfile() {
        var category = AirCategory.SENSITIVE;
        assertEquals(3, java.util.Arrays.stream(AdviceProfile.values()).map(p -> p.strategy().advice(category)).distinct().count());
    }
    @ParameterizedTest @CsvSource({"GENERAL,GOOD","CHILDREN,UNHEALTHY","OLDER_ADULTS,MODERATE"})
    void advice_selectedProfile_producesGuidance(AdviceProfile profile, AirCategory category) {
        assertFalse(profile.strategy().advice(category).isBlank());
    }
    @Test void advice_nullCategory_throws() {
        assertThrows(NullPointerException.class, () -> AdviceProfile.CHILDREN.strategy().advice(null));
    }
    @ParameterizedTest @NullAndEmptySource
    void citizenReport_missingDescription_throws(String description) {
        assertThrows(IllegalArgumentException.class, () -> report(description));
    }
    @Test void citizenReport_minimumAfterTrim_savesNormalizedText() {
        assertEquals("abcdefghij", report("  abcdefghij  ").description());
    }
    @Test void citizenReport_aboveMaximum_throws() {
        assertThrows(IllegalArgumentException.class, () -> report("x".repeat(CitizenReport.MAX_DESCRIPTION + 1)));
    }
    @Test void citizenReport_exactMaximum_accepts() {
        assertEquals(CitizenReport.MAX_DESCRIPTION, report("x".repeat(CitizenReport.MAX_DESCRIPTION)).description().length());
    }
    @Test void nearby_eachPilotDistrict_hasVerifiedSource() {
        for (var district : District.values()) {
            assertEquals("https", CityCatalog.nearby(district).get(0).source().getScheme());
        }
    }

    static CitizenReport report(String description) {
        return new CitizenReport(UUID.randomUUID(), District.TITIWANGSA, CitizenReport.Issue.SMOKE, description, AT);
    }
}
