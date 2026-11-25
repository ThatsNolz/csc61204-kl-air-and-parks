package cityair.view;

import cityair.observer.AlertMonitor;
import cityair.view.component.AuroraComponent;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Panels;
import cityair.view.component.Spinners;
import cityair.view.component.Text;
import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The bar across the foot of the application: what just happened, and anything the Observer has
 * delivered.
 *
 * <p>Alerts arrive here rather than in a dialog. A modal would interrupt whatever the reader is
 * doing to tell them something they can act on at their own pace, so instead the banner slides in,
 * holds its colour and stays until the next one replaces it. The message area never relies on
 * colour alone: a failure is prefixed with the word "Notice" and carries a warning icon.</p>
 */
final class StatusBar extends JPanel implements Themed {
    private static final int SPINNER = 16;
    private final AuroraLabel status = Text.named(new AuroraLabel("Ready. Choose a pilot area to start.",
            Type.text(Type.LABEL), Tone.INK), "status");
    private final AlertBanner banner = new AlertBanner();
    private final ProgressDot progress = new ProgressDot();

    StatusBar() {
        super(new BorderLayout(Metrics.SPACE_4, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, Metrics.SPACE_5, 0, Metrics.SPACE_5));
        setPreferredSize(new Dimension(0, Metrics.STATUS_BAR_HEIGHT));
        add(status, BorderLayout.CENTER);
        var right = Panels.row(Metrics.SPACE_4);
        right.add(banner, BorderLayout.CENTER);
        right.add(progress, BorderLayout.EAST);
        add(right, BorderLayout.EAST);
    }

    /** Shows a message, marking a failure in words, icon and colour together. */
    void message(String text, boolean error) {
        status.setText((error ? "Notice: " : "") + text);
        status.setForeground(Skin.color(error ? Tone.DANGER : Tone.INK));
        status.setIcon(error ? Icons.of(Glyph.WARNING, Icons.SMALL, Tone.DANGER) : null);
        status.setIconTextGap(Metrics.SPACE_2);
        getAccessibleContext().setAccessibleDescription(status.getText());
    }

    /** Turns the activity indicator on or off. */
    void setBusy(boolean busy) { progress.setBusy(busy); }

    /** Shows a delivered threshold crossing. */
    void showAlert(AlertMonitor.Alert alert) { banner.show(alert); }

    /** The label the integration tests read for the current message. */
    AuroraLabel statusLabel() { return status; }

    /** Re-reads the colours of the message area and the bar itself. */
    @Override public void applyTheme() {
        status.applyTheme();
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setColor(Paints.alpha(Skin.color(Tone.SURFACE), Skin.theme().isDark() ? 150 : 210));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Skin.color(Tone.STROKE));
            g.fillRect(0, 0, getWidth(), 1);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    /** The alert banner, which slides in when a crossing is delivered. */
    private static final class AlertBanner extends AuroraComponent {
        private final AuroraLabel text = Text.named(new AuroraLabel("No notifications delivered in this session.",
                Type.text(Type.LABEL), Tone.INK_MUTED), "latestAlert");
        private boolean active;
        private double entrance = 1;

        AlertBanner() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, Metrics.SPACE_3, 5, Metrics.SPACE_3));
            add(text, BorderLayout.CENTER);
        }

        void show(AlertMonitor.Alert alert) {
            active = true;
            text.setText("Pollution alert for " + alert.district() + ": U.S. AQI " + alert.aqi()
                    + " (threshold " + alert.threshold() + ")");
            text.setFont(Type.strong(Type.LABEL));
            text.setForeground(Skin.color(Tone.DANGER));
            text.setIcon(Icons.of(Glyph.BELL, Icons.SMALL, Tone.DANGER));
            text.setIconTextGap(Metrics.SPACE_2);
            getAccessibleContext().setAccessibleDescription(text.getText());
            Motion.play(Metrics.MOTION_SLOW, progress -> {
                entrance = Motion.easeOut(progress);
                repaint();
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (!active) return;
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                g.setColor(Paints.fade(Skin.color(Tone.DANGER_SOFT), entrance));
                g.fill(Paints.round(0, 2, getWidth(), getHeight() - 4.0, Metrics.RADIUS_PILL));
            } finally {
                g.dispose();
            }
        }
    }

    /** A small ring that spins only while a request is in flight. */
    private static final class ProgressDot extends AuroraComponent {
        private double angle;
        private boolean busy;
        private transient Motion motion;

        ProgressDot() {
            setPreferredSize(new Dimension(SPINNER + 4, SPINNER + 4));
            getAccessibleContext().setAccessibleName("Retrieval activity");
        }

        void setBusy(boolean value) {
            if (busy == value) return;
            busy = value;
            if (motion != null) motion.cancel();
            if (value) {
                motion = Motion.play(Integer.MAX_VALUE, progress -> {
                    angle = (angle + 8) % 360;
                    repaint();
                });
            }
            getAccessibleContext().setAccessibleDescription(value ? "Retrieving" : "Idle");
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (!busy) return;
            var g = (Graphics2D) graphics.create();
            try {
                Spinners.paint(g, 2, 2, SPINNER, angle, Skin.color(Tone.ACCENT));
            } finally {
                g.dispose();
            }
        }
    }
}
