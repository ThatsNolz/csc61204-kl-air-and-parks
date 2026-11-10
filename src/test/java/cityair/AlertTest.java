package cityair;

import cityair.model.District;
import cityair.observer.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static cityair.DomainTest.reading;
import static cityair.model.District.*;
import static org.junit.jupiter.api.Assertions.*;

class AlertTest {
    private final AlertMonitor monitor = new AlertMonitor(AlertMonitor.DEFAULT_THRESHOLD);
    private final ArrayList<AlertMonitor.Alert> delivered = new ArrayList<>();
    private final PollutionObserver observer = delivered::add;

    @Test void publish_atThreshold_notifiesOnce() {
        monitor.subscribe(TITIWANGSA, observer);
        monitor.publish(reading(TITIWANGSA, 101, 0));
        monitor.publish(reading(TITIWANGSA, 130, 60));
        assertEquals(1, delivered.size());
    }
    @Test void publish_belowThreshold_recordsNoAlert() {
        monitor.publish(reading(TITIWANGSA, 100, 0));
        assertTrue(monitor.history().isEmpty());
    }
    @Test void publish_recoveryAndRecross_addsSecondAlert() {
        monitor.subscribe(TITIWANGSA, observer);
        monitor.publish(reading(TITIWANGSA, 120, 0));
        monitor.publish(reading(TITIWANGSA, 80, 60));
        monitor.publish(reading(TITIWANGSA, 120, 120));
        assertEquals(2, delivered.size());
    }
    @Test void publish_duplicateAndOlderModelHours_ignoresThem() {
        monitor.publish(reading(TITIWANGSA, 80, 60));
        monitor.publish(reading(TITIWANGSA, 120, 60));
        monitor.publish(reading(TITIWANGSA, 120, 0));
        assertTrue(monitor.history().isEmpty());
    }
    @Test void subscribe_repeatedListener_deliversOnce() {
        monitor.subscribe(TITIWANGSA, observer);
        monitor.subscribe(TITIWANGSA, observer);
        monitor.publish(reading(TITIWANGSA, 110, 0));
        assertEquals(1, delivered.size());
    }
    @Test void unsubscribe_beforeCrossing_skipsNotificationButKeepsHistory() {
        monitor.subscribe(TITIWANGSA, observer);
        monitor.unsubscribe(TITIWANGSA, observer);
        monitor.publish(reading(TITIWANGSA, 101, 0));
        assertAll(() -> assertTrue(delivered.isEmpty()), () -> assertEquals(1, monitor.history().size()));
    }
    @Test void publish_differentDistrict_doesNotNotifySubscriber() {
        monitor.subscribe(CITY_CENTRE, observer);
        monitor.publish(reading(PERDANA, 101, 0));
        assertAll(() -> assertTrue(delivered.isEmpty()), () -> assertEquals(District.PERDANA, monitor.history().get(0).district()));
    }
    @Test void publish_badListener_doesNotPreventOtherDelivery() {
        monitor.subscribe(TITIWANGSA, a -> { throw new IllegalStateException("test observer"); });
        monitor.subscribe(TITIWANGSA, observer);
        monitor.publish(reading(TITIWANGSA, 110, 0));
        assertEquals(1, delivered.size());
    }
    @Test void constructor_nonPositiveThreshold_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AlertMonitor(0));
    }
}
