package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Themed;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * A screen body that scrolls vertically and always matches the width it is given.
 *
 * <p>An ordinary panel inside a scroll pane keeps its preferred width, so a window narrower than
 * the content clips the right-hand side instead of reflowing it. Implementing {@link Scrollable}
 * and tracking the viewport width makes the width available rather than preferred, which is what
 * lets the responsive grids and the wrapped paragraphs inside a screen reflow as the window
 * narrows. Height is still free, so a tall screen scrolls.</p>
 */
public final class ScrollableColumn extends JPanel implements Scrollable, Themed {
    /** Creates a transparent scrollable body with the given layout. */
    public ScrollableColumn(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    /** Returns the viewport size this content asks a scroll pane for. */
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    /** Returns the distance one scroll step moves. */
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return Metrics.SPACE_5;
    }

    /** Returns the distance one page step moves. */
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(Metrics.SPACE_8, visible.height - Metrics.SPACE_8);
    }

    /** True, so the content reflows to the window width instead of being clipped by it. */
    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    /** False, so a screen taller than the window scrolls rather than being compressed. */
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
