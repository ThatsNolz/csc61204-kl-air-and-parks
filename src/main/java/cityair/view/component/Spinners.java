package cityair.view.component;

import cityair.view.design.Paints;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

/** Draws the indeterminate progress ring used inside buttons, cards and the status bar. */
public final class Spinners {
    private static final int SWEEP = 280;
    private static final float WEIGHT = 2.1f;

    private Spinners() {}

    /**
     * Paints a progress ring of the given size with its opening at {@code angle} degrees. The
     * caller owns the animation, so one timer can drive several indicators.
     */
    public static void paint(Graphics2D graphics, double x, double y, double size, double angle, Color colour) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            double inset = WEIGHT / 2;
            g.setStroke(new BasicStroke(WEIGHT, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Paints.alpha(colour, 55));
            g.draw(new Arc2D.Double(x + inset, y + inset, size - WEIGHT, size - WEIGHT, 0, 360, Arc2D.OPEN));
            g.setColor(colour);
            g.draw(new Arc2D.Double(x + inset, y + inset, size - WEIGHT, size - WEIGHT, -angle, -SWEEP, Arc2D.OPEN));
        } finally {
            g.dispose();
        }
    }
}
