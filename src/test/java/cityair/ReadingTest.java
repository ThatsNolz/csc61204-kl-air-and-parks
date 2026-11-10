package cityair;

import cityair.api.ApiFailure;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static cityair.DomainTest.reading;
import static cityair.model.District.*;
import static cityair.service.ReadingService.State.*;
import static org.junit.jupiter.api.Assertions.*;

class ReadingTest {
    @TempDir Path folder;
    private final AlertMonitor monitor = new AlertMonitor(101);

    @Test void refresh_liveReading_persistsAndUpdatesHistory() throws Exception {
        var service = new ReadingService(d -> reading(d, 101, 0), new ReadingCache(folder), monitor);
        var result = service.refresh(TITIWANGSA);
        var stored = new ReadingCache(folder).load(TITIWANGSA);
        assertAll(() -> assertEquals(LIVE, result.state()), () -> assertEquals(result.reading(), stored.orElseThrow()),
                () -> assertEquals(1, service.history(TITIWANGSA).size()), () -> assertEquals(1, monitor.history().size()));
    }
    @Test void refresh_failureWithCache_returnsTimestampedCachedReading() throws Exception {
        var cache = new ReadingCache(folder);
        var expected = reading(TITIWANGSA, 115, 0);
        cache.save(expected);
        var service = new ReadingService(d -> { throw offline(); }, cache, monitor);
        var result = service.refresh(TITIWANGSA);
        assertAll(() -> assertEquals(CACHED, result.state()), () -> assertEquals(expected, result.reading()),
                () -> assertTrue(monitor.history().isEmpty()), () -> assertTrue(service.history(TITIWANGSA).isEmpty()));
    }
    @Test void refresh_failureWithoutCache_doesNotInventData() {
        var service = new ReadingService(d -> { throw offline(); }, new ReadingCache(folder), monitor);
        var result = service.refresh(PERDANA);
        assertAll(() -> assertEquals(UNAVAILABLE, result.state()), () -> assertNull(result.reading()));
    }
    @Test void refresh_olderProviderData_keepsNewerCache() {
        var counter = new AtomicInteger(1);
        var service = new ReadingService(d -> reading(d, 110, counter.getAndDecrement()), new ReadingCache(folder), monitor);
        service.refresh(TITIWANGSA);
        assertEquals(CACHED, service.refresh(TITIWANGSA).state());
    }
    @Test void refresh_sameHourTwice_replacesTrendPoint() {
        var service = new ReadingService(d -> reading(d, 105, 0), new ReadingCache(folder), monitor);
        service.refresh(TITIWANGSA);
        service.refresh(TITIWANGSA);
        assertEquals(1, service.history(TITIWANGSA).size());
    }
    @Test void refresh_wrongDistrict_doesNotCacheIt() {
        var service = new ReadingService(d -> reading(PERDANA, 110, 0), new ReadingCache(folder), monitor);
        assertEquals(UNAVAILABLE, service.refresh(TITIWANGSA).state());
    }
    private static ApiFailure offline() { return new ApiFailure(ApiFailure.Kind.NETWORK, "Offline.", null); }
}
