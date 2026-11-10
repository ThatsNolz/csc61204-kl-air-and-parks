package cityair;

import cityair.model.CitizenReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static cityair.DomainTest.report;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("limitation")
class DocumentedLimitationTest {
    @Test void citizenReport_maximumUnicodeCodePoints_shouldAcceptSupplementaryCharacters() {
        // Known limitation: production currently counts UTF-16 units, rather than Unicode code points.
        assertDoesNotThrow(() -> report("🌳".repeat(CitizenReport.MAX_DESCRIPTION)));
    }
}
