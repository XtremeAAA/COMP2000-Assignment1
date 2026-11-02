// STRATEGY pattern: one way of deciding where an entity goes next.
// It is generic (T extends Entity) so the SAME interface works for humans and zombies,
// and it has a single method, so a strategy can also be written as a lambda.
@FunctionalInterface
public interface MovementStrategy<T extends Entity> {
    void move(T self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked);
}