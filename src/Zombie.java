public class Zombie extends Entity {

    public Zombie(int x, int y) {
        super(x, y, "\uD83E\uDDDF"); // 🧟
    }

    // Overloaded constructor: build a Zombie directly from a Human that just got infected
    public Zombie(Human infectedHuman) {
        super(infectedHuman.getX(), infectedHuman.getY(), "\uD83E\uDDDF");
    }


    @Override
    public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        // Find the nearest Human
        Human target = Entity.findNearest(allEntities, Human.class, gridX, gridY);
        if (target == null) return;
        
        int [] next = PathFinder.findNextStep(gridX, gridY, target.getX(), target.getY(), blocked, gridWidth);
        if (next == null) {
            return; // No valid path found
        }
        int newX = next[0];
        int newY = next[1];

        if (!Entity.isTileOccupiedBy(Zombie.class, newX, newY, allEntities, this)) {
            // Move to the next cell
            planPosition(newX, newY);
        }
    }
}