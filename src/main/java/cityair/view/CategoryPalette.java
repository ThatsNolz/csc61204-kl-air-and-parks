package cityair.view;

import cityair.model.AirCategory;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The colour of each U.S. AQI category in each theme.
 *
 * <p>Aurora Light uses the published U.S. EPA AirNow colours unchanged. Aurora Dark uses the same
 * six hues with their luminance adjusted, because the AirNow palette was defined for dark text on
 * a white page and its yellow and green are both glaring and low-contrast against a dark surface.
 * The hue, the order and the meaning are unchanged, every category is also written in words
 * wherever a colour appears, and the Data and impact screen states the adjustment.</p>
 *
 * <p>Colour lives in the view layer so the domain model stays free of Swing.</p>
 */
public final class CategoryPalette {
    private static final Map<AirCategory, Color> AIRNOW = new EnumMap<>(AirCategory.class);
    private static final Map<AirCategory, Color> NIGHT = new EnumMap<>(AirCategory.class);
    private static final Map<AirCategory, Color> LIGHT_INK = new EnumMap<>(AirCategory.class);

    static {
        AIRNOW.put(AirCategory.GOOD, new Color(0x00E400));
        AIRNOW.put(AirCategory.MODERATE, new Color(0xFFFF00));
        AIRNOW.put(AirCategory.SENSITIVE, new Color(0xFF7E00));
        AIRNOW.put(AirCategory.UNHEALTHY, new Color(0xFF0000));
        AIRNOW.put(AirCategory.VERY_UNHEALTHY, new Color(0x8F3F97));
        AIRNOW.put(AirCategory.HAZARDOUS, new Color(0x7E0023));
        NIGHT.put(AirCategory.GOOD, new Color(0x3AD277));
        NIGHT.put(AirCategory.MODERATE, new Color(0xEFC73C));
        NIGHT.put(AirCategory.SENSITIVE, new Color(0xF98C33));
        NIGHT.put(AirCategory.UNHEALTHY, new Color(0xF45A61));
        NIGHT.put(AirCategory.VERY_UNHEALTHY, new Color(0xB07AD6));
        NIGHT.put(AirCategory.HAZARDOUS, new Color(0xD0536F));
        LIGHT_INK.put(AirCategory.GOOD, new Color(0x118038));
        LIGHT_INK.put(AirCategory.MODERATE, new Color(0x8A6B00));
        LIGHT_INK.put(AirCategory.SENSITIVE, new Color(0xB35100));
        LIGHT_INK.put(AirCategory.UNHEALTHY, new Color(0xC31119));
        LIGHT_INK.put(AirCategory.VERY_UNHEALTHY, new Color(0x7B3482));
        LIGHT_INK.put(AirCategory.HAZARDOUS, new Color(0x7E0023));
    }

    private CategoryPalette() {}

    /** The category's colour for fills, bars and dial segments in the active theme. */
    public static Color fill(AirCategory category) {
        Objects.requireNonNull(category);
        return Skin.theme().isDark() ? NIGHT.get(category) : AIRNOW.get(category);
    }

    /**
     * The category's colour for thin strokes and small text. In the light theme this is a darker
     * variant of the same hue, because the AirNow yellow and green fail text contrast on white.
     */
    public static Color ink(AirCategory category) {
        Objects.requireNonNull(category);
        return Skin.theme().isDark() ? NIGHT.get(category) : LIGHT_INK.get(category);
    }

    /** Text colour with sufficient contrast when drawn on {@link #fill(AirCategory)}. */
    public static Color onFill(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD, MODERATE, SENSITIVE -> new Color(0x111111);
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> Skin.theme().isDark() ? new Color(0x111111) : Color.WHITE;
        };
    }

    /** The colour of the category an index belongs to, for charts that work in index values. */
    public static Color forIndex(int aqi) { return fill(AirCategory.from(Math.max(0, aqi))); }

    /** The stroke colour of the category an index belongs to. */
    public static Color inkForIndex(int aqi) { return ink(AirCategory.from(Math.max(0, aqi))); }

    /** The colour used where no verified category exists. */
    public static Color unknown() { return Skin.color(Tone.INK_FAINT); }
}
