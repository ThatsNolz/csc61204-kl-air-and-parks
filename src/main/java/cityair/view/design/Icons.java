package cityair.view.design;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.Icon;

/** Adapts a {@link Glyph} to the Swing {@link Icon} contract used by labels, buttons and tabs. */
public final class Icons {
    /** Side of an icon placed beside body text. */
    public static final int SMALL = 16;
    /** Side of an icon inside a control or a navigation item. */
    public static final int MEDIUM = 20;
    /** Side of an icon that leads a card or an empty state. */
    public static final int LARGE = 28;

    private Icons() {}

    /** Icon that always paints in the given semantic role, so it follows a theme change. */
    public static Icon of(Glyph glyph, int size, Tone tone) {
        Objects.requireNonNull(tone);
        return of(glyph, size, () -> Skin.color(tone));
    }

    /** Icon whose colour is resolved at paint time by the supplied function. */
    public static Icon of(Glyph glyph, int size, Supplier<Color> colour) {
        return new GlyphIcon(Objects.requireNonNull(glyph), size, Objects.requireNonNull(colour));
    }

    private record GlyphIcon(Glyph glyph, int size, Supplier<Color> colour) implements Icon {
        /** Paints the glyph at the requested position in the colour resolved now. */
        @Override public void paintIcon(Component host, Graphics g, int x, int y) {
            var graphics = (Graphics2D) g.create();
            try {
                graphics.setColor(colour.get());
                glyph.paint(graphics, x, y, size);
            } finally {
                graphics.dispose();
            }
        }

        /** Returns the icon's width in pixels. */
        @Override public int getIconWidth() { return size; }

        /** Returns the icon's height in pixels. */
        @Override public int getIconHeight() { return size; }
    }
}
