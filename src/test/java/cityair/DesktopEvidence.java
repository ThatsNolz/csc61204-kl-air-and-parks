package cityair;

import cityair.api.OpenMeteoClient;
import cityair.controller.MonitorController;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.strategy.AdviceProfile;
import cityair.view.MonitorPanel;
import com.sun.net.httpserver.HttpServer;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.BorderLayout;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import static cityair.SwingFlowTest.find;

/** Explicitly invoked desktop integration evidence, not a headless Surefire test. */
public final class DesktopEvidence {
    private final Path output;
    private final Path temporary = Files.createTempDirectory("cityair-desktop-test");
    private final Preferences prefs = Preferences.userRoot().node("csc61204-evidence/" + UUID.randomUUID());
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicInteger response = new AtomicInteger(500);
    private final AtomicInteger fixtureAqi = new AtomicInteger(79);
    private MonitorPanel panel;
    private JFrame frame;

    private DesktopEvidence(Path output) throws Exception { this.output = output; }

    /** Launches real Swing windows and captures five verified integration outcomes. */
    public static void main(String[] args) throws Exception {
        new DesktopEvidence(Path.of(args[0])).run();
    }

    private void run() throws Exception {
        Files.createDirectories(output);
        var server = server();
        try {
            open(OpenMeteoClient.ENDPOINT, "Live provider");
            action("refresh");
            waitText("dashboard", "LIVE API");
            shot("IT-02-live-api");
            tab(1);
            action("refreshAll");
            waitEnabled("refreshAll");
            assertDistrictTable();
            shot("GUI-district-monitoring");
            tab(5);
            shot("GUI-data-and-impact");
            tab(3);
            onEdt(() -> area("description").setText("Test observation: smoke near a park entrance."));
            action("save");
            waitText("reportHistory", "Test observation: smoke near a park entrance.");
            shot("IT-01-save-report");
            action("save");
            waitText("status", "10 to 500");
            if (new ReportStore(temporary).list().size() != 1) {
                throw new AssertionError("A blank input changed the stored report count.");
            }
            shot("IT-05-invalid-input");
            close();
            localCases(server.getAddress().getPort());
        } finally {
            close();
            server.stop(0);
            worker.shutdownNow();
            prefs.removeNode();
        }
    }

    private void localCases(int port) throws Exception {
        open(URI.create("http://127.0.0.1:" + port + "/v1/air-quality"), "TEST HARNESS · local HTTP fixtures");
        action("refresh");
        waitText("dashboard", "CACHED");
        waitText("status", "500");
        shot("IT-03-http-500-cached");
        response.set(200);
        tab(4);
        onEdt(() -> ((JCheckBox) find(panel, "subscription")).doClick(0));
        action("refresh");
        waitText("dashboard", "TEST FIXTURE");
        waitText("aqiValue", "79");
        waitText("latestAlert", "No notifications");
        fixtureAqi.set(115);
        action("refresh");
        waitText("notifications", "crossed 101");
        waitText("latestAlert", "Pollution alert");
        shot("IT-04-observer-threshold");
        tab(1);
        shot("GUI-controlled-trend");
        tab(2);
        onEdt(() -> ((JComboBox<?>) find(panel, "profile")).setSelectedItem(AdviceProfile.CHILDREN));
        waitText("advice", "Shorten vigorous outdoor play");
        shot("GUI-advice-and-parks");
    }

    private HttpServer server() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/air-quality", e -> {
            try (e) {
                int aqi = fixtureAqi.get();
                int hours = aqi == 79 ? 1 : 2;
                String future = java.time.LocalDateTime.now(OpenMeteoClient.ZONE).plusHours(hours)
                        .withSecond(0).withNano(0).toString();
                String body = ApiTest.GOOD.replace("2026-11-10T15:00", future)
                        .replace("\"us_aqi\":79", "\"us_aqi\":" + aqi);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                e.sendResponseHeaders(response.get(), bytes.length);
                e.getResponseBody().write(bytes);
            }
        });
        server.start();
        return server;
    }

    private void open(URI endpoint, String mode) throws Exception {
        var alerts = new AlertMonitor(AlertMonitor.DEFAULT_THRESHOLD);
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var api = new OpenMeteoClient(endpoint, http, Duration.ofSeconds(12), Clock.systemUTC());
        var service = new ReadingService(api, new ReadingCache(temporary.resolve("cache")), alerts);
        onEdt(() -> {
            panel = new MonitorPanel(prefs, !endpoint.equals(OpenMeteoClient.ENDPOINT));
            panel.bind(new MonitorController(service, new ReportStore(temporary), alerts, worker,
                    SwingUtilities::invokeLater, Clock.systemUTC(), panel));
            frame = new JFrame("Kuala Lumpur Air & Parks | " + mode);
            var content = new JPanel(new BorderLayout());
            if (!endpoint.equals(OpenMeteoClient.ENDPOINT)) {
                var fixture = new JLabel("TEST ONLY: requests go to a controlled local HTTP server.", SwingConstants.CENTER);
                fixture.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                content.add(fixture, BorderLayout.NORTH);
            }
            content.add(panel, BorderLayout.CENTER);
            frame.setContentPane(content);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
    }

    private void action(String name) throws Exception {
        onEdt(() -> ((JButton) find(panel, name)).doClick(0));
    }

    private void tab(int i) throws Exception {
        onEdt(() -> ((JTabbedPane) find(panel, "tabs")).setSelectedIndex(i));
    }

    private JTextArea area(String name) { return (JTextArea) find(panel, name); }

    private void waitEnabled(String name) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(40).toNanos();
        while (System.nanoTime() < deadline) {
            var ready = new java.util.concurrent.atomic.AtomicBoolean();
            onEdt(() -> ready.set(find(panel, name).isEnabled()));
            if (ready.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError(name + " remained disabled");
    }

    private void assertDistrictTable() throws Exception {
        onEdt(() -> {
            var table = (JTable) find(panel, "districtTable");
            for (int i = 0; i < table.getRowCount(); i++) {
                if ("Not loaded".equals(table.getValueAt(i, 3))) throw new AssertionError("Missing comparison attempt");
            }
        });
    }

    private void waitText(String name, String expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(18).toNanos();
        while (System.nanoTime() < deadline) {
            var match = new java.util.concurrent.atomic.AtomicBoolean();
            onEdt(() -> {
                var c = find(panel, name);
                String text = c instanceof JLabel label ? label.getText() : ((JTextArea) c).getText();
                match.set(text.contains(expected));
            });
            if (match.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError(name + " did not show " + expected);
    }

    private void shot(String name) throws Exception {
        var bounds = new Rectangle[1];
        onEdt(() -> bounds[0] = frame.getBounds());
        var robot = new Robot(frame.getGraphicsConfiguration().getDevice());
        robot.waitForIdle();
        robot.delay(200);
        var image = robot.createScreenCapture(bounds[0]);
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
        System.out.println("PASS " + name);
    }

    private void close() throws Exception {
        if (frame != null) onEdt(() -> frame.dispose());
    }

    private static void onEdt(Runnable runnable) throws Exception { SwingUtilities.invokeAndWait(runnable); }
}
