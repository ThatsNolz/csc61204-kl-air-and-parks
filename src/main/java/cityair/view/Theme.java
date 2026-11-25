package cityair.view;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/** Design tokens (colour, type, spacing) and component factories shared by every screen. */
final class Theme {
    static final Color CANVAS = new Color(0xF2F5F7), SURFACE = Color.WHITE;
    static final Color RAIL = new Color(0x0F2A36), RAIL_SELECTED = new Color(0x1D4352);
    static final Color TEXT = new Color(0x14303B), MUTED = new Color(0x5A707A);
    static final Color TEAL = new Color(0x00796B), TEAL_DARK = new Color(0x005B50);
    static final Color LINE = new Color(0xD9E2E6), PALE = new Color(0xE6F2F0);
    static final Color ERROR_TEXT = new Color(0xA1262F), ERROR_FILL = new Color(0xFDECEC);
    static final Color AMBER = new Color(0x8A5A00), AMBER_FILL = new Color(0xFFF4E0);
    static final Color ON_DARK = new Color(0xE3F2EF);
    static final int GAP = 12, PAD = 20, SMALL = 12, BODY = 14, HEADING = 16, TITLE = 24, HERO = 64, ROW = 30;
    private static final String FONT = pickFont("Segoe UI", "Inter", "Helvetica Neue", "Arial");

    private Theme() {}

    private static String pickFont(String... preferred) {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return Arrays.stream(preferred).filter(installed::contains).findFirst().orElse(Font.SANS_SERIF);
    }

    static Font font(int size, int style) { return new Font(FONT, style, size); }

    static JLabel label(String text, int size, int style, Color foreground) {
        var label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(foreground);
        return label;
    }

    /** Uppercase caption used above values and inside cards. */
    static JLabel caption(String text) {
        return label(text.toUpperCase(java.util.Locale.ENGLISH), SMALL, Font.BOLD, MUTED);
    }

    static JPanel panel(LayoutManager layout) {
        var panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel card(LayoutManager layout) {
        var panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(cardBorder(LINE));
        return panel;
    }

    /** Card with a coloured strip on its left edge, used to signal state or category. */
    static Border accentBorder(Color accent) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createCompoundBorder(new MatteBorder(0, 4, 0, 0, accent), new EmptyBorder(PAD - 1, PAD - 4, PAD - 1, PAD)));
    }

    static Border cardBorder(Color line) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(line), new EmptyBorder(PAD, PAD, PAD, PAD));
    }

    /** Titled card: an uppercase caption above the supplied content. */
    static JPanel titled(String title, JComponent content) {
        var block = card(new BorderLayout(0, GAP));
        block.add(caption(title), BorderLayout.NORTH);
        block.add(content, BorderLayout.CENTER);
        return block;
    }

    static JScrollPane scroll(Component content) {
        var scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(BODY);
        return scroll;
    }

    static void styleButton(AbstractButton button, boolean primary) {
        button.setFont(font(BODY, Font.BOLD));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBackground(primary ? TEAL : SURFACE);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setFocusPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? TEAL_DARK : LINE), new EmptyBorder(9, 18, 9, 18)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static <T> void styleCombo(JComboBox<T> combo) {
        combo.setFont(font(BODY, Font.PLAIN));
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
    }

    /** Screen heading: title and one-line purpose statement above the screen's content. */
    static JPanel screen(String title, String subtitle, JComponent content) {
        var screen = panel(new BorderLayout(0, PAD));
        screen.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));
        var heading = panel(new GridLayout(0, 1, 0, 2));
        heading.add(label(title, TITLE, Font.BOLD, TEXT));
        heading.add(label(subtitle, BODY, Font.PLAIN, MUTED));
        screen.add(heading, BorderLayout.NORTH);
        screen.add(content, BorderLayout.CENTER);
        return screen;
    }

    /** Read-only wrapped text that participates in accessibility and component lookup. */
    static JTextArea text(String name) {
        var field = named(new JTextArea(), name);
        field.setEditable(false);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setFont(font(BODY, Font.PLAIN));
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setBorder(new EmptyBorder(0, 0, 0, 0));
        return field;
    }

    static <T extends JComponent> T named(T control, String name) {
        control.setName(name);
        control.getAccessibleContext().setAccessibleName(name);
        return control;
    }

    static void styleTable(JTable table) {
        table.setFont(font(BODY, Font.PLAIN));
        table.setForeground(TEXT);
        table.getTableHeader().setFont(font(SMALL, Font.BOLD));
        table.getTableHeader().setBackground(PALE);
        table.getTableHeader().setForeground(TEXT);
        table.setRowHeight(ROW);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setSelectionBackground(PALE);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
    }
}
