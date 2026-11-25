package cityair;

import cityair.controller.MonitorController;
import cityair.model.District;
import cityair.model.Reading;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.view.MonitorPanel;
import cityair.view.design.AuroraTheme;
import cityair.view.design.Skin;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import static cityair.SwingFlowTest.find;

/**
 * Captures every screen in both themes and at two window sizes.
 *
 * <p>This is a design record, not integration evidence: the provider is a local stub, so every
 * screen is labelled TEST FIXTURE by the application itself and no capture can be mistaken for a
 * live reading. Run it after a visual change to see the whole product at once.</p>
 *
 * <pre>java -cp "target/kl-air-and-parks-1.0.0.jar;target/test-classes" cityair.DesignShowcase evidence/showcase</pre>
 */
public final class DesignShowcase {
    private static final String[] SCREENS = {"overview", "monitoring", "planner", "reports", "alerts",
            "data", "settings"};
    private static final int[][] SIZES = {{1420, 900}, {1000, 700}};
    private final Path output;
    private final Path temporary = Files.createTempDirectory("cityair-showcase");
    private final Preferences prefs = Preferences.userRoot().node("csc61204-showcase/" + UUID.randomUUID());
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private MonitorPanel panel;
    private JFrame frame;

    private DesignShowcase(Path output) throws Exception {
        this.output = output;
    }

    /** Writes one PNG per screen, theme and size into the folder named by the first argument. */
    public static void main(String[] args) throws Exception {
        new DesignShowcase(Path.of(args[0])).run();
    }

    private void run() throws Exception {
        Files.createDirectories(output);
        try {
            for (int[] size : SIZES) {
                for (AuroraTheme theme : AuroraTheme.values()) {
                    capture(theme, size[0], size[1], size == SIZES[1]);
                }
            }
        } finally {
            close();
            worker.shutdownNow();
            prefs.removeNode();
        }
    }

    private void capture(AuroraTheme theme, int width, int height, boolean narrow) throws Exception {
        open(theme, width, height);
        seed();
        String suffix = (theme == AuroraTheme.DARK ? "dark" : "light") + (narrow ? "-narrow" : "");
        for (int i = 0; i < SCREENS.length; i++) {
            tab(i);
            Thread.sleep(700);
            shot(SCREENS[i] + "-" + suffix);
        }
        close();
    }

    /** Feeds the view controlled readings, session history, reports and one delivered alert. */
    private void seed() throws Exception {
        var now = Instant.now();
        onEdt(() -> {
            for (District area : District.values()) panel.reading(area, result(area, now), history(area, now));
            panel.alert(List.of(new AlertMonitor.Alert(District.TITIWANGSA, 118, 101, now)),
                    new AlertMonitor.Alert(District.TITIWANGSA, 118, 101, now));
            panel.message("Controlled sample data for the design record.", false);
        });
        Thread.sleep(900);
    }

    private static ReadingService.Result result(District area, Instant now) {
        return new ReadingService.Result(ReadingService.State.LIVE, sample(area, now, index(area), 0),
                "Controlled fixture response.");
    }

    private static List<Reading> history(District area, Instant now) {
        int base = index(area);
        return List.of(sample(area, now, base - 23, 4), sample(area, now, base - 11, 3),
                sample(area, now, base - 16, 2), sample(area, now, base - 5, 1), sample(area, now, base, 0));
    }

    private static Reading sample(District area, Instant now, int aqi, int hoursAgo) {
        var validAt = now.minus(Duration.ofHours(hoursAgo));
        double pm25 = Math.round(aqi * 0.42 * 10) / 10d;
        Double pm10 = area == District.CITY_CENTRE ? null : Math.round(aqi * 0.63 * 10) / 10d;
        return new Reading(area, validAt, now, Math.max(1, aqi), pm25, pm10,
                Math.round(aqi * 0.31 * 10) / 10d, Math.round(aqi * 0.84 * 10) / 10d);
    }

    private static int index(District area) {
        return switch (area) {
            case TITIWANGSA -> 118;
            case CITY_CENTRE -> 96;
            case PERDANA -> 47;
        };
    }

    private void open(AuroraTheme theme, int width, int height) throws Exception {
        var alerts = new AlertMonitor(AlertMonitor.DEFAULT_THRESHOLD);
        var service = new ReadingService(area -> sample(area, Instant.now(), index(area), 0),
                new ReadingCache(temporary.resolve("cache")), alerts);
        onEdt(() -> {
            panel = new MonitorPanel(prefs, true);
            panel.bind(new MonitorController(service, new ReportStore(temporary), alerts, worker,
                    SwingUtilities::invokeLater, Clock.systemUTC(), panel));
            frame = window(theme, width, height);
            frame.setVisible(true);
        });
        selectTheme(theme);
    }

    /** Switches the theme the way a user would, through the control in the top bar. */
    private void selectTheme(AuroraTheme theme) throws Exception {
        onEdt(() -> {
            if (Skin.theme() != theme) ((javax.swing.AbstractButton) find(panel, "themeToggle")).doClick(0);
        });
    }

    private JFrame window(AuroraTheme theme, int width, int height) {
        var created = new JFrame("Kuala Lumpur Air & Parks | DESIGN RECORD · " + theme);
        var content = new JPanel(new BorderLayout());
        var banner = new JLabel("DESIGN RECORD: controlled sample data, not a live provider response.",
                SwingConstants.CENTER);
        banner.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(banner, BorderLayout.NORTH);
        content.add(panel, BorderLayout.CENTER);
        created.setContentPane(content);
        created.setSize(new Dimension(width, height));
        created.setLocationRelativeTo(null);
        created.setAlwaysOnTop(true);
        return created;
    }

    private void tab(int index) throws Exception {
        onEdt(() -> ((JTabbedPane) find(panel, "tabs")).setSelectedIndex(index));
    }

    private void shot(String name) throws Exception {
        var bounds = new Rectangle[1];
        onEdt(() -> bounds[0] = frame.getBounds());
        var robot = new Robot(frame.getGraphicsConfiguration().getDevice());
        robot.waitForIdle();
        robot.delay(250);
        ImageIO.write(robot.createScreenCapture(bounds[0]), "png", output.resolve(name + ".png").toFile());
        System.out.println("CAPTURED " + name);
    }

    private void close() throws Exception {
        if (frame != null) onEdt(() -> frame.dispose());
    }

    private static void onEdt(Runnable runnable) throws Exception { SwingUtilities.invokeAndWait(runnable); }
}
