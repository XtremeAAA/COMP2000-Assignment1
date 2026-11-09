import java.awt.Point;
import java.util.Comparator;

public class SeekShelterStrategy implements MovementStrategy<Human> {
    @Override
    public void move(Human self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        Point target = self.getSafeZones().stream()
                .min(Comparator.comparingInt(p -> Math.abs(p.x - self.getX()) + Math.abs(p.y - self.getY())))
                .orElse(null);

        if (target == null) return;

        // Nếu đã đứng ngay trong ô Safe Zone rồi thì đứng yên trú ẩn
        if (self.getX() == target.x && self.getY() == target.y) {
            return;
        }

        int[] next = PathFinder.findNextStep(
                self.getX(), self.getY(),
                target.x, target.y,
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