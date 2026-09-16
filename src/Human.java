// public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
//     if (hasCure) {
//         ...
//     }
//     ...
// }
// private int clamp(int value, int min, int max) { ... }

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Human extends Entity implements Infectable {

    private boolean infected = false;
    private boolean hasCure = false;
    private static final Random rand = new Random();

    private List<Cure> visibleCures = new ArrayList<>(); 
    private static final int DETECTION_RADIUS = 5;
    public List<Cure> getVisibleCures() {
        return visibleCures;
    }

    public void setVisibleCures(List<Cure> cures) {
        this.visibleCures = cures;
    }

    public Human(int x, int y) {
        super(x, y, "\uD83E\uDDCD"); // 🧍
    }

    // Overloaded constructor: allow creating a human with a custom emoji
    public Human(int x, int y, String customEmoji) {
        super(x, y, customEmoji);
    }
    public boolean hasCure() {
        return hasCure;
    }

    public void giveCure() {
        this.hasCure = true;
        this.emoji = "\uD83E\uDDD1\u200D\u2695\uFE0F"; //🧑‍⚕️
    }

    public void useCure() {
        this.hasCure = false;
        this.emoji = "\uD83E\uDDCD"; //🧍
    }

    @Override public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
        MovementStrategy strategy;
        if (hasCure) {
            strategy = new DeliverCureStrategy();
        } else {
            Zombie nearestZombie = Entity.findNearest(allEntities, Zombie.class, gridX, gridY);
            Cure nearestCure = findNearestCure();
            boolean zombieNear = nearestZombie != null && distanceTo(nearestZombie.getX(), nearestZombie.getY()) <= DETECTION_RADIUS;
            boolean cureNear = nearestCure != null && distanceTo(nearestCure.getX(), nearestCure.getY()) <= DETECTION_RADIUS;
            if (zombieNear) {
                strategy = new FleeStrategy(); 
            } else if (cureNear) {
                strategy = new SeekCureStrategy();
            } else {
                strategy = new WanderStrategy();
            }
        } 
        strategy.move(this, gridWidth, gridHeight, allEntities, blocked);
    }
    private int distanceTo(int x, int y) {
        return Math.abs(x - gridX) + Math.abs(y - gridY);
    } 
    private Cure findNearestCure() {
        Cure nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Cure c : visibleCures) {
            int dist = distanceTo(c.getX(), c.getY());
            if (dist < bestDist) {
                bestDist = dist;
                nearest = c;
            }
        }
    return nearest;
    }
    // public void move(int gridWidth, int gridHeight, Entity[] allEntities, boolean[][] blocked) {
    //     if (hasCure) {
    //         Zombie targetZombie = Entity.findNearest(allEntities, Zombie.class, gridX, gridY);
    //         if (targetZombie != null) {
    //             int dx = Integer.compare(targetZombie.getX(), this.gridX);
    //             int dy = Integer.compare(targetZombie.getY(), this.gridY);

    //             int newX = clamp(gridX + dx, 0, gridWidth - 1);
    //             int newY = clamp(gridY + dy, 0, gridHeight - 1);

    //             if (!blocked[newY][newX]) {
    //                 setPosition(newX, newY);
    //                 return;
    //             }
    //         }
    //     }
    //     // Humans wander randomly, avoiding buildings
    //     int dx = rand.nextInt(3) - 1; // -1, 0, or 1
    //     int dy = rand.nextInt(3) - 1;

    //     int newX = clamp(gridX + dx, 0, gridWidth - 1);
    //     int newY = clamp(gridY + dy, 0, gridHeight - 1);

    //     if (!blocked[newY][newX] && !Entity.isTileOccupiedBy(Zombie.class, newX, newY, allEntities, this)) {
    //         setPosition(newX, newY);
    //     }
    // }

    // private int clamp(int value, int min, int max) {
    //     return Math.max(min, Math.min(max, value));
    // }

    @Override
    public void infect() {
        this.infected = true;
    }

    @Override
    public boolean isInfected() {
        return infected;
    }
}