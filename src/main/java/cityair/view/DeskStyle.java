package cityair.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Shared desktop presentation tokens and component factories. */
final class DeskStyle {
    static final Color CANVAS = new Color(0xEEF4F5), SURFACE = Color.WHITE;
    static final Color NAVY = new Color(0x132F3B), TEXT = new Color(0x173541);
    static final Color MUTED = new Color(0x4B6570), TEAL = new Color(0x00685B);
    static final Color LINE = new Color(0xCEDDDF), PALE = new Color(0xE3F2EF);
    static final Color ERROR_TEXT = new Color(0x962B35), AMBER = new Color(0x7B5200);
    static final int GAP = 14, PAD = 20, BODY = 15, TITLE = 26, HERO = 48, ROW = 30;

    private DeskStyle() {}

    static JLabel label(String text, int size, int weight, Color foreground) {
        var label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, weight, size));
        label.setForeground(foreground);
        return label;
    }

    static JPanel panel(LayoutManager layout) {
        var panel = new JPanel(layout);
        panel.setBackground(CANVAS);
        return panel;
    }

    static JPanel card(LayoutManager layout) {
        var panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                new EmptyBorder(GAP, GAP, GAP, GAP)));
        return panel;
    }

    static JScrollPane scroll(Component content) {
        var scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    static void styleButton(AbstractButton button, boolean primary) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, BODY));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBackground(primary ? TEAL : SURFACE);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setMargin(new Insets(GAP / 2, GAP, GAP / 2, GAP));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static JPanel section(String title, String subtitle, JComponent content) {
        var panel = panel(new BorderLayout(GAP, GAP));
        panel.setBorder(new EmptyBorder(GAP, 0, 0, 0));
        var heading = panel(new GridLayout(0, 1));
        heading.add(label(title, TITLE, Font.BOLD, TEXT));
        heading.add(label(subtitle, BODY, Font.PLAIN, MUTED));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    static void table(JTable table) {
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, BODY));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, BODY));
        table.getTableHeader().setBackground(PALE);
        table.setRowHeight(ROW);
        table.setGridColor(LINE);
        table.setFillsViewportHeight(true);
    }
}
