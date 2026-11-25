package cityair.view;

import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Motion;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Draws a left-placed {@link JTabbedPane} as the Aurora navigation rail.
 *
 * <p>Replacing the delegate rather than the component is a deliberate choice: the tabbed pane
 * keeps Swing's selection model, its mnemonics, its focus traversal and its accessibility role,
 * and the screens keep their indices, while every pixel of the rail is drawn here. The rail groups
 * its destinations under quiet captions, marks the active one with an indicator that slides rather
 * than jumps, and can collapse to icons when the window is narrow.</p>
 */
public final class NavigationRailUI extends BasicTabbedPaneUI {
    private static final int ITEM_HEIGHT = 44, GROUP_HEADER = 32, TOP_SPACE = 12, INDICATOR = 3;
    private static final int ICON_X = 22, TEXT_X = 54, RADIUS = 10;
    private static final Map<Integer, String> GROUPS = Map.of(0, "Monitor", 2, "Plan and respond", 5, "Reference");
    private static final List<Glyph> ICONS = List.of(Glyph.OVERVIEW, Glyph.PULSE, Glyph.PLANNER,
            Glyph.REPORTS, Glyph.BELL, Glyph.DATABASE, Glyph.SETTINGS);
    private boolean collapsed;
    private double indicator = -1;
    private int hovered = -1;
    private transient Motion motion;

    /** Switches between the full rail and the icon-only rail. */
    public void setCollapsed(boolean value) {
        if (collapsed == value) return;
        collapsed = value;
        tabPane.revalidate();
        tabPane.repaint();
    }

    /** Returns true while the rail is showing icons only. */
    public boolean isCollapsed() { return collapsed; }

    /** Returns the width the rail currently occupies. */
    public int width() { return collapsed ? Metrics.RAIL_WIDTH_COLLAPSED : Metrics.RAIL_WIDTH; }

    @Override protected void installDefaults() {
        super.installDefaults();
        // For LEFT placement BasicTabbedPaneUI rotates these: "left" is the space above the first item.
        tabAreaInsets = new Insets(0, TOP_SPACE, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabInsets = new Insets(0, 0, 0, 0);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
    }

    @Override protected void installListeners() {
        super.installListeners();
        var pointer = new MouseAdapter() {
            /** Tracks the pointer so the component under it can show its hovered state. */
            @Override public void mouseMoved(MouseEvent event) { setHovered(tabForCoordinate(tabPane, event.getX(), event.getY())); }

            /** Clears the hovered state when the pointer leaves. */
            @Override public void mouseExited(MouseEvent event) { setHovered(-1); }
        };
        tabPane.addMouseMotionListener(pointer);
        tabPane.addMouseListener(pointer);
    }

    @Override protected int calculateTabWidth(int placement, int index, FontMetrics metrics) { return width(); }

    @Override protected int calculateTabHeight(int placement, int index, int fontHeight) {
        return ITEM_HEIGHT + (GROUPS.containsKey(index) ? GROUP_HEADER : 0);
    }

    @Override protected void paintTabArea(Graphics graphics, int placement, int selectedIndex) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setPaint(Paints.vertical(0, tabPane.getHeight(), Skin.color(Tone.RAIL), Skin.color(Tone.RAIL_EDGE)));
            g.fillRect(0, 0, width(), tabPane.getHeight());
            g.setColor(Skin.color(Tone.STROKE));
            g.fillRect(width() - 1, 0, 1, tabPane.getHeight());
        } finally {
            g.dispose();
        }
        moveIndicator(selectedIndex);
        paintIndicator(graphics, selectedIndex);
        super.paintTabArea(graphics, placement, selectedIndex);
    }

    private void paintIndicator(Graphics graphics, int selectedIndex) {
        if (indicator < 0 || selectedIndex < 0) return;
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var item = itemBounds(selectedIndex);
            g.setColor(Skin.color(Tone.RAIL_ACTIVE));
            g.fill(Paints.round(6, indicator, width() - 12.0, item.height, RADIUS));
            g.setColor(Skin.color(Tone.ACCENT));
            g.fill(Paints.round(6, indicator + 9, INDICATOR, item.height - 18.0, INDICATOR));
        } finally {
            g.dispose();
        }
    }

    @Override protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y,
                                                int w, int h, boolean selected) {
        if (selected || index != hovered) return;
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var item = itemBounds(index);
            g.setColor(Paints.alpha(Skin.color(Tone.RAIL_ACTIVE), 150));
            g.fill(Paints.round(6, item.y, width() - 12.0, item.height, RADIUS));
        } finally {
            g.dispose();
        }
    }

    @Override protected void paintText(Graphics graphics, int placement, Font font, FontMetrics metrics,
                                       int index, String title, Rectangle textRect, boolean selected) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            paintGroupCaption(g, index);
            var item = itemBounds(index);
            Color ink = Skin.color(selected ? Tone.RAIL_INK : Tone.RAIL_INK_MUTED);
            g.setColor(index == hovered && !selected ? Skin.color(Tone.RAIL_INK) : ink);
            ICONS.get(Math.min(index, ICONS.size() - 1)).paint(g, iconX(), item.y + (item.height - Icons.MEDIUM) / 2.0,
                    Icons.MEDIUM);
            if (collapsed) return;
            paintTitle(g, index, title, item, selected);
        } finally {
            g.dispose();
        }
    }

    private void paintTitle(Graphics2D g, int index, String title, Rectangle item, boolean selected) {
        var font = selected ? Type.strong(Type.BODY) : Type.text(Type.BODY);
        g.setFont(font);
        var metrics = g.getFontMetrics(font);
        g.setColor(Skin.color(selected ? Tone.RAIL_INK : Tone.RAIL_INK_MUTED));
        if (index == hovered && !selected) g.setColor(Skin.color(Tone.RAIL_INK));
        g.drawString(title, TEXT_X, item.y + item.height / 2 + (metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private void paintGroupCaption(Graphics2D g, int index) {
        String caption = GROUPS.get(index);
        if (caption == null || collapsed) return;
        var font = Type.caption();
        g.setFont(font);
        g.setColor(Paints.alpha(Skin.color(Tone.RAIL_INK_MUTED), 170));
        g.drawString(caption, ICON_X, rects[index].y + GROUP_HEADER - 12);
    }

    private int iconX() {
        return collapsed ? (width() - Icons.MEDIUM) / 2 : ICON_X;
    }

    private Rectangle itemBounds(int index) {
        Rectangle tab = rects[index];
        int offset = GROUPS.containsKey(index) ? GROUP_HEADER : 0;
        return new Rectangle(tab.x, tab.y + offset, tab.width, tab.height - offset);
    }

    private void moveIndicator(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= rects.length) return;
        double target = itemBounds(selectedIndex).y;
        if (indicator < 0) {
            indicator = target;
            return;
        }
        if (Math.abs(indicator - target) < 0.5 || (motion != null && motion.isRunning())) return;
        double from = indicator;
        motion = Motion.play(Metrics.MOTION_BASE, progress -> {
            indicator = from + (target - from) * Motion.easeOut(progress);
            tabPane.repaint();
        });
    }

    private void setHovered(int index) {
        if (hovered == index) return;
        hovered = index;
        tabPane.setCursor(java.awt.Cursor.getPredefinedCursor(
                index < 0 ? java.awt.Cursor.DEFAULT_CURSOR : java.awt.Cursor.HAND_CURSOR));
        tabPane.repaint();
    }

    @Override protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
                                            boolean selected) {
        // The rail separates its items with space and an indicator rather than with lines.
    }

    @Override protected void paintFocusIndicator(Graphics graphics, int placement, Rectangle[] bounds, int index,
                                                 Rectangle iconRect, Rectangle textRect, boolean selected) {
        if (!tabPane.hasFocus() || !selected) return;
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            var item = itemBounds(index);
            g.setStroke(new java.awt.BasicStroke(Metrics.STROKE_EMPHASIS));
            g.setColor(Skin.color(Tone.FOCUS));
            g.draw(Paints.round(6, item.y + 1.0, width() - 13.0, item.height - 2.0, RADIUS));
        } finally {
            g.dispose();
        }
    }

    @Override protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
        // The content area sits directly on the application backdrop.
    }
}
