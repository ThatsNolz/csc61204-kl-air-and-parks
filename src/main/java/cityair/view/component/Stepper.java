package cityair.view.component;

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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSpinnerUI;

/**
 * A numeric field with increment and decrement controls, used for the alert threshold.
 *
 * <p>It stays a {@link JSpinner} so the bounded model keeps rejecting values outside the allowed
 * range before the controller ever sees them, and so the field remains editable by typing as well
 * as by stepping.</p>
 */
public final class Stepper extends JSpinner implements Themed {
    private static final int BUTTON_WIDTH = 30, ARROW = 8;

    /** Creates a stepper over an inclusive range. */
    public Stepper(int value, int minimum, int maximum) {
        super(new SpinnerNumberModel(value, minimum, maximum, 1));
        setUI(new StepperUI());
        setOpaque(false);
        setBorder(InputSurface.normal());
        setPreferredSize(new Dimension(130, Metrics.CONTROL_HEIGHT + 4));
        applyTheme();
    }

    /** Re-reads the editor's colours and keeps its surface transparent. */
    @Override public void applyTheme() {
        var editor = (DefaultEditor) getEditor();
        // The editor is a panel wrapping a text field, and both are opaque by default. Left
        // alone, the panel paints the platform's white field over the Aurora surface.
        editor.setOpaque(false);
        editor.setBackground(Skin.color(Tone.SURFACE_SUNKEN));
        var field = editor.getTextField();
        field.setFont(Type.numberStrong(Type.HEADING));
        field.setForeground(Skin.color(Tone.INK));
        field.setBackground(Skin.color(Tone.SURFACE_SUNKEN));
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, Metrics.SPACE_3, 0, 0));
        field.setCaretColor(Skin.color(Tone.ACCENT));
        field.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setColor(Skin.color(Tone.SURFACE_SUNKEN));
            g.fill(Paints.round(0, 0, getWidth(), getHeight(), Metrics.RADIUS_CONTROL));
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    /** Replaces Swing's stepper buttons with Aurora arrows. */
    private static final class StepperUI extends BasicSpinnerUI {
        @Override protected Component createNextButton() { return arrow(true, "Increase"); }

        @Override protected Component createPreviousButton() { return arrow(false, "Decrease"); }

        private Component arrow(boolean up, String name) {
            var button = new ArrowButton(up);
            button.getAccessibleContext().setAccessibleName(name);
            installButtonListeners(button, up);
            return button;
        }

        private void installButtonListeners(JButton button, boolean up) {
            if (up) super.installNextButtonListeners(button);
            else super.installPreviousButtonListeners(button);
        }
    }

    /** One triangular step control. */
    private static final class ArrowButton extends JButton {
        private final boolean up;

        ArrowButton(boolean up) {
            this.up = up;
            setOpaque(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusable(false);
            setPreferredSize(new Dimension(BUTTON_WIDTH, Metrics.CONTROL_HEIGHT / 2));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                paintArrow(g, (JComponent) this);
            } finally {
                g.dispose();
            }
        }

        private void paintArrow(Graphics2D g, JComponent host) {
            double centreX = host.getWidth() / 2.0;
            double centreY = host.getHeight() / 2.0;
            double tip = up ? -ARROW / 3.0 : ARROW / 3.0;
            var path = new Path2D.Double();
            path.moveTo(centreX - ARROW / 2.0, centreY - tip);
            path.lineTo(centreX, centreY + tip);
            path.lineTo(centreX + ARROW / 2.0, centreY - tip);
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Skin.color(getModel().isRollover() ? Tone.ACCENT : Tone.INK_MUTED));
            g.draw(path);
        }
    }
}
