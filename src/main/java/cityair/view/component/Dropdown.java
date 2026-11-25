package cityair.view.component;

import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * A drop-down built on {@link JComboBox}, so the model, keyboard handling, type-ahead and
 * accessibility role are Swing's, while the field, the chevron and the popup are Aurora's.
 *
 * @param <T> the type of the values offered
 */
public final class Dropdown<T> extends JComboBox<T> implements Themed {
    private static final int PADDING = 14, CHEVRON = 9;

    /** Creates a drop-down over the given values. */
    public Dropdown(T[] values) {
        super(values);
        setUI(new FieldUI());
        setFont(Type.text(Type.BODY));
        setRenderer(new Renderer());
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(220, Metrics.CONTROL_HEIGHT));
        applyTheme();
    }

    /** Re-reads the field's text and surface colours. */
    @Override public void applyTheme() {
        setForeground(Skin.color(Tone.INK));
        setBackground(Skin.color(Tone.SURFACE_RAISED));
        repaint();
    }

    /** Paints the closed field: a rounded surface, a focus ring and a chevron. */
    private final class FieldUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            var button = new JButton();
            button.setVisible(false);
            return button;
        }

        /** Draws the closed field: its surface, its focus ring, its label and its chevron. */
        @Override public void paint(Graphics graphics, JComponent host) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                var shape = Paints.round(0.5, 0.5, host.getWidth() - 1, host.getHeight() - 1, Metrics.RADIUS_CONTROL);
                g.setColor(Skin.color(isEnabled() ? Tone.SURFACE_RAISED : Tone.SURFACE_SUNKEN));
                g.fill(shape);
                g.setStroke(new BasicStroke(hasFocus ? Metrics.STROKE_EMPHASIS : Metrics.STROKE_HAIRLINE));
                g.setColor(Skin.color(hasFocus ? Tone.FOCUS : Tone.STROKE));
                g.draw(shape);
                paintLabel(g, host);
                paintChevron(g, host);
            } finally {
                g.dispose();
            }
        }

        private void paintLabel(Graphics2D g, JComponent host) {
            Object value = getSelectedItem();
            if (value == null) return;
            var metrics = g.getFontMetrics(getFont());
            g.setFont(getFont());
            g.setColor(Skin.color(isEnabled() ? Tone.INK : Tone.INK_FAINT));
            int baseline = host.getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.drawString(metrics.stringWidth(value.toString()) > host.getWidth() - PADDING * 3
                    ? clip(value.toString(), metrics, host.getWidth() - PADDING * 3) : value.toString(), PADDING, baseline);
        }

        private String clip(String text, java.awt.FontMetrics metrics, int limit) {
            var shortened = new StringBuilder(text);
            while (shortened.length() > 1 && metrics.stringWidth(shortened + "…") > limit) {
                shortened.deleteCharAt(shortened.length() - 1);
            }
            return shortened + "…";
        }

        private void paintChevron(Graphics2D g, JComponent host) {
            double x = host.getWidth() - PADDING - CHEVRON;
            double y = host.getHeight() / 2.0 - CHEVRON / 4.0;
            var chevron = new Path2D.Double();
            chevron.moveTo(x, y - 1);
            chevron.lineTo(x + CHEVRON / 2.0, y + CHEVRON / 2.0);
            chevron.lineTo(x + CHEVRON, y - 1);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Skin.color(Tone.INK_MUTED));
            g.draw(chevron);
        }

        @SuppressWarnings("unchecked")
        @Override protected ComboPopup createPopup() {
            var popup = new BasicComboPopup((JComboBox<Object>) comboBox);
            popup.setBorder(BorderFactory.createLineBorder(Skin.color(Tone.STROKE), 1));
            popup.getList().setBackground(Skin.color(Tone.SURFACE_RAISED));
            popup.getList().setSelectionBackground(Skin.color(Tone.ACCENT_SOFT));
            popup.getList().setSelectionForeground(Skin.color(Tone.INK));
            return popup;
        }
    }

    /** Draws one option in the open popup. */
    private static final class Renderer extends DefaultListCellRenderer {
        /** Prepares this renderer for one option and returns it. */
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            setFont(Type.text(Type.BODY));
            setBorder(new EmptyBorder(Metrics.SPACE_2, PADDING, Metrics.SPACE_2, PADDING));
            setBackground(selected ? Skin.color(Tone.ACCENT_SOFT) : Skin.color(Tone.SURFACE_RAISED));
            setForeground(Skin.color(Tone.INK));
            setIcon(selected ? Icons.of(Glyph.CHECK, Icons.SMALL, Tone.ACCENT) : null);
            setIconTextGap(Metrics.SPACE_2);
            return this;
        }
    }
}
