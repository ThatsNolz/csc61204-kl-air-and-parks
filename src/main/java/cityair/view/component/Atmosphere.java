package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.JPanel;

/**
 * The background the whole application sits on, and the signature element of the Aurora identity.
 *
 * <p>It is a quiet vertical gradient lit by two soft fields of colour. The upper field takes the
 * colour of the air-quality category currently being shown, so the room the data sits in reflects
 * the condition of the air before a single figure has been read. The effect is deliberately faint,
 * never the only indication of a category, and it fades between colours rather than switching, so
 * a refresh reads as a change in the world rather than a flicker in the interface.</p>
 *
 * <p>A fine grain is laid over the gradient. Flat digital gradients band visibly on wide surfaces;
 * a few per cent of noise removes the banding and gives the surface a physical quality. The grain
 * is generated once per size into an image and tiled, so it costs nothing per frame.</p>
 */
public class Atmosphere extends JPanel implements Themed {
    private static final int GRAIN_TILE = 140, GRAIN_SEED = 611204;
    private static final double GLOW_RADIUS = 0.78, SECOND_GLOW_RADIUS = 0.55;
    private static BufferedImage grain;
    private transient Color condition;
    private transient Color previousCondition;
    private double blend = 1;
    private transient Motion motion;

    /** Creates the backdrop with the given layout for its content. */
    public Atmosphere(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    /**
     * Sets the colour of the upper light field, normally the category of the reading on screen.
     * Passing {@code null} returns the backdrop to its neutral state for "no verified data".
     */
    public void setCondition(Color value) {
        if (java.util.Objects.equals(condition, value)) return;
        previousCondition = currentGlow();
        condition = value;
        if (motion != null) motion.cancel();
        motion = Motion.play(Metrics.MOTION_DATA, progress -> {
            blend = Motion.easeInOut(progress);
            repaint();
        });
    }

    /** Returns the colour of the upper light field, or null while no reading is shown. */
    public Color condition() { return condition; }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var bounds = new Rectangle(0, 0, getWidth(), getHeight());
            g.setPaint(Paints.vertical(0, getHeight(), Skin.color(Tone.CANVAS), Skin.color(Tone.CANVAS_EDGE)));
            g.fill(bounds);
            paintGlows(g);
            paintGrain(g, bounds);
        } finally {
            g.dispose();
        }
    }

    private void paintGlows(Graphics2D g) {
        double radius = Math.max(getWidth(), getHeight()) * GLOW_RADIUS;
        g.setPaint(Paints.glow(getWidth() * 0.82, -getHeight() * 0.1, radius, blended()));
        g.fillRect(0, 0, getWidth(), getHeight());
        var accent = Paints.alpha(Skin.color(Tone.ACCENT), Skin.theme().isDark() ? 20 : 14);
        g.setPaint(Paints.glow(getWidth() * 0.05, getHeight() * 1.05,
                Math.max(getWidth(), getHeight()) * SECOND_GLOW_RADIUS, accent));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private Color blended() {
        Color from = previousCondition == null ? neutral() : previousCondition;
        Color to = currentGlow();
        return Paints.mix(from, to, blend);
    }

    private Color currentGlow() {
        if (condition == null) return neutral();
        return Paints.alpha(condition, Skin.theme().isDark() ? 34 : 24);
    }

    private static Color neutral() {
        return Skin.color(Tone.GLOW);
    }

    private void paintGrain(Graphics2D g, Rectangle bounds) {
        g.setPaint(new TexturePaint(grainTile(), new Rectangle(0, 0, GRAIN_TILE, GRAIN_TILE)));
        g.fill(bounds);
    }

    private static BufferedImage grainTile() {
        if (grain != null) return grain;
        var image = new BufferedImage(GRAIN_TILE, GRAIN_TILE, BufferedImage.TYPE_INT_ARGB);
        var random = new Random(GRAIN_SEED);
        for (int y = 0; y < GRAIN_TILE; y++) {
            for (int x = 0; x < GRAIN_TILE; x++) {
                int level = random.nextInt(2) == 0 ? 0 : 255;
                image.setRGB(x, y, (random.nextInt(7) << 24) | (level << 16) | (level << 8) | level);
            }
        }
        grain = image;
        return grain;
    }
}
