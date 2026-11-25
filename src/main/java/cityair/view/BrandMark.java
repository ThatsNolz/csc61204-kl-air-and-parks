package cityair.view;

import cityair.view.component.AuroraComponent;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/**
 * The application mark: three arcs rising from a point, drawn in Java2D.
 *
 * <p>The form is the application in one shape. The point is a pilot area, the arcs are the
 * readings rising from it, and the outermost arc carries the accent that the rest of the interface
 * uses for live data. Because it is drawn rather than loaded, it is sharp at any display scale,
 * follows the active theme and adds nothing to the packaged size.</p>
 */
final class BrandMark extends AuroraComponent {
    private static final int SIZE = 30;

    BrandMark() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        getAccessibleContext().setAccessibleName("Kuala Lumpur Air and Parks");
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            double centreX = getWidth() / 2.0;
            double base = getHeight() - 5.0;
            for (int ring = 0; ring < 3; ring++) {
                double radius = 6.5 + ring * 5.4;
                g.setStroke(new BasicStroke(2.4f - ring * 0.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(arcColour(ring));
                g.draw(new Arc2D.Double(centreX - radius, base - radius, radius * 2, radius * 2, 25, 130, Arc2D.OPEN));
            }
            g.setColor(Skin.color(Tone.ACCENT));
            g.fill(new Ellipse2D.Double(centreX - 2.6, base - 2.6, 5.2, 5.2));
        } finally {
            g.dispose();
        }
    }

    private static java.awt.Color arcColour(int ring) {
        return switch (ring) {
            case 0 -> Skin.color(Tone.ACCENT);
            case 1 -> Paints.alpha(Skin.color(Tone.ACCENT), 170);
            default -> Paints.alpha(Skin.color(Tone.INFO), 150);
        };
    }
}
