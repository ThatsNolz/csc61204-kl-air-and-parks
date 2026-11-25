package cityair.view.component;

import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.Dimension;
import java.util.Objects;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import javax.swing.border.EmptyBorder;

/**
 * Wrapped read-only body text.
 *
 * <p>Swing's usual shortcut for wrapped text is HTML inside a {@code JLabel}, but that requires a
 * pixel width to be written into the markup, which stops the text reflowing when the window is
 * resized and hard-codes colours that a theme change cannot reach. This component wraps to whatever
 * width its container gives it, reports the height that width needs, and follows the theme. It also
 * keeps the text selectable and visible to assistive technology.</p>
 */
public final class Paragraph extends JTextArea implements Themed {
    private static final int MINIMUM_WIDTH = 60;
    private final transient Tone tone;

    /** Creates body text in the given semantic colour role. */
    public Paragraph(String text, Tone tone) {
        this(text, tone, Type.BODY);
    }

    /** Creates wrapped text at an explicit size, for captions and footnotes. */
    public Paragraph(String text, Tone tone, float size) {
        super(text);
        this.tone = Objects.requireNonNull(tone);
        setFont(Type.text(size));
        setEditable(false);
        setFocusable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        // A caret that follows new text would scroll the whole screen to this paragraph.
        ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        applyTheme();
    }

    /** Reports the height the current width needs, so wrapped text is never clipped. */
    @Override public Dimension getPreferredSize() {
        int width = getWidth();
        if (width <= MINIMUM_WIDTH) return super.getPreferredSize();
        var view = getUI().getRootView(this);
        view.setSize(width - getInsets().left - getInsets().right, Integer.MAX_VALUE);
        int height = (int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS));
        return new Dimension(width, height + getInsets().top + getInsets().bottom);
    }

    /** Re-reads the text colour for the active theme. */
    @Override public void applyTheme() {
        setForeground(Skin.color(tone));
        setDisabledTextColor(Skin.color(Tone.INK_FAINT));
    }
}
