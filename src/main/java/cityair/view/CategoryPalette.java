package cityair.view;

import cityair.model.AirCategory;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Official U.S. EPA AirNow colours for each AQI category, with a readable text colour for each.
 * Kept in the view layer so the domain model stays independent of Swing.
 */
final class CategoryPalette {
    private static final Map<AirCategory, Color> FILL = new EnumMap<>(AirCategory.class);
    private static final Color DARK_TEXT = new Color(0x111111);

    static {
        FILL.put(AirCategory.GOOD, new Color(0x00E400));
        FILL.put(AirCategory.MODERATE, new Color(0xFFFF00));
        FILL.put(AirCategory.SENSITIVE, new Color(0xFF7E00));
        FILL.put(AirCategory.UNHEALTHY, new Color(0xFF0000));
        FILL.put(AirCategory.VERY_UNHEALTHY, new Color(0x8F3F97));
        FILL.put(AirCategory.HAZARDOUS, new Color(0x7E0023));
    }

    private CategoryPalette() {}

    /** Background colour that identifies the category. */
    static Color fill(AirCategory category) { return FILL.get(Objects.requireNonNull(category)); }

    /** Text colour with sufficient contrast on {@link #fill(AirCategory)}. */
    static Color onFill(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD, MODERATE, SENSITIVE -> DARK_TEXT;
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> Color.WHITE;
        };
    }
}
