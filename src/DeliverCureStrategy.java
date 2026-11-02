public class DeliverCureStrategy implements MovementStrategy<Human> {
    @Override
    public void move(Human self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        Zombie target = Entity.findNearest(allEntities, Zombie.class, self.getX(), self.getY());
        if (target == null) return;

        int[] next = PathFinder.findNextStep(
                self.getX(), self.getY(),
                target.getX(), target.getY(),
                blocked, gridWidth);

        if (next == null) return;

        int newX = next[0];
        int newY = next[1];

        // Don't step onto another human. Stepping onto a zombie is fine — that's how cures are delivered.
        if (!Entity.isTileOccupiedBy(Human.class, newX, newY, allEntities, self)) {
            self.planPosition(newX, newY);
        }
    }
}