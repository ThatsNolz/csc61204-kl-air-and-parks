package cityair.strategy;

import cityair.model.AirCategory;
import java.util.Objects;

/** Advice policy for older adults, a sensitive group: walks are shortened from the Moderate category upward. */
public final class OlderAdultsAdvice implements AdviceStrategy {
    /** Returns outdoor-activity guidance for older adults in the given category. */
    @Override public String advice(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD -> "Consider a walk at a comfortable pace.";
            case MODERATE -> "Prefer a shorter walk at a comfortable pace if sensitive to air pollution.";
            case SENSITIVE -> "Choose a brief, gentle outing or an indoor walk.";
            case UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS -> "Prefer an indoor outing and check local official guidance.";
        };
    }
}
