import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Zombie behaviours written as LAMBDAS instead of one class per behaviour.
// MovementStrategy has a single method, so each behaviour is just a function.
// Humans use classes (Flee/Seek/...) because they are long and need their own names;
// the zombie behaviours are short, so a lambda replaces what would have been a
// hierarchy of ChaseStrategy / WanderStrategy / SidestepStrategy classes.
//
// Everything here is thread safe: the lambdas keep no state of their own, only read
// the shared snapshot, and only write the zombie's own planned position.
public final class ZombieStrategies {

    private ZombieStrategies() { }

    private static final int[][] FOUR_WAYS = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };

    // Shuffle about at random (used when there is nobody to chase or no path to them).
    public static final MovementStrategy<Zombie> WANDER = (self, w, h, all, blocked) -> {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int newX = Math.max(0, Math.min(w - 1, self.getX() + rand.nextInt(3) - 1));
        int newY = Math.max(0, Math.min(h - 1, self.getY() + rand.nextInt(3) - 1));
        if (!blocked[newY][newX] && !Entity.isTileOccupiedBy(Zombie.class, newX, newY, all, self)) {
            self.planPosition(newX, newY);
        }
    };

    // Walk toward the nearest human using A*. If another zombie is standing on the next
    // tile, step around it instead of freezing (this is what stops zombies piling up
    // in one spot). If there is no path at all, wander.
    public static final MovementStrategy<Zombie> CHASE = (self, w, h, all, blocked) -> {
        Human target = Entity.findNearest(all, Human.class, self.getX(), self.getY());
        if (target == null) {
            WANDER.move(self, w, h, all, blocked);
            return;
        }

        int[] next = PathFinder.findNextStep(self.getX(), self.getY(),
                target.getX(), target.getY(), blocked, w);

        if (next == null) {
            WANDER.move(self, w, h, all, blocked);
            return;
        }
        if (!Entity.isTileOccupiedBy(Zombie.class, next[0], next[1], all, self)) {
            self.planPosition(next[0], next[1]);
            return;
        }

        // Next tile is taken by another zombie: pick the free neighbour closest to the human.
        List<int[]> options = new ArrayList<>();
        for (int[] d : FOUR_WAYS) {
            int nx = self.getX() + d[0];
            int ny = self.getY() + d[1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
            if (blocked[ny][nx]) continue;
            if (Entity.isTileOccupiedBy(Zombie.class, nx, ny, all, self)) continue;
            options.add(new int[] { nx, ny });
        }
        // Shuffle first so ties between equally good tiles are broken randomly.
        Collections.shuffle(options, ThreadLocalRandom.current());
        options.stream()
                .min(Comparator.comparingInt(o -> Math.abs(o[0] - target.getX()) + Math.abs(o[1] - target.getY())))
                .ifPresent(best -> self.planPosition(best[0], best[1]));
    };
}