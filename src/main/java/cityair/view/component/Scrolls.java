package cityair.view.component;

import cityair.view.design.Metrics;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/** Builds scroll panes that keep the Aurora surfaces visible and never draw a frame of their own. */
public final class Scrolls {
    private Scrolls() {}

    /** Vertical scrolling for content that can exceed the height of a screen. */
    public static JScrollPane vertical(Component content) {
        var scroll = of(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    /**
     * Scrolling in both directions, for a table that is wider than its card. A table is the one
     * place horizontal scrolling is acceptable, because its columns cannot wrap.
     */
    public static JScrollPane of(Component content) {
        var scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(Metrics.SPACE_4);
        AuroraScrollBarUI.install(scroll.getVerticalScrollBar());
        AuroraScrollBarUI.install(scroll.getHorizontalScrollBar());
        return scroll;
    }
}
