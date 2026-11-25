package cityair.view.component;

import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.util.Locale;
import javax.swing.JComponent;

/**
 * Factory for the application's text styles. Screens choose a role, such as a screen title or a
 * caption, rather than a font and a colour, which keeps the typographic hierarchy identical
 * everywhere and makes a change to the scale a single edit.
 */
public final class Text {
    private Text() {}

    /** Screen title, one per screen. */
    public static AuroraLabel title(String value) {
        return new AuroraLabel(value, Type.strong(Type.TITLE), Tone.INK);
    }

    /** Card or section heading. */
    public static AuroraLabel heading(String value) {
        return new AuroraLabel(value, Type.strong(Type.HEADING), Tone.INK);
    }

    /** Uppercase tracked caption placed above a value or a group of controls. */
    public static AuroraLabel caption(String value) {
        return new AuroraLabel(value.toUpperCase(Locale.ENGLISH), Type.caption(), Tone.INK_MUTED);
    }

    /** Ordinary single-line body text. */
    public static AuroraLabel body(String value) {
        return new AuroraLabel(value, Type.text(Type.BODY), Tone.INK);
    }

    /** Secondary single-line text. */
    public static AuroraLabel muted(String value) {
        return new AuroraLabel(value, Type.text(Type.BODY), Tone.INK_MUTED);
    }

    /** Footnote-sized secondary text. */
    public static AuroraLabel micro(String value) {
        return new AuroraLabel(value, Type.text(Type.MICRO), Tone.INK_FAINT);
    }

    /** Emphasised measurement, in the even-width numeric family. */
    public static AuroraLabel value(String value) {
        return new AuroraLabel(value, Type.numberStrong(Type.VALUE), Tone.INK);
    }

    /** Measurement at body size, used in dense rows. */
    public static AuroraLabel number(String value) {
        return new AuroraLabel(value, Type.number(Type.LABEL), Tone.INK);
    }

    /** Wrapped body text that reflows with its container. */
    public static Paragraph paragraph(String value) {
        return new Paragraph(value, Tone.INK);
    }

    /** Wrapped secondary text, for explanations and provenance notes. */
    public static Paragraph note(String value) {
        return new Paragraph(value, Tone.INK_MUTED, Type.LABEL);
    }

    /** Names a component for accessibility and for the integration tests that drive it. */
    public static <T extends JComponent> T named(T control, String name) {
        control.setName(name);
        control.getAccessibleContext().setAccessibleName(name);
        return control;
    }

    /** Names a component and gives assistive technology a sentence describing its purpose. */
    public static <T extends JComponent> T described(T control, String name, String description) {
        named(control, name).getAccessibleContext().setAccessibleDescription(description);
        return control;
    }
}
