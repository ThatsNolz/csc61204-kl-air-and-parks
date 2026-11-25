package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A slim scrollbar: no stepper buttons, a transparent track and a rounded thumb that darkens
 * under the pointer. Replacing the delegate rather than the component keeps all of Swing's
 * scrolling behaviour, including the mouse wheel and keyboard paging.
 */
public final class AuroraScrollBarUI extends BasicScrollBarUI {
    private static final int THICKNESS = 11, THUMB_INSET = 3, MINIMUM_THUMB = 34;

    /** Applies the Aurora scrollbar to one scroll bar. */
    public static void install(JScrollBar bar) {
        bar.setUI(new AuroraScrollBarUI());
        bar.setOpaque(false);
        bar.setUnitIncrement(Metrics.SPACE_4);
        bar.setBlockIncrement(Metrics.SPACE_8 * 3);
        bar.setPreferredSize(new Dimension(THICKNESS, THICKNESS));
    }

    @Override protected JButton createIncreaseButton(int orientation) { return hiddenButton(); }

    @Override protected JButton createDecreaseButton(int orientation) { return hiddenButton(); }

    @Override protected Dimension getMinimumThumbSize() { return new Dimension(MINIMUM_THUMB, MINIMUM_THUMB); }

    @Override protected void paintTrack(Graphics g, JComponent host, Rectangle bounds) {
        // The track stays transparent so a scrollbar never draws a bar across a card.
    }

    @Override protected void paintThumb(Graphics graphics, JComponent host, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setColor(thumbColour());
            g.fill(Paints.round(bounds.x + THUMB_INSET, bounds.y + THUMB_INSET,
                    bounds.width - THUMB_INSET * 2.0, bounds.height - THUMB_INSET * 2.0, Metrics.RADIUS_PILL));
        } finally {
            g.dispose();
        }
    }

    private Color thumbColour() {
        if (isDragging) return Skin.color(Tone.ACCENT);
        return Paints.alpha(Skin.color(Tone.INK_FAINT), isThumbRollover() ? 190 : 120);
    }

    private static JButton hiddenButton() {
        var button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        return button;
    }
}
