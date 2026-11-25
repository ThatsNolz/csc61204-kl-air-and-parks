package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JCheckBox;

/**
 * A switch for settings that take effect immediately.
 *
 * <p>It extends {@link JCheckBox} on purpose: the model, the action listeners, the keyboard
 * handling and the accessibility role all keep working, and only the painting is replaced. State is
 * shown by position, colour and a tick mark together, so the control does not rely on colour alone.
 * </p>
 */
public final class ToggleSwitch extends JCheckBox implements Themed {
    private static final int TRACK_WIDTH = 42, TRACK_HEIGHT = 24, KNOB_INSET = 3, LABEL_GAP = 12;
    private double position;
    private boolean quiet;
    private transient Motion motion;

    /** Creates a labelled switch. */
    public ToggleSwitch(String label) {
        super(label);
        setFont(Type.text(Type.BODY));
        setOpaque(false);
        setFocusPainted(false);
        setIconTextGap(LABEL_GAP);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addItemListener(event -> animate());
        position = isSelected() ? 1 : 0;
        applyTheme();
    }

    /**
     * Applies a saved preference: the switch moves to its final position at once, because an
     * animation on a value the user did not just change would suggest something happened.
     */
    public void setSelectedQuietly(boolean value) {
        quiet = true;
        setSelected(value);
        quiet = false;
        if (motion != null) motion.cancel();
        position = value ? 1 : 0;
        repaint();
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { setForeground(Skin.color(Tone.INK)); }

    /** Returns the width of the track plus its label. */
    @Override public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        int text = getText() == null ? 0 : metrics.stringWidth(getText()) + LABEL_GAP;
        return new Dimension(TRACK_WIDTH + text, Math.max(TRACK_HEIGHT, metrics.getHeight()));
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            int top = (getHeight() - TRACK_HEIGHT) / 2;
            paintTrack(g, top);
            paintKnob(g, top);
            paintLabel(g);
        } finally {
            g.dispose();
        }
    }

    private void paintTrack(Graphics2D g, int top) {
        var track = Paints.round(0, top, TRACK_WIDTH, TRACK_HEIGHT, Metrics.RADIUS_PILL);
        g.setColor(Paints.mix(Skin.color(Tone.SURFACE_SUNKEN), Skin.color(Tone.ACCENT), position));
        g.fill(track);
        g.setStroke(new BasicStroke(Metrics.STROKE_HAIRLINE));
        g.setColor(Paints.mix(Skin.color(Tone.STROKE_STRONG), Skin.color(Tone.ACCENT), position));
        g.draw(track);
        if (isFocusOwner()) {
            g.setStroke(new BasicStroke(Metrics.STROKE_EMPHASIS));
            g.setColor(Skin.color(Tone.FOCUS));
            g.draw(Paints.round(-2, top - 2, TRACK_WIDTH + 4, TRACK_HEIGHT + 4, Metrics.RADIUS_PILL));
        }
    }

    private void paintKnob(Graphics2D g, int top) {
        double travel = TRACK_WIDTH - TRACK_HEIGHT;
        double size = TRACK_HEIGHT - KNOB_INSET * 2;
        double x = KNOB_INSET + travel * position;
        var knob = new Ellipse2D.Double(x, top + KNOB_INSET, size, size);
        Paints.shadow(g, knob, Skin.color(Tone.SHADOW), 2, 1);
        g.setColor(position > 0.5 ? Skin.color(Tone.ON_ACCENT) : Skin.color(Tone.INK_MUTED));
        g.fill(knob);
        if (position > 0.6) paintTick(g, x, top + TRACK_HEIGHT / 2.0, size);
    }

    private void paintTick(Graphics2D g, double x, double middle, double size) {
        var tick = new java.awt.geom.Path2D.Double();
        tick.moveTo(x + size * 0.3, middle);
        tick.lineTo(x + size * 0.45, middle + size * 0.17);
        tick.lineTo(x + size * 0.72, middle - size * 0.2);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Skin.color(Tone.ACCENT));
        g.draw(tick);
    }

    private void paintLabel(Graphics2D g) {
        if (getText() == null || getText().isEmpty()) return;
        var metrics = g.getFontMetrics(getFont());
        g.setFont(getFont());
        g.setColor(isEnabled() ? Skin.color(Tone.INK) : Skin.color(Tone.INK_FAINT));
        g.drawString(getText(), TRACK_WIDTH + LABEL_GAP,
                getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private void animate() {
        if (quiet) return;
        if (motion != null) motion.cancel();
        double from = position;
        double to = isSelected() ? 1 : 0;
        motion = Motion.play(Metrics.MOTION_BASE, progress -> {
            position = from + (to - from) * Motion.easeOut(progress);
            repaint();
        });
    }
}
