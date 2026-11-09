import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Human extends Entity implements Infectable {

    private boolean infected = false;
    private boolean hasCure = false;

    private List<Cure> visibleCures = new ArrayList<>();
    private List<Point> safeZones = new ArrayList<>();
    private static final int DETECTION_RADIUS = 5;

    private static final MovementStrategy<Human> DELIVER_CURE_STRATEGY = new DeliverCureStrategy();

    private static final MovementStrategy<Human> SEEK_SHELTER_STRATEGY =
            new ScaledSpeedDecorator<>(new SeekShelterStrategy(), c -> c.weather() == Weather.STORM ? 0.6 : 0.9);

    private static final MovementStrategy<Human> FLEE_STRATEGY =
            new ScaledSpeedDecorator<>(new FleeStrategy(), c -> c.weather() == Weather.STORM ? 0.5 : 0.8);

    private static final MovementStrategy<Human> SEEK_CURE_STRATEGY =
            new ScaledSpeedDecorator<>(new SeekCureStrategy(), c -> c.weather() == Weather.STORM ? 0.6 : 0.9);

    private static final MovementStrategy<Human> WANDER_STRATEGY =
            new ScaledSpeedDecorator<>(new WanderStrategy(), c -> c.weather() == Weather.STORM ? 0.4 : 0.7);

    public List<Cure> getVisibleCures() {
        return visibleCures;
    }

    public void setVisibleCures(List<Cure> cures) {
        this.visibleCures = cures;
    }

    public List<Point> getSafeZones() {
        return safeZones;
    }

    public void setSafeZones(List<Point> safeZones) {
        this.safeZones = safeZones;
    }

    public boolean isInSafeZone() {
        return safeZones.stream().anyMatch(p -> p.x == gridX && p.y == gridY);
    }

    public Human(int x, int y) {
        super(x, y, "\uD83E\uDDCD"); // 🧍
    }

    public Human(int x, int y, String customEmoji) {
        super(x, y, customEmoji);
    }

    public boolean hasCure() {
        return hasCure;
    }

    public void giveCure() {
        this.hasCure = true;
        this.emoji = "\uD83E\uDDD1\u200D\u2695\uFE0F"; // 🧑‍⚕️
    }

    public void useCure() {
        this.hasCure = false;
        this.emoji = "\uD83E\uDDCD"; // 🧍
    }

    @Override
    public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        MovementStrategy<Human> strategy;

        if (hasCure) {
            strategy = DELIVER_CURE_STRATEGY;
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

    private int zombieSightRange() {
        int range = conditions.weather().getSightRange();
        return conditions.night() ? Math.max(1, range - 1) : range;
    }

    private int distanceTo(int x, int y) {
        return Math.abs(x - gridX) + Math.abs(y - gridY);
    }

    private Cure findNearestCure() {
        return visibleCures.stream()
                .min(Comparator.comparingInt(c -> distanceTo(c.getX(), c.getY())))
                .orElse(null);
    }

    @Override
    public void infect() {
        this.infected = true;
    }

    @Override
    public boolean isInfected() {
        return infected;
    }
}