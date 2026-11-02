public class FleeStrategy implements MovementStrategy<Human> {
    @Override
    public void move(Human self, int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        Zombie threat = Entity.findNearest(allEntities, Zombie.class, self.getX(), self.getY());
        if (threat == null) return;

        // Pick the corner of the grid farthest from the zombie as the escape goal.
        int farX = (threat.getX() < gridWidth / 2) ? gridWidth - 1 : 0;
        int farY = (threat.getY() < gridHeight / 2) ? gridHeight - 1 : 0;

        int[] next = PathFinder.findNextStep(
                self.getX(), self.getY(),
                farX, farY,
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