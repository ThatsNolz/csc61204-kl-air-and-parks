package cityair.view.design;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import javax.swing.Timer;

/**
 * A single animation driven by a Swing {@link Timer}, so every frame runs on the event dispatch
 * thread and no animation can block or be blocked by application work.
 *
 * <p>The animation reports progress from 0 to 1 and never sleeps, delays an action or defers data:
 * the final state is applied on the last frame, and immediately when the user has asked for reduced
 * motion. Starting a new animation on the same component should replace the previous one through
 * {@link #cancel()} so two timers can never fight over one value.</p>
 */
public final class Motion {
    private final Timer timer;
    private final long start = System.nanoTime();
    private final int duration;
    private final DoubleConsumer frame;

    private Motion(int duration, DoubleConsumer frame) {
        this.duration = duration;
        this.frame = frame;
        this.timer = new Timer(Metrics.FRAME_MILLIS, event -> tick());
        timer.setCoalesce(true);
    }

    /**
     * Runs {@code frame} from 0 to 1 over the requested duration, honouring the reduced-motion
     * setting. Returns the running animation so the caller can cancel it.
     */
    public static Motion play(int durationMillis, DoubleConsumer frame) {
        Objects.requireNonNull(frame);
        var motion = new Motion(Skin.duration(durationMillis), frame);
        motion.begin();
        return motion;
    }

    /** Stops the animation without applying its final value. */
    public void cancel() { timer.stop(); }

    /** Returns true while the animation is still producing frames. */
    public boolean isRunning() { return timer.isRunning(); }

    /** Decelerating curve for entrances, selections and value changes. */
    public static double easeOut(double t) {
        double clamped = clamp(t);
        return 1 - Math.pow(1 - clamped, 3);
    }

    /** Symmetric curve for movements that start and end at rest. */
    public static double easeInOut(double t) {
        double clamped = clamp(t);
        return clamped < 0.5 ? 4 * clamped * clamped * clamped : 1 - Math.pow(-2 * clamped + 2, 3) / 2;
    }

    private void begin() {
        if (duration <= 0) {
            frame.accept(1);
            return;
        }
        frame.accept(0);
        timer.start();
    }

    private void tick() {
        double elapsed = (System.nanoTime() - start) / 1_000_000d;
        double progress = clamp(elapsed / duration);
        frame.accept(progress);
        if (progress >= 1) timer.stop();
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
