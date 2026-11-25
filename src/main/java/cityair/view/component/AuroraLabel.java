package cityair.view.component;

import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JLabel;

/**
 * A label that remembers the semantic role of its text instead of a literal colour, so it can
 * re-read the active theme when the user switches between Aurora Dark and Aurora Light.
 */
public final class AuroraLabel extends JLabel implements Themed {
    private final transient Tone tone;

    /** Creates a label with the given text, font and semantic colour role. */
    public AuroraLabel(String text, Font font, Tone tone) {
        super(text);
        this.tone = Objects.requireNonNull(tone);
        setFont(font);
        applyTheme();
    }

    /** Returns the semantic colour role this label paints its text with. */
    public Tone tone() { return tone; }

    /** Re-reads the text colour for this label's semantic role. */
    @Override public void applyTheme() { setForeground(Skin.color(tone)); }
}
