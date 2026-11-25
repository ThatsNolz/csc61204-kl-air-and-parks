package cityair.strategy;

import cityair.model.AirCategory;
import java.util.Objects;

/** Advice policy for healthy adults: outdoor activity stays reasonable until the Unhealthy category. */
public final class GeneralPublicAdvice implements AdviceStrategy {
    /** Returns outdoor-activity guidance for the general public in the given category. */
    @Override public String advice(AirCategory category) {
        return switch (Objects.requireNonNull(category)) {
            case GOOD, MODERATE, SENSITIVE -> "Routine outdoor activity is generally reasonable. Recheck conditions before sustained exertion.";
            case UNHEALTHY -> "Reduce prolonged or heavy outdoor exertion. Plan breaks or choose an indoor alternative.";
            case VERY_UNHEALTHY, HAZARDOUS -> "Prefer indoor activity and check local official air-quality guidance.";
        };
    }
}
