package cityair.app;

import cityair.api.OpenMeteoClient;
import cityair.controller.MonitorController;
import cityair.model.District;
import cityair.observer.AlertMonitor;
import cityair.repository.ReadingCache;
import cityair.repository.ReportStore;
import cityair.service.ReadingService;
import cityair.view.MonitorPanel;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Entrypoint for the desktop application and command-line smoke checks. */
public final class Main {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private Main() {}

    /** Starts Swing, or performs an explicit smoke check when requested. */
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--version")) {
            System.out.println("Kuala Lumpur Air & Parks 1.0.0 (CSC61204)");
            return;
        }
        if (args.length == 1 && args[0].equals("--live-check")) {
            System.out.println(client().fetch(District.TITIWANGSA));
            return;
        }
        if (args.length != 0) throw new IllegalArgumentException("Use no arguments, --version or --live-check.");
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("A desktop is required for Swing. Use --version for a headless smoke check.");
        SwingUtilities.invokeLater(Main::open);
    }

    private static OpenMeteoClient client() {
        var http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        return new OpenMeteoClient(OpenMeteoClient.ENDPOINT, http, REQUEST_TIMEOUT, Clock.systemUTC());
    }

    private static void open() {
        var data = Path.of(System.getProperty("user.home"), ".kl-air-and-parks");
        var worker = Executors.newSingleThreadExecutor();
        var alerts = new AlertMonitor(AlertMonitor.DEFAULT_THRESHOLD);
        var service = new ReadingService(client(), new ReadingCache(data.resolve("cache")), alerts);
        var panel = new MonitorPanel(Preferences.userRoot().node("csc61204/kl-air-parks"));
        panel.bind(new MonitorController(service, new ReportStore(data), alerts, worker,
                SwingUtilities::invokeLater, Clock.systemUTC(), panel));
        var window = new JFrame("Kuala Lumpur Air & Parks");
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { worker.shutdownNow(); }
        });
        window.setContentPane(panel);
        window.pack();
        window.setMinimumSize(window.getSize());
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
