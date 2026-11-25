package cityair.view;

import cityair.Components;
import cityair.controller.MonitorController;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Skin;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Paints the real screens off screen, in both themes and at several window sizes.
 *
 * <p>Custom painting is where a desktop interface fails silently: an exception inside
 * {@code paintComponent} is swallowed by the event thread, and a layout that overflows simply
 * clips. These tests render every screen into an image and check that it drew something, that the
 * two themes actually differ, and that the content fits the width it was given.</p>
 */
class RenderTest {
    private static final Instant NOW = Instant.parse("2026-11-25T08:00:00Z");
    private static final int[][] SIZES = {{1420, 900}, {1100, 760}, {980, 660}};
    @TempDir Path folder;

    @AfterEach void resetAppearance() {
        Skin.setTheme(AuroraTheme.DARK);
        Skin.setReducedMotion(false);
    }

    @ParameterizedTest @EnumSource(AuroraTheme.class)
    void everyScreen_inBothThemes_paintsWithoutFailingAndIsNotBlank(AuroraTheme theme) throws Exception {
        onEdt(() -> {
            var panel = loaded();
            applyTheme(panel, theme);
            var tabs = (JTabbedPane) Components.find(panel, "tabs");
            for (int screen = 0; screen < tabs.getTabCount(); screen++) {
                tabs.setSelectedIndex(screen);
                var image = paint(panel, 1420, 900);
                assertTrue(distinctColours(image) > 12,
                        "screen " + screen + " in " + theme + " painted almost nothing");
            }
        });
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "2", "3", "4", "5", "6"})
    void everyScreen_atEverySupportedSize_fitsTheWidthItIsGiven(int screen) throws Exception {
        onEdt(() -> {
            var panel = loaded();
            var tabs = (JTabbedPane) Components.find(panel, "tabs");
            tabs.setSelectedIndex(screen);
            for (int[] size : SIZES) {
                paint(panel, size[0], size[1]);
                var body = tabs.getSelectedComponent();
                assertTrue(body.getWidth() <= size[0],
                        "screen " + screen + " is wider than the " + size[0] + " pixel window");
                assertTrue(overflowing(body).isEmpty(),
                        "clipped at " + size[0] + ": " + overflowing(body));
            }
        });
    }

    @Test void theme_switchedToLight_paintsTheSameScreenDifferently() throws Exception {
        onEdt(() -> {
            var panel = loaded();
            applyTheme(panel, AuroraTheme.DARK);
            var dark = paint(panel, 1200, 800);
            applyTheme(panel, AuroraTheme.LIGHT);
            var light = paint(panel, 1200, 800);
            assertNotEquals(dark.getRGB(600, 400), light.getRGB(600, 400),
                    "the two themes painted the same pixel");
            assertTrue(brightness(light) > brightness(dark), "Aurora Light should be the brighter theme");
        });
    }

    @Test void motion_reducedMotionEnabled_appliesEveryAnimationImmediately() throws Exception {
        onEdt(() -> {
            var panel = loaded();
            ((javax.swing.AbstractButton) Components.require(panel, "reducedMotion")).doClick(0);
            assertEquals(0, Skin.duration(500), "the setting did not reach the motion engine");
            var settled = paint(panel, 1200, 800);
            assertTrue(distinctColours(settled) > 12, "the interface still paints with motion reduced");
        });
    }

    @Test void emptyApplication_beforeAnyRefresh_paintsItsEmptyStates() throws Exception {
        onEdt(() -> {
            var panel = panel();
            var tabs = (JTabbedPane) Components.find(panel, "tabs");
            for (int screen = 0; screen < tabs.getTabCount(); screen++) {
                tabs.setSelectedIndex(screen);
                assertTrue(distinctColours(paint(panel, 1200, 800)) > 8, "empty screen " + screen);
            }
        });
    }

    @Test void overview_extremeIndex_paintsTheTopOfTheScaleWithoutOverflowing() throws Exception {
        onEdt(() -> {
            var panel = panel();
            var hazardous = new Reading(District.TITIWANGSA, NOW, NOW, 820, 400.0, 500.0, 300.0, 400.0);
            panel.reading(District.TITIWANGSA, live(hazardous), List.of(hazardous));
            assertTrue(distinctColours(paint(panel, 1200, 800)) > 12);
            assertEquals("820", ((javax.swing.JLabel) Components.find(panel, "aqiValue")).getText());
        });
    }

    @Test void interaction_pointerAndKeyboard_leaveEveryScreenPaintable() throws Exception {
        onEdt(() -> {
            var panel = loaded();
            var tabs = (JTabbedPane) Components.find(panel, "tabs");
            paint(panel, 1300, 860);
            hover(Components.find(panel, "refresh"));
            hover(Components.find(panel, "refreshAll"));
            tabs.setSelectedIndex(1);
            paint(panel, 1300, 860);
            var chart = Components.require(panel, "trendChart");
            move(chart, chart.getWidth() / 2, chart.getHeight() / 2);
            press(chart, KeyEvent.VK_LEFT);
            press(chart, KeyEvent.VK_RIGHT);
            assertTrue(distinctColours(paint(panel, 1300, 860)) > 12);
        });
    }

    @Test void navigationRail_collapsed_paintsIconsOnlyAndStaysNarrow() throws Exception {
        onEdt(() -> {
            var panel = loaded();
            paint(panel, 1420, 900);
            int wide = railWidth(panel);
            ((javax.swing.AbstractButton) Components.find(panel, "railToggle")).doClick(0);
            paint(panel, 1420, 900);
            assertTrue(railWidth(panel) < wide, "the rail did not collapse");
        });
    }

    private static int railWidth(MonitorPanel panel) {
        var tabs = (JTabbedPane) Components.find(panel, "tabs");
        return tabs.getBoundsAt(0).width;
    }

    private MonitorPanel loaded() {
        var panel = panel();
        for (District area : District.values()) {
            var sample = reading(area, index(area), 0);
            panel.reading(area, live(sample), history(area));
        }
        panel.alert(List.of(new AlertMonitor.Alert(District.TITIWANGSA, 118, 101, NOW)),
                new AlertMonitor.Alert(District.TITIWANGSA, 118, 101, NOW));
        return panel;
    }

    private MonitorPanel panel() {
        var prefs = Preferences.userRoot().node("csc61204-test/render/" + UUID.randomUUID());
        var panel = new MonitorPanel(prefs, true, Clock.fixed(NOW, java.time.ZoneOffset.UTC));
        var monitor = new AlertMonitor(101);
        var service = new ReadingService(area -> reading(area, index(area), 0),
                new ReadingCache(folder.resolve("cache")), monitor);
        panel.bind(new MonitorController(service, new ReportStore(folder), monitor, Runnable::run,
                Runnable::run, Clock.fixed(NOW, java.time.ZoneOffset.UTC), panel));
        return panel;
    }

    private static void applyTheme(MonitorPanel panel, AuroraTheme theme) {
        if (Skin.theme() != theme) {
            ((javax.swing.AbstractButton) Components.find(panel, "themeToggle")).doClick(0);
        }
    }

    private static ReadingService.Result live(Reading sample) {
        return new ReadingService.Result(ReadingService.State.LIVE, sample, "Controlled fixture.");
    }

    private static List<Reading> history(District area) {
        int base = index(area);
        return List.of(reading(area, base - 20, 3), reading(area, base - 8, 2),
                reading(area, base - 14, 1), reading(area, base, 0));
    }

    private static Reading reading(District area, int aqi, int hoursAgo) {
        return new Reading(area, NOW.minus(Duration.ofHours(hoursAgo)), NOW, Math.max(1, aqi),
                aqi * 0.4, area == District.CITY_CENTRE ? null : aqi * 0.6, aqi * 0.3, aqi * 0.8);
    }

    private static int index(District area) {
        return switch (area) {
            case TITIWANGSA -> 118;
            case CITY_CENTRE -> 96;
            case PERDANA -> 42;
        };
    }

    /** Lays the panel out at a size and paints it into an image, exactly as a window would. */
    static BufferedImage paint(Component component, int width, int height) {
        component.setSize(width, height);
        layOut(component);
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Lays a component tree out without a window.
     *
     * <p>{@link Container#validate()} does nothing until a component has a native peer, so an
     * off-screen tree keeps zero-sized children and paints nothing but its own background. Laying
     * each container out explicitly, parents before children, gives every child the bounds a real
     * window would and makes the painting below the top level real.</p>
     */
    private static void layOut(Component component) {
        if (!(component instanceof Container container)) return;
        container.doLayout();
        for (Component child : container.getComponents()) layOut(child);
    }

    /** Returns the names of components that stick out beyond their parent's right edge. */
    private static List<String> overflowing(Component root) {
        var found = new java.util.ArrayList<String>();
        collectOverflow(root, found);
        return found;
    }

    private static void collectOverflow(Component parent, List<String> found) {
        if (!(parent instanceof Container container)) return;
        for (Component child : container.getComponents()) {
            boolean named = child.getName() != null;
            boolean clipped = child.getWidth() > 0 && child.getX() + child.getWidth() > parent.getWidth() + 1;
            if (named && clipped && !(parent instanceof javax.swing.JViewport)) {
                found.add(child.getName());
            }
            collectOverflow(child, found);
        }
    }

    private static int distinctColours(BufferedImage image) {
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 7) {
            for (int x = 0; x < image.getWidth(); x += 7) colours.add(image.getRGB(x, y));
        }
        return colours.size();
    }

    private static double brightness(BufferedImage image) {
        long total = 0;
        int samples = 0;
        for (int y = 0; y < image.getHeight(); y += 11) {
            for (int x = 0; x < image.getWidth(); x += 11) {
                var colour = new Color(image.getRGB(x, y));
                total += colour.getRed() + colour.getGreen() + colour.getBlue();
                samples++;
            }
        }
        return total / (double) samples;
    }

    private static void hover(Component target) {
        target.dispatchEvent(new java.awt.event.MouseEvent(target, java.awt.event.MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, 2, 2, 0, false));
    }

    private static void move(Component target, int x, int y) {
        target.dispatchEvent(new java.awt.event.MouseEvent(target, java.awt.event.MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, x, y, 0, false));
    }

    private static void press(Component target, int key) {
        target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key,
                KeyEvent.CHAR_UNDEFINED));
    }

    private static void onEdt(Runnable work) throws Exception {
        var failure = new java.util.concurrent.atomic.AtomicReference<RuntimeException>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                work.run();
            } catch (RuntimeException ex) {
                failure.set(ex);
            }
        });
        if (failure.get() != null) throw failure.get();
    }
}
