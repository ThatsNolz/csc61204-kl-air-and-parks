package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import javax.swing.JComponent;

/**
 * A placeholder block shown while the first reading is still being retrieved.
 *
 * <p>A skeleton describes the shape of the content that is coming, which reads as faster and
 * calmer than an empty panel that suddenly fills. The sweep is purely decorative and stops as soon
 * as the block is hidden, and it is not drawn at all when the user has asked for reduced motion.
 * </p>
 */
public final class Skeleton extends AuroraComponent {
    private static final int SWEEP_WIDTH = 160, CYCLE_MILLIS = 1400;
    private final int preferredHeight;
    private double sweep;
    private transient Motion motion;

    /** Creates a placeholder of the given height. */
    public Skeleton(int height) {
        this.preferredHeight = height;
        getAccessibleContext().setAccessibleName("Loading placeholder");
    }

    /** Returns the height of the block this placeholder stands in for. */
    @Override public Dimension getPreferredSize() { return new Dimension(80, preferredHeight); }

    /** Starts the placeholder's sweep once the block is on screen. */
    @Override public void addNotify() {
        super.addNotify();
        start();
    }

    /** Stops the sweep when the block leaves the screen, so no timer outlives it. */
    @Override public void removeNotify() {
        if (motion != null) motion.cancel();
        super.removeNotify();
    }

    @Override protected javax.accessibility.AccessibleRole role() { return javax.accessibility.AccessibleRole.PROGRESS_BAR; }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var shape = Paints.round(0, 0, getWidth(), getHeight(), Metrics.RADIUS_SMALL);
            g.setColor(Skin.color(Tone.SKELETON));
            g.fill(shape);
            if (Skin.reducedMotion() || getWidth() <= 0) return;
            g.clip(shape);
            g.setPaint(sweepPaint());
            g.fill(shape);
        } finally {
            g.dispose();
        }
    }

    private LinearGradientPaint sweepPaint() {
        double x = sweep * (getWidth() + SWEEP_WIDTH * 2) - SWEEP_WIDTH;
        var highlight = Paints.alpha(Skin.color(Tone.INK), Skin.theme().isDark() ? 16 : 26);
        return new LinearGradientPaint(new Point2D.Double(x, 0), new Point2D.Double(x + SWEEP_WIDTH, 0),
                new float[]{0f, 0.5f, 1f},
                new java.awt.Color[]{Paints.alpha(highlight, 0), highlight, Paints.alpha(highlight, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    private void start() {
        if (motion != null) motion.cancel();
        if (Skin.reducedMotion()) return;
        motion = Motion.play(Integer.MAX_VALUE, progress -> {
            sweep = (System.currentTimeMillis() % CYCLE_MILLIS) / (double) CYCLE_MILLIS;
            repaint();
        });
    }
}
