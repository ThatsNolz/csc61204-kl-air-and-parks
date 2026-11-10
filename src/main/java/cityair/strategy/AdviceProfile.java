package cityair.strategy;

import cityair.model.AirCategory;
import java.util.Objects;

/** Built-in selection of independent activity advice rules. */
public enum AdviceProfile {
    GENERAL("General public", AdviceProfile::general),
    CHILDREN("Children", AdviceProfile::children),
    OLDER_ADULTS("Older adults", AdviceProfile::olderAdults);

    private final String label;
    private final AdviceStrategy strategy;

    AdviceProfile(String label, AdviceStrategy strategy) {
        this.label = label;
        this.strategy = strategy;
    }

    /** Returns the selected strategy. */
    public AdviceStrategy strategy() { return strategy; }

    /** Returns the label used by the view. */
    @Override public String toString() { return label; }

    private static String general(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD, MODERATE, SENSITIVE -> "Routine outdoor activity is generally reasonable. Recheck conditions before sustained exertion.";
            case UNHEALTHY -> "Reduce prolonged or heavy outdoor exertion. Plan breaks or choose an indoor alternative.";
            case VERY_UNHEALTHY, HAZARDOUS -> "Prefer indoor activity and check local official air-quality guidance.";
        };
    }

    private static String children(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD -> "Usual outdoor play is reasonable.";
            case MODERATE -> "Keep normal play flexible and check updated conditions before a long session.";
            case SENSITIVE -> "Shorten vigorous outdoor play, add breaks and plan an indoor option.";
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> "Choose indoor play for this planning period.";
        };
    }

    private static String olderAdults(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD -> "Consider a walk at a comfortable pace.";
            case MODERATE -> "Prefer a shorter walk at a comfortable pace if sensitive to air pollution.";
            case SENSITIVE -> "Choose a brief, gentle outing or an indoor walk.";
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> "Prefer an indoor outing and check local official guidance.";
        };
    }
}
