import java.util.Comparator;

public class SeekCureStrategy implements MovementStrategy<Human>{
    @Override
    public void move(Human self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        // Nearest visible cure by Manhattan distance (first one wins a tie, as before).
        Cure target = self.getVisibleCures().stream()
                .min(Comparator.comparingInt(c -> Math.abs(c.getX() - self.getX()) + Math.abs(c.getY() - self.getY())))
                .orElse(null);
        if (target == null) return;

        // A* routes around buildings instead of stepping straight at the cure and stalling.
        int[] next = PathFinder.findNextStep(
                self.getX(), self.getY(),
                target.getX(), target.getY(),
                blocked, gridWidth);

        if (next == null) return;

        int newX = next[0];
        int newY = next[1];

        if (!Entity.isTileOccupiedBy(Human.class, newX, newY, allEntities, self)
            && !Entity.isTileOccupiedBy(Zombie.class, newX, newY, allEntities, self)) {
            self.planPosition(newX, newY);
        }
    }
}
