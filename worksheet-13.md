# COMP2000 Worksheet 2 — Final Submission

**Student name:** 

**GitHub repo URL:** (your own fork of your team's repository, not your team's URL)

---

## 1. Design Patterns

Don't forget to submit a pdf file which shows a diagram of your pattern implemenation to supplement this text description.

**1.1.** Which design pattern(s) did you implement? Name them and state where each appears in your codebase.
Strategy Pattern (MovementStrategy<T extends Entity>)
Strategy Interface: MovementStrategy.java (public interface MovementStrategy<T extends Entity>).
Concrete Strategies for Human: WanderStrategy.java, FleeStrategy.java, SeekCureStrategy.java, DeliverCureStrategy.java, and SeekShelterStrategy.java.
Concrete Strategies for Zombie: ZombieHuntStrategy and ZombieWanderStrategy (in ZombieStrategies.java).

Decorator Pattern (ScaledSpeedDecorator<T extends Entity>)
Component Interface: MovementStrategy.java (MovementStrategy<T extends Entity>).
Concrete Component: Any concrete MovementStrategy<T> (FleeStrategy, SeekCureStrategy, WanderStrategy, SeekShelterStrategy, ZombieHuntStrategy, ZombieWanderStrategy).
Decorator: ScaledSpeedDecorator.java (public class ScaledSpeedDecorator<T extends Entity> implements MovementStrategy<T>), which wraps an inner MovementStrategy<T> and gates movement execution behind a probability function (ToDoubleFunction<WorldConditions>).
Clients: Human.java and Zombie.java

Observer Pattern (GameListener & GameEvent)

Subject (Publisher): GamePanel.java maintains a thread-safe CopyOnWriteArrayList<GameListener> listeners (addListener, removeListener, fire).
Observer Interface (Subscriber): GameListener.java (public interface GameListener { void onEvent(GameEvent event); }).
Event Payload: GameEvent.java (an immutable record holding Type, message, day, weather, and night).
Concrete Observers:
StatsRecorder.java (records all simulation events to compute end-of-game Stream analytics).
EventLog.java (updates the scrolling Swing log panel when significant events occur).
Two lambda observers registered in Main.java (one refreshing the sidebar statistics JLabel on every event, and another stopping logicTimer when GameEvent.Type.GAME_OVER fires).





**1.2.** For each pattern, paste a code snippet showing the key interaction between participants.
Strategy + Generic Decorator Interaction
1. Component / Strategy Interface (MovementStrategy.java)
public interface MovementStrategy<T extends Entity> {
    void move(T entity, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked);
}
2. Generic Decorator (ScaledSpeedDecorator.java)
public class ScaledSpeedDecorator<T extends Entity> implements MovementStrategy<T> {
    private final MovementStrategy<T> inner;
    private final ToDoubleFunction<WorldConditions> chance;
    public ScaledSpeedDecorator(MovementStrategy<T> inner, ToDoubleFunction<WorldConditions> chance) {
        this.inner = inner;
        this.chance = chance;
    }
    @Override
    public void move(T self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        if (ThreadLocalRandom.current().nextDouble() < chance.applyAsDouble(self.getConditions())) {
            inner.move(self, gridWidth, gridHeight, allEntities, blocked);
        }
    }
}
private static final MovementStrategy<Human> DELIVER_CURE_STRATEGY = new DeliverCureStrategy();

private static final MovementStrategy<Human> SEEK_SHELTER_STRATEGY =
        new ScaledSpeedDecorator<>(new SeekShelterStrategy(), c -> c.weather() == Weather.STORM ? 0.6 : 0.9);

private static final MovementStrategy<Human> FLEE_STRATEGY =
        new ScaledSpeedDecorator<>(new FleeStrategy(), c -> c.weather() == Weather.STORM ? 0.5 : 0.8);

@Override
public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
    MovementStrategy<Human> strategy;
    if (hasCure) {
        strategy = DELIVER_CURE_STRATEGY; // Unwrapped: Doctors move at full speed even in storms
    } else {
        Zombie nearestZombie = Entity.findNearest(allEntities, Zombie.class, gridX, gridY);
        Cure nearestCure = findNearestCure();
        boolean zombieNear = nearestZombie != null && distanceTo(nearestZombie.getX(), nearestZombie.getY()) <= zombieSightRange();
        boolean cureNear = nearestCure != null && distanceTo(nearestCure.getX(), nearestCure.getY()) <= DETECTION_RADIUS;
        boolean dangerousConditions = conditions.night() || conditions.weather() == Weather.STORM;

        if (dangerousConditions && !safeZones.isEmpty() && (zombieNear || !cureNear)) {
            strategy = SEEK_SHELTER_STRATEGY;
        } else if (zombieNear) {
            strategy = FLEE_STRATEGY;
        } else if (cureNear) {
            strategy = SEEK_CURE_STRATEGY;
        } else {
            strategy = WANDER_STRATEGY;
        }
    }
    strategy.move(this, gridWidth, gridHeight, allEntities, blocked);
}
1. Observer Interface (GameListener.java) & Immutable Payload (GameEvent.java)
public interface GameListener {
    void onEvent(GameEvent event);
}

public record GameEvent(Type type, String message, int day, Weather weather, boolean night) {
    public enum Type { STEP, HUMAN_INFECTED, ZOMBIE_CURED, CURE_PICKED_UP, CURE_SPAWNED, DAY_BROKE, NIGHT_FELL, WEATHER_CHANGED, GAME_OVER, RESET }
}

2. Subject Registration & Notification (GamePanel.java)
private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

public void addListener(GameListener listener) {
    listeners.add(listener);
}

private void fire(GameEvent.Type type, String message) {
    GameEvent event = new GameEvent(type, message, dayCount, weather, isNight());
    listeners.forEach(l -> l.onEvent(event));
}

 3. Concrete & Lambda Subscribers (Main.java & StatsRecorder.java)
gamePanel.addListener(recorder);analytics
gamePanel.addListener(eventLog);
gamePanel.addListener(event -> updateInfoLabel(infoLabel, gamePanel));




**1.3.** For each pattern, explain why you chose it over an alternative. What trade-off did you make?





---

## 2. Streams / Lambdas / Parallelism

**2.1.** List every file in your project that uses lambda expressions or the Streams API.
GamePanel.java

Streams & Parallel Streams: Arrays.stream(snapshot).parallel().forEach(...) for concurrent entity movement planning; entities.stream().filter(...).flatMap(...).map(...).distinct().collect(...) in handleInfections(); declarative cures.stream().forEach(...) and entities.stream().filter(...).map(...).filter(Human::hasCure)...findFirst().ifPresent(...) in handleCures(); entities.stream().collect(WorldStats.collector()) in getStats(); entities.stream().parallel().collect(DangerZoneStats.collector(...)) in getDangerStats(); and the DangerZone infection pipeline using .findFirst().ifPresent(Human::infect).
Lambdas: Observer notification listeners.forEach(l -> l.onEvent(event)); entities.forEach(e -> e.setConditions(conditions)); and injecting the EmergencyAlert lambda (hx, hy) -> entities.stream()...min(...) into each Human.

StatsRecorder.java
Streams & Downstream Collectors: Multi-stage pipelines over events.stream() using filter, mapToInt(GameEvent::day).max(), Collectors.groupingBy(..., Collectors.counting()), Collectors.partitioningBy(..., Collectors.counting()), entrySet().stream().max(Map.Entry.comparingByValue()), and Optional.map(...).orElse(...) / ifPresent(...) to compute deadliest weather, day vs. night infection rates, peak infection day, and best recovery day.

WorldStats.java
Custom Collector & Lambdas: Implements Collector.of( supplier, accumulator, combiner, finisher ) using four lambda expressions to aggregate human, zombie, and doctor counts in one pass.

DangerZoneStats.java
Custom Collector & Lambdas: Implements Collector.of( () -> new int[2], (acc, e) -> ..., (a, b) -> ..., acc -> new DangerZoneStats(...) ) for parallel single-pass spatial aggregation inside the outbreak zone.

WeatherStream.java
Infinite Stream: Uses Stream.generate(this::advance) with a method reference and .iterator() to model weather transitions as a lazy, infinite stream.

Entity.java
Streams & Generics: Uses Arrays.stream(entities) with .filter(e -> e != exclude && type.isInstance(e)), .map(type::cast), .anyMatch(...), and .min(Comparator.comparingInt(...)).orElse(null) inside findNearest and isTileOccupiedBy.

Human.java
Streams & Lambdas: Uses ToDoubleFunction<WorldConditions> lambdas (c -> c.weather() == Weather.STORM ? 0.5 : 0.8) inside ScaledSpeedDecorator declarations; visibleCures.stream().min(Comparator.comparingInt(...)).orElse(null) in findNearestCure(); safeZones.stream().anyMatch(p -> p.x == gridX && p.y == gridY) in isInSafeZone(); and default lambda initialization (hx, hy) -> Optional.empty() for EmergencyAlert.

Zombie.java
Lambdas: Uses ToDoubleFunction<WorldConditions> lambdas (c -> c.night() ? 1.0 : 0.5 and c -> c.night() ? 0.75 : 0.35) to parameterise ScaledSpeedDecorator instances.

SeekShelterStrategy.java
Streams & Lambdas: Uses self.getSafeZones().stream().min(Comparator.comparingInt(p -> Math.abs(p.x - self.getX()) + Math.abs(p.y - self.getY()))).orElse(null) to select the nearest safe zone tile.

ScaledSpeedDecorator.java & EmergencyAlert.java
Functional Interfaces: ScaledSpeedDecorator accepts and invokes ToDoubleFunction<WorldConditions>; EmergencyAlert defines a custom @FunctionalInterface returning Optional<Point>.

Main.java
Lambdas & Method References: Uses method reference SwingUtilities.invokeLater(Main::createAndShowUI), Timer action lambdas, ChangeListener lambda for the FPS slider, ActionListener lambdas for playButton, resetButton, speedButton (0.5x–4x), closeButton, and GameListener lambdas for sidebar UI updates.




**2.2.** For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?





**2.3.** Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?
Yes, I implemented both Parallel Streams and two Custom Collectors (WorldStats.collector() and DangerZoneStats.collector()).WorldConditions conditions = new WorldConditions(isNight(), weather);
entities.forEach(e -> e.setConditions(conditions));
boolean[][] blocked = computeBlockedGrid();
Entity[] snapshot = entities.toArray(new Entity[0]);
Phase 1: sequentially reset plans and inject read-only perceptions
for (Entity e : entities) { e.resetPlan(); }
entities.stream().filter(e -> e instanceof Human).forEach(e -> {
    Human h = (Human) e;
    h.setVisibleCures(cures);
    h.setSafeZones(safeZones);
    h.setEmergencyAlert(...);
});
Phase 2: execute movement & A* pathfinding decisions in parallel across ForkJoinPool worker threads
Arrays.stream(snapshot).parallel()
        .forEach(e -> e.move(GRID_SIZE, GRID_SIZE, snapshot, blocked));




---

## 3. Uniqueness and Creativity

**3.1.** What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.





**3.2.** Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.





**3.3.** If you had another 6 weeks, what would you add or change?





---

## 4. Log Book and Final Reflection

**4.1.** Attach or link your log book entries for Weeks 7–13.





**4.2.** Reflect on the semester:

- What was the most important concept you learned?

- What design trade-off did you face, and how did you resolve it?

- If you started over, what would you do differently?

- What feedback from the Week 7 submission did you act on?
