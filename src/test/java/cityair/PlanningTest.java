package cityair;

import cityair.model.ActivityIntensity;
import cityair.model.ActivityWindow;
import cityair.model.AirCategory;
import cityair.model.District;
import cityair.model.GuidelineLevel;
import cityair.model.Reading;
import cityair.service.ActivityPlanner;
import cityair.service.ActivityPlanner.Verdict;
import cityair.service.InsightEngine;
import cityair.service.TrendSummary;
import cityair.strategy.AdviceProfile;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** The rules behind the planner, the insights and the session statistics. */
class PlanningTest {
    private static final Instant NOW = Instant.parse("2026-11-25T08:00:00Z");

    /** A reading for a model hour a whole number of hours before {@link #NOW}. */
    private static Reading hour(District area, int aqi, int hoursAgo) {
        return new Reading(area, NOW.minus(Duration.ofHours(hoursAgo)), NOW, aqi, 15.0, null, 25.0, 30.0);
    }

    @ParameterizedTest
    @CsvSource({"GOOD, LIGHT, SHORT, GENERAL, SUITABLE",
                "MODERATE, LIGHT, SHORT, GENERAL, SUITABLE",
                "MODERATE, VIGOROUS, MEDIUM, CHILDREN, MODIFY",
                "SENSITIVE, LIGHT, SHORT, GENERAL, ACCEPTABLE",
                "SENSITIVE, VIGOROUS, LONG, CHILDREN, RESCHEDULE",
                "UNHEALTHY, LIGHT, SHORT, GENERAL, MODIFY",
                "VERY_UNHEALTHY, LIGHT, SHORT, GENERAL, RESCHEDULE",
                "HAZARDOUS, LIGHT, SHORT, OLDER_ADULTS, RESCHEDULE"})
    void plan_publishedRuleTable_producesTheStatedVerdict(AirCategory category, ActivityIntensity intensity,
                                                          ActivityWindow window, AdviceProfile audience,
                                                          Verdict expected) {
        assertEquals(expected, ActivityPlanner.plan(category, intensity, window, audience).verdict());
    }

    @ParameterizedTest @EnumSource(AirCategory.class)
    void plan_everyCategory_explainsItselfAndQuotesTheAudienceStrategy(AirCategory category) {
        var plan = ActivityPlanner.plan(category, ActivityIntensity.MODERATE, ActivityWindow.MEDIUM,
                AdviceProfile.CHILDREN);
        assertAll(() -> assertTrue(plan.reason().contains("exposure score"), plan.reason()),
                () -> assertEquals(AdviceProfile.CHILDREN.strategy().advice(category), plan.guidance()),
                () -> assertTrue(plan.score() >= 0),
                () -> assertNotNull(plan.verdict()));
    }

    @Test void plan_harderOrLongerActivity_neverLowersTheExposureScore() {
        int light = ActivityPlanner.score(AirCategory.MODERATE, ActivityIntensity.LIGHT, ActivityWindow.SHORT,
                AdviceProfile.GENERAL);
        int harder = ActivityPlanner.score(AirCategory.MODERATE, ActivityIntensity.VIGOROUS, ActivityWindow.SHORT,
                AdviceProfile.GENERAL);
        int longer = ActivityPlanner.score(AirCategory.MODERATE, ActivityIntensity.LIGHT, ActivityWindow.LONG,
                AdviceProfile.GENERAL);
        int sensitive = ActivityPlanner.score(AirCategory.MODERATE, ActivityIntensity.LIGHT, ActivityWindow.SHORT,
                AdviceProfile.OLDER_ADULTS);
        assertAll(() -> assertTrue(harder > light), () -> assertTrue(longer > light),
                () -> assertTrue(sensitive > light));
    }

    @Test void plan_precautions_areImmutableAndMatchTheVerdict() {
        var plan = ActivityPlanner.plan(AirCategory.HAZARDOUS, ActivityIntensity.VIGOROUS, ActivityWindow.LONG,
                AdviceProfile.CHILDREN);
        assertAll(() -> assertFalse(plan.precautions().isEmpty()),
                () -> assertTrue(plan.precautions().get(0).contains("indoors")),
                () -> assertThrows(UnsupportedOperationException.class, () -> plan.precautions().add("x")),
                () -> assertThrows(NullPointerException.class,
                        () -> ActivityPlanner.plan(null, ActivityIntensity.LIGHT, ActivityWindow.SHORT,
                                AdviceProfile.GENERAL)));
    }

    @Test void summary_sessionHours_reportRangeAverageAndDirection() {
        var samples = List.of(hour(District.TITIWANGSA, 80, 3), hour(District.TITIWANGSA, 120, 1),
                hour(District.TITIWANGSA, 100, 2));
        var summary = TrendSummary.of(samples);
        assertAll(() -> assertEquals(3, summary.samples()),
                () -> assertEquals(80, summary.lowest()),
                () -> assertEquals(120, summary.highest()),
                () -> assertEquals(100.0, summary.average(), 0.01),
                () -> assertEquals(40, summary.change()),
                () -> assertEquals(TrendSummary.Direction.RISING, summary.direction()),
                () -> assertEquals(Duration.ofHours(2), summary.span()));
    }

    @Test void summary_oneOrNoSamples_claimsNoDirection() {
        assertAll(() -> assertEquals(TrendSummary.Direction.UNKNOWN, TrendSummary.EMPTY.direction()),
                () -> assertFalse(TrendSummary.EMPTY.hasData()),
                () -> assertNull(TrendSummary.EMPTY.newestHour()),
                () -> assertEquals(TrendSummary.Direction.UNKNOWN,
                        TrendSummary.of(List.of(hour(District.PERDANA, 60, 0))).direction()));
    }

    @Test void summary_indices_areOrderedOldestFirst() {
        var samples = List.of(hour(District.PERDANA, 70, 1), hour(District.PERDANA, 50, 3));
        assertEquals(List.of(50, 70), TrendSummary.indices(samples));
    }

    @Test void insights_noReading_asksForARefreshAndClaimsNothingElse() {
        var found = InsightEngine.insights(District.PERDANA, Map.of(), List.of(), 101, NOW);
        assertAll(() -> assertEquals(1, found.size()),
                () -> assertEquals(InsightEngine.Level.NEUTRAL, found.get(0).level()),
                () -> assertTrue(found.get(0).detail().contains("Refresh")));
    }

    @Test void insights_aboveThreshold_leadWithTheCrossingAndNameThePollutant() {
        var sample = new Reading(District.TITIWANGSA, NOW, NOW, 140, 30.0, 40.0, 20.0, 50.0);
        var found = InsightEngine.insights(District.TITIWANGSA, Map.of(District.TITIWANGSA, sample),
                List.of(sample), 101, NOW);
        assertAll(() -> assertEquals(InsightEngine.Level.CRITICAL, found.get(0).level()),
                () -> assertTrue(found.get(0).detail().contains("101")),
                () -> assertTrue(found.stream().anyMatch(i -> i.title().contains("PM2.5"))),
                () -> assertTrue(found.size() <= 4));
    }

    @Test void insights_cleanerNeighbour_isReportedWithItsDifference() {
        var here = new Reading(District.TITIWANGSA, NOW, NOW, 120, 20.0, null, null, null);
        var there = new Reading(District.PERDANA, NOW, NOW, 60, 10.0, null, null, null);
        var found = InsightEngine.insights(District.TITIWANGSA,
                Map.of(District.TITIWANGSA, here, District.PERDANA, there), List.of(here), 101, NOW);
        assertTrue(found.stream().anyMatch(i -> i.detail().contains("60 points lower")),
                found.toString());
    }

    @Test void insights_staleModelHour_isReportedAsOld() {
        var old = new Reading(District.PERDANA, NOW.minus(Duration.ofHours(5)), NOW, 40, 5.0, null, null, null);
        var found = InsightEngine.insights(District.PERDANA, Map.of(District.PERDANA, old), List.of(old), 101, NOW);
        assertTrue(found.stream().anyMatch(i -> i.detail().contains("5 hours old")), found.toString());
    }

    @Test void insight_withoutText_isRejected() {
        assertAll(() -> assertThrows(IllegalArgumentException.class,
                        () -> new InsightEngine.Insight(InsightEngine.Level.NEUTRAL, " ", "detail")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InsightEngine.Insight(InsightEngine.Level.NEUTRAL, "title", null)));
    }

    @ParameterizedTest @EnumSource(GuidelineLevel.class)
    void guideline_everyPollutant_statesItsLevelPeriodAndMeasurement(GuidelineLevel pollutant) {
        var sample = new Reading(District.PERDANA, NOW, NOW, 50, 15.0, 45.0, 25.0, 100.0);
        assertAll(() -> assertTrue(pollutant.level() > 0),
                () -> assertFalse(pollutant.period().isBlank()),
                () -> assertEquals(100.0, pollutant.percentOfLevel(sample), 0.001),
                () -> assertEquals(pollutant.level(), pollutant.measure(sample), 0.001));
    }

    @Test void guideline_missingPollutant_reportsNoPercentage() {
        var sample = new Reading(District.PERDANA, NOW, NOW, 50, null, null, null, null);
        assertAll(() -> assertNull(GuidelineLevel.PM25.percentOfLevel(sample)),
                () -> assertNull(GuidelineLevel.PM25.measure(sample)),
                () -> assertThrows(NullPointerException.class, () -> GuidelineLevel.PM25.measure(null)),
                () -> assertTrue(GuidelineLevel.CAVEAT.contains("not a compliance assessment")));
    }
}
