package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 * The rounded, focus-aware frame drawn around every editable field.
 *
 * <p>It is implemented as a {@link Border} rather than a panel so it can be attached to any text
 * component without changing the layout, and because a border is painted after the component's
 * content it can draw the focus ring on top rather than behind the text.</p>
 */
public final class InputSurface implements Border {
    private static final int PAD_X = 12, PAD_Y = 9;
    private final boolean error;

    private InputSurface(boolean error) { this.error = error; }

    /** Frame for a field in its ordinary state. */
    public static InputSurface normal() { return new InputSurface(false); }

    /** Frame for a field whose current content was rejected. */
    public static InputSurface invalid() { return new InputSurface(true); }

    /** Draws the rounded outline, in its focused, invalid or resting colour. */
    @Override public void paintBorder(Component host, Graphics graphics, int x, int y, int width, int height) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var shape = Paints.round(x + 0.5, y + 0.5, width - 1, height - 1, Metrics.RADIUS_CONTROL);
            boolean focused = host.isFocusOwner();
            g.setStroke(new BasicStroke(focused || error ? Metrics.STROKE_EMPHASIS : Metrics.STROKE_HAIRLINE));
            g.setColor(Skin.color(outline(focused)));
            g.draw(shape);
        } finally {
            g.dispose();
        }
    }

    private Tone outline(boolean focused) {
        if (error) return Tone.DANGER;
        return focused ? Tone.FOCUS : Tone.STROKE;
    }

    /** Returns the padding the frame reserves around the field's text. */
    @Override public Insets getBorderInsets(Component host) { return new Insets(PAD_Y, PAD_X, PAD_Y, PAD_X); }

    /** Returns false: the frame draws an outline and never fills its area. */
    @Override public boolean isBorderOpaque() { return false; }
}
