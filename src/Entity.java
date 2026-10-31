import java.util.Arrays;
import java.util.Comparator;

// Base class for anything that lives on the grid (humans and zombies)
public abstract class Entity {

    protected int gridX;
    protected int gridY;
    protected int nextX;
    protected int nextY;

    // The pixel-smooth position used for drawing, which glides toward gridX/gridY
    // instead of jumping instantly - this is what makes movement look like walking.
    protected double renderX;
    protected double renderY;

    // Used to animate a small vertical bounce while the entity is actively walking
    protected double animPhase = 0;

    protected String emoji;

    public Entity(int x, int y, String emoji) {
        this.gridX = x;
        this.gridY = y;
        this.renderX = x;
        this.renderY = y;
        this.emoji = emoji;
    }

    public int getX() { return gridX; }
    public int getY() { return gridY; }
    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public String getEmoji() { return emoji; }

    // Call this to set a reset position for the entity
    public void resetPlan() {
        this.nextX = gridX;
        this.nextY = gridY;
    }

    public int getNextX() { return nextX; }
    public int getNextY() { return nextY; }

    // Strategies call this instead of setPosition directionly
    public void planPosition(int x, int y) {
        this.nextX = x;
        this.nextY = y;
    }

    // Called after every entity has planned its next moves
    public void applyPlannedMove() {
        if (nextX != gridX || nextY != gridY) {
            setPosition(nextX, nextY);
        }
    }

    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }

    // Used when a Human turns into a Zombie, so the new zombie starts drawing
    // from exactly where the human currently was rendered, not from scratch.
    public void syncRenderPosition(double rx, double ry) {
        this.renderX = rx;
        this.renderY = ry;
    }

    // Glides the rendered pixel position toward the grid target each frame,
    // and advances a small walking-bounce animation while actually moving.
    public void updateRenderPosition(double lerpFactor) {
        double dx = gridX - renderX;
        double dy = gridY - renderY;
        boolean moving = Math.abs(dx) > 0.02 || Math.abs(dy) > 0.02;

        if (moving) {
            animPhase += 0.35;
        } else {
            animPhase = 0;
        }

        renderX += dx * lerpFactor;
        renderY += dy * lerpFactor;
    }

    // Small vertical bounce applied while walking, 0 while standing still
    public double getBobOffset() {
        return Math.sin(animPhase) * 3.0;
    }

    // Decides the next grid cell to move to. blocked[row][col] is true where
    // a building (or other obstacle) occupies that cell.
    public abstract void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked);

    public static boolean isTileOccupiedBy(Class<? extends Entity> type, int x, int y, Entity[] allEntities, Entity exclude) {
        return Arrays.stream(allEntities)
                .anyMatch(e -> type.isInstance(e) && e != exclude && e.getX() == x && e.getY() == y);
    }

    // Nearest entity of the given type by Manhattan distance (first one wins a tie).
    public static <T extends Entity> T findNearest(Entity[] allEntities, Class<T> type, int fromX, int fromY) {
        return Arrays.stream(allEntities)
                .filter(type::isInstance)
                .map(type::cast)
                .min(Comparator.comparingInt(e -> Math.abs(e.getX() - fromX) + Math.abs(e.getY() - fromY)))
                .orElse(null);
    }
}
