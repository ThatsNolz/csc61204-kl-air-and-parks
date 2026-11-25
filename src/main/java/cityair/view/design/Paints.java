package cityair.view.design;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/** Java2D helpers shared by every custom component: quality hints, shapes, washes and shadows. */
public final class Paints {
    private static final int SHADOW_LAYERS = 7;
    private static final float GLOW_SPREAD = 0.85f;

    private Paints() {}

    /** Applies the rendering quality the whole application draws with. */
    public static Graphics2D quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }

    /** Returns a rounded rectangle, inset by half a stroke so its outline stays crisp. */
    public static RoundRectangle2D round(double x, double y, double width, double height, double radius) {
        double limit = Math.min(radius, Math.min(width, height) / 2);
        return new RoundRectangle2D.Double(x, y, Math.max(0, width), Math.max(0, height), limit * 2, limit * 2);
    }

    /** Returns the same colour with a different alpha. */
    public static Color alpha(Color colour, int value) {
        return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), clampByte(value));
    }

    /** Returns the same colour with its alpha scaled by a fraction of its current value. */
    public static Color fade(Color colour, double fraction) {
        return alpha(colour, (int) Math.round(colour.getAlpha() * clampUnit(fraction)));
    }

    /** Blends two colours; {@code amount} 0 returns the first and 1 returns the second. */
    public static Color mix(Color from, Color to, double amount) {
        double t = clampUnit(amount);
        return new Color(channel(from.getRed(), to.getRed(), t), channel(from.getGreen(), to.getGreen(), t),
                channel(from.getBlue(), to.getBlue(), t), channel(from.getAlpha(), to.getAlpha(), t));
    }

    /** Vertical two-stop gradient across the given height. */
    public static GradientPaint vertical(double y, double height, Color top, Color bottom) {
        return new GradientPaint(0f, (float) y, top, 0f, (float) (y + Math.max(1, height)), bottom);
    }

    /** Horizontal two-stop gradient across the given width. */
    public static GradientPaint horizontal(double x, double width, Color left, Color right) {
        return new GradientPaint((float) x, 0f, left, (float) (x + Math.max(1, width)), 0f, right);
    }

    /** Soft circular wash that fades to transparent, used for atmospheric lighting. */
    public static RadialGradientPaint glow(double centreX, double centreY, double radius, Color colour) {
        return new RadialGradientPaint(new Point2D.Double(centreX, centreY), (float) Math.max(1, radius),
                new float[]{0f, GLOW_SPREAD, 1f},
                new Color[]{colour, fade(colour, 0.35), alpha(colour, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    /**
     * Paints a soft drop shadow beneath a shape by stacking translucent outlines, which costs far
     * less than a blurred image and stays sharp when the display scales.
     */
    public static void shadow(Graphics2D g, Shape shape, Color colour, int spread, int offsetY) {
        var saved = g.getTransform();
        for (int layer = SHADOW_LAYERS; layer >= 1; layer--) {
            double scale = layer / (double) SHADOW_LAYERS;
            g.setColor(alpha(colour, (int) (colour.getAlpha() * 0.16 * (1 - scale) + 4)));
            g.setStroke(new java.awt.BasicStroke((float) (spread * scale * 2)));
            g.translate(0, offsetY * scale);
            g.draw(shape);
            g.setTransform(saved);
        }
    }

    /** Fills a shape with a translucent colour without disturbing the caller's composite. */
    public static void fill(Graphics2D g, Shape shape, Color colour, double opacity) {
        var saved = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clampUnit(opacity)));
        g.setColor(colour);
        g.fill(shape);
        g.setComposite(saved);
    }

    private static int channel(int from, int to, double t) {
        return clampByte((int) Math.round(from + (to - from) * t));
    }

    private static int clampByte(int value) { return Math.max(0, Math.min(255, value)); }

    private static double clampUnit(double value) { return Math.max(0, Math.min(1, value)); }
}
