package cityair.strategy;

import cityair.model.AirCategory;

/** Contract for choosing activity guidance without changing the controller. */
@FunctionalInterface
public interface AdviceStrategy {
    /** Returns informational outdoor activity advice for the given category. */
    String advice(AirCategory category);
}
