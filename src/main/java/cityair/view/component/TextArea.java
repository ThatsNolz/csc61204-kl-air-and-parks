package cityair.view.component;

import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JTextArea;

/**
 * A multi-line editable field on an Aurora surface, used for the citizen report description.
 *
 * <p>It paints its own rounded background and keeps the invalid state visible until the content
 * becomes acceptable, so the message in the status bar and the field itself always agree.</p>
 */
public final class TextArea extends JTextArea implements Themed {
    private boolean invalid;

    /** Creates an editable area of the requested visible height in rows. */
    public TextArea(int rows) {
        super(rows, 0);
        setFont(Type.text(Type.BODY));
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(InputSurface.normal());
        addFocusListener(new FocusAdapter() {
            /** Repaints so the focus ring appears. */
            @Override public void focusGained(FocusEvent event) { repaint(); }
            /** Repaints so the focus ring disappears. */
            @Override public void focusLost(FocusEvent event) { repaint(); }
        });
        applyTheme();
    }

    /** Marks the field as holding content the application rejected. */
    public void setInvalid(boolean value) {
        if (invalid == value) return;
        invalid = value;
        setBorder(value ? InputSurface.invalid() : InputSurface.normal());
        repaint();
    }

    /** Returns true while the field is marked invalid. */
    public boolean isInvalid() { return invalid; }

    /** Re-reads the text, caret and selection colours. */
    @Override public void applyTheme() {
        setForeground(Skin.color(Tone.INK));
        setCaretColor(Skin.color(Tone.ACCENT));
        setSelectionColor(Skin.color(Tone.ACCENT_SOFT));
        setSelectedTextColor(Skin.color(Tone.INK));
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = Paints.quality((Graphics2D) graphics.create());
        try {
            g.setColor(Skin.color(isEditable() ? Tone.SURFACE_SUNKEN : Tone.SURFACE));
            g.fill(Paints.round(0, 0, getWidth(), getHeight(), Metrics.RADIUS_CONTROL));
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
