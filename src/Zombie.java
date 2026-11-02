public class Zombie extends Entity {

    // Zombies now use the Strategy pattern too. The chase behaviour (a lambda in
    // ZombieStrategies) is wrapped in two DECORATORS that slow the zombie down:
    //   1. by time of day  - zombies are sluggish in daylight, full speed at night
    //   2. by weather      - rain and storms slow them further
    // Each decorator is itself a MovementStrategy, so they stack.
    private final MovementStrategy<Zombie> strategy = buildStrategy();

    public Zombie(int x, int y) {
        super(x, y, "🧟"); // zombie emoji
    }

    // Overloaded constructor: build a Zombie directly from a Human that just got infected
    public Zombie(Human infectedHuman) {
        super(infectedHuman.getX(), infectedHuman.getY(), "🧟");
    }

    private static MovementStrategy<Zombie> buildStrategy() {
        MovementStrategy<Zombie> byTimeOfDay =
                new ScaledSpeedDecorator<>(ZombieStrategies.CHASE, c -> c.night() ? 1.0 : 0.5);
        return new ScaledSpeedDecorator<>(byTimeOfDay, c -> c.weather().getZombieSpeed());
    }

    @Override
    public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        strategy.move(this, gridWidth, gridHeight, allEntities, blocked);
    }
}