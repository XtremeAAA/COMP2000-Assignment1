import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

// DECORATOR pattern: wraps ANY other MovementStrategy and adds one extra effect to it
// ("only move some of the time") without changing the strategy inside.
//
// The chance of moving comes from a lambda that looks at the current world, e.g.
//     c -> c.night() ? 1.0 : 0.7
// Decorators can be stacked, because a decorator is itself a MovementStrategy:
//     new ScaledSpeedDecorator<>(new ScaledSpeedDecorator<>(chase, byDayAndNight), byWeather)
//
// Why not subclasses? Slow-by-day, slow-by-rain and both together would need
// a new subclass for every combination. With decorators we just stack the wrappers.
public class ScaledSpeedDecorator<T extends Entity> implements MovementStrategy<T> {

    private final MovementStrategy<T> inner;
    private final ToDoubleFunction<WorldConditions> moveChance;

    public ScaledSpeedDecorator(MovementStrategy<T> inner, ToDoubleFunction<WorldConditions> moveChance) {
        this.inner = inner;
        this.moveChance = moveChance;
    }

    @Override
    public void move(T self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        double chance = moveChance.applyAsDouble(self.getConditions());
        // ThreadLocalRandom: each thread has its own generator, so this is safe in a parallel stream.
        if (chance >= 1.0 || ThreadLocalRandom.current().nextDouble() < chance) {
            inner.move(self, gridWidth, gridHeight, allEntities, blocked);
        }
        // Otherwise do nothing: the entity keeps its "stay put" plan for this step.
    }
}