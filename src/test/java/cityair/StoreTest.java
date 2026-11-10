package cityair;

import cityair.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static cityair.DomainTest.*;
import static cityair.model.District.*;
import static org.junit.jupiter.api.Assertions.*;

class StoreTest {
    @TempDir Path folder;

    @Test void list_newStore_returnsEmptyHistory() throws Exception {
        assertTrue(new ReportStore(folder).list().isEmpty());
    }
    @Test void save_twoReports_preservesNewestFirstAcrossRestart() throws Exception {
        var first = report("Smoke observed near park entrance.");
        var second = report("Strong odour beside the footpath.");
        var store = new ReportStore(folder);
        store.save(first);
        store.save(second);
        assertEquals(java.util.List.of(second, first), new ReportStore(folder).list());
    }
    @Test void save_corruptedReportFile_leavesExistingBytes() throws Exception {
        var file = Files.writeString(folder.resolve("reports.json"), "not valid");
        assertThrows(IOException.class, () -> new ReportStore(folder).save(report("A new citizen report.")));
        assertEquals("not valid", Files.readString(file));
    }
    @Test void load_missingDistrictCache_returnsEmpty() throws Exception {
        assertTrue(new ReadingCache(folder).load(PERDANA).isEmpty());
    }
    @Test void save_verifiedReading_roundTripsWithNullAndTimes() throws Exception {
        var expected = reading(TITIWANGSA, 120, 0);
        new ReadingCache(folder).save(expected);
        assertEquals(expected, new ReadingCache(folder).load(TITIWANGSA).orElseThrow());
    }
    @Test void load_misfiledReading_rejectsIt() throws Exception {
        new ReadingCache(folder).save(reading(TITIWANGSA, 120, 0));
        Files.move(folder.resolve("TITIWANGSA.json"), folder.resolve("PERDANA.json"));
        assertThrows(IOException.class, () -> new ReadingCache(folder).load(PERDANA));
    }
}
