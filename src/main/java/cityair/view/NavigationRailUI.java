package cityair.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import static cityair.view.Theme.*;

/**
 * Renders a left-placed {@link JTabbedPane} as a dark navigation rail. Keeping the standard
 * tabbed pane preserves keyboard mnemonics, accessibility and the existing tab indices.
 */
final class NavigationRailUI extends BasicTabbedPaneUI {
    private static final int RAIL_WIDTH = 196, ITEM_HEIGHT = 46, INDICATOR = 4, TOP_SPACE = 18;

    @Override protected void installDefaults() {
        super.installDefaults();
        // BasicTabbedPaneUI rotates these insets for LEFT placement: "left" becomes the space above the first tab.
        tabAreaInsets = new Insets(0, TOP_SPACE, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabInsets = new Insets(0, PAD, 0, PAD);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
    }

    @Override protected int calculateTabWidth(int placement, int index, FontMetrics metrics) { return RAIL_WIDTH; }

    @Override protected int calculateTabHeight(int placement, int index, int fontHeight) { return ITEM_HEIGHT; }

    @Override protected void paintTabArea(Graphics g, int placement, int selectedIndex) {
        g.setColor(RAIL);
        g.fillRect(0, 0, RAIL_WIDTH, tabPane.getHeight());
        super.paintTabArea(g, placement, selectedIndex);
    }

    @Override protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h,
                                                boolean selected) {
        if (!selected) return;
        g.setColor(RAIL_SELECTED);
        g.fillRect(x, y, w, h);
        g.setColor(TEAL);
        g.fillRect(x, y, INDICATOR, h);
    }

    @Override protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics, int index,
                                       String title, Rectangle textRect, boolean selected) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(selected ? Color.WHITE : ON_DARK);
        Rectangle tab = rects[index];
        int baseline = tab.y + (tab.height + metrics.getAscent() - metrics.getDescent()) / 2;
        g.drawString(title, tab.x + PAD, baseline);
    }

    @Override protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
                                            boolean selected) { }

    @Override protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index,
                                                 Rectangle iconRect, Rectangle textRect, boolean selected) {
        if (!tabPane.hasFocus() || !selected) return;
        g.setColor(TEAL);
        Rectangle tab = rects[index];
        g.drawRect(tab.x + INDICATOR + 2, tab.y + 2, tab.width - INDICATOR - 5, tab.height - 5);
    }

    @Override protected void paintContentBorder(Graphics g, int placement, int selectedIndex) { }
}
