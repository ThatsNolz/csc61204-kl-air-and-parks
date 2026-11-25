package cityair.strategy;

import cityair.model.AirCategory;
import java.util.Objects;

/** Advice policy for children, a sensitive group: play is shortened from the Sensitive category upward. */
public final class ChildrenAdvice implements AdviceStrategy {
    /** Returns outdoor-activity guidance for children in the given category. */
    @Override public String advice(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD -> "Usual outdoor play is reasonable.";
            case MODERATE -> "Keep normal play flexible and check updated conditions before a long session.";
            case SENSITIVE -> "Shorten vigorous outdoor play, add breaks and plan an indoor option.";
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> "Choose indoor play for this planning period.";
        };
    }
}
