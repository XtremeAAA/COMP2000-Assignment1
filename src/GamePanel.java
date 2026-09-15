import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class GamePanel extends JPanel {
    private int dayCount = 1;

    private boolean gameOver = false;
    private String winnerText = "";
    private int curesUsedCount = 0;

    private static final int GRID_SIZE = 18;
    private static final int CELL_SIZE = 48;
    private static final int DAY_LENGTH_TICKS = 300; // how many render ticks make one full day/night cycle

    private final List<Entity> entities = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Point> trees = new ArrayList<>();
    private final Random rand = new Random();
    private final List<Cure> cures = new ArrayList<>();
    private int dayNightTick = 0;
    private int[][] background;

    private static final int CURE_CAP = 6;           // never have more than this many on the map
    private static final int CURE_SPAWN_EVERY = 10;  // spawn a new cure every N logic steps
    private int cureSpawnCounter = 0;

    public int getCureCount() {
        return cures.size();
    }

    public GamePanel() throws WorldSetupException {
        setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));
        setupWorld();
        resetEntities();
    }

    private void checkGameOver() {
        if (gameOver) return;

        if (getZombieCount() == 0) {
            gameOver = true;
            winnerText = "Humans Win!";
        } else if (getHumanCount() == 0) {
            gameOver = true;
            winnerText = "Zombies Win!";
        }
    }

    // Fills a 2D array with tile codes: 0 = plain grass, 1 = dark tuft,
    // 2 = light tuft, 3 = tiny flower. Fixed seed so it's stable across runs.
    private int[][] generateBackground() {
        int[][] bg = new int[GRID_SIZE][GRID_SIZE];
        Random bgRand = new Random(42);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int roll = bgRand.nextInt(100);
                if (roll < 65)       bg[row][col] = 0;
                else if (roll < 82)  bg[row][col] = 1;
                else if (roll < 96)  bg[row][col] = 2;
                else                 bg[row][col] = 3;
            }
        }
        return bg;
    }

    private void setupWorld() throws WorldSetupException {
        buildings.add(new Building(2, 1, 2, 2));
        buildings.add(new Building(12, 2, 3, 2));
        buildings.add(new Building(6, 6, 2, 3));
        buildings.add(new Building(13, 11, 2, 2));
        buildings.add(new Building(2, 12, 3, 2));
        buildings.add(new Building(9, 14, 2, 2));

        while (trees.size() < 30) {
            Point p = findFreeCell(1000);
            trees.add(p);
        }
        background = generateBackground();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private boolean isBlocked(int col, int row) {
        for (Building b : buildings) {
            if (b.occupiesCell(col, row)) return true;
        }
        return false;
    }

    private Point findFreeCell(int maxAttempts) throws WorldSetupException {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = rand.nextInt(GRID_SIZE);
            int y = rand.nextInt(GRID_SIZE);
            if (!isBlocked(x, y)) {
                return new Point(x, y);
            }
        }
        throw new WorldSetupException("Could not find a free cell after " + maxAttempts + " attempts — grid may be too crowded.");
    }

    public void resetEntities() throws WorldSetupException {
        entities.clear();
        dayCount = 1;
        int placed = 0;
        while (placed < 18) {
            Point p = findFreeCell(1000);
            entities.add(new Human(p.x, p.y));
            placed++;
        }

        placed = 0;
        while (placed < 5) {
            Point p = findFreeCell(1000);
            entities.add(new Zombie(p.x, p.y));
            placed++;
        }

        cures.clear();
        int placedCures = 0;
        while (placedCures < 6) {
            Point p = findFreeCell(1000);
            cures.add(new Cure(p.x, p.y));
            placedCures++;
        }

        dayNightTick = 0;
        cureSpawnCounter = 0;
        gameOver = false;
        winnerText = "";
        curesUsedCount = 0;
        repaint();
    }

    private void spawnCureIfNeeded() {
        cureSpawnCounter++;
        if (cureSpawnCounter < CURE_SPAWN_EVERY) return;
        cureSpawnCounter = 0;

        if (cures.size() >= CURE_CAP) return;

        try {
            Point p = findFreeCell(1000);
            cures.add(new Cure(p.x, p.y));
        } catch (WorldSetupException e) {
            // Grid is too crowded — skip this spawn.
        }
    }

    private void handleCures() {
        // Part 1: pick up any cure a human is standing on.
        List<Cure> pickedCures = new ArrayList<>();
        for (Cure c : cures) {
            for (Entity e : entities) {
                if (e instanceof Human && e.getX() == c.getX() && e.getY() == c.getY()) {
                    Human h = (Human) e;
                    if (!h.hasCure()) {
                        h.giveCure();
                        pickedCures.add(c);
                        break;
                    }
                }
            }
        }
        cures.removeAll(pickedCures);

        // Part 2: any cure-carrier adjacent to a zombie cures it.
        List<Zombie> curedZombies = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Human) {
                Human h = (Human) e;
                if (h.hasCure()) {
                    for (Entity other : entities) {
                        if (other instanceof Zombie) {
                            int dx = Math.abs(other.getX() - h.getX());
                            int dy = Math.abs(other.getY() - h.getY());
                            if (dx <= 1 && dy <= 1 && !curedZombies.contains(other)) {
                                curedZombies.add((Zombie) other);
                                h.useCure();
                                curesUsedCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Part 3: convert each cured zombie into a human.
        for (Zombie z : curedZombies) {
            Human newHuman = new Human(z.getX(), z.getY());
            newHuman.syncRenderPosition(z.getRenderX(), z.getRenderY());
            entities.remove(z);
            entities.add(newHuman);
        }
    }

    public void step() {
        if (gameOver) return;
        boolean[][] blocked = computeBlockedGrid();
        Entity[] snapshot = entities.toArray(new Entity[0]);

        // Phase 1: reset every entity's plan to "stay put".
        for (Entity e : entities) {
            e.resetPlan();
        }

        // Phase 2: ask every entity where it WANTS to go.
        // Nobody actually moves yet. Everyone sees the same snapshot.
        for (Entity e : entities) {
            if (e instanceof Human) {
                ((Human) e).setVisibleCures(cures);
            }
            e.move(GRID_SIZE, GRID_SIZE, snapshot, blocked);
        }

        // Phase 3: apply plans. Two entities of the SAME class can't claim the same tile.
        // Zombies may still step onto humans (to infect), and cure-carriers may step onto zombies.
        java.util.Set<String> taken = new java.util.HashSet<>();
        for (Entity e : entities) {
            int tx = e.getNextX();
            int ty = e.getNextY();
            String key = e.getClass().getSimpleName() + ":" + tx + "," + ty;
            if (taken.contains(key)) {
                continue; // same-class entity already claimed this cell
            }
            taken.add(key);
            e.applyPlannedMove();
        }

        // Phase 4: resolve interactions using the new positions.
        handleInfections();
        handleCures();

        // Phase 5: occasionally spawn a new cure so the world always has some.
        spawnCureIfNeeded();
        checkGameOver();
    }

    // Fast render tick: advances day/night and glides entities toward their targets
    public void tick() {
    int prevTick = dayNightTick;
    dayNightTick = (dayNightTick + 1) % DAY_LENGTH_TICKS;
    
    // dayNightTick =0 --> finish 1 day (day++)
    if (dayNightTick < prevTick) {
        dayCount++;
    }

    for (Entity e : entities) {
        e.updateRenderPosition(0.15);
    }
    repaint();
}

    private boolean[][] computeBlockedGrid() {
        boolean[][] blocked = new boolean[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                blocked[row][col] = isBlocked(col, row);
            }
        }
        return blocked;
    }

    private void handleInfections() {
        List<Entity> toConvert = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Zombie) {
                for (Entity other : entities) {
                    if (other instanceof Human && other.getX() == e.getX() && other.getY() == e.getY()) {
                        Human h = (Human) other;
                        if (!h.hasCure() && !toConvert.contains(other)) {
                            toConvert.add(other);
                        }
                    }
                }
            }
        }
        for (Entity e : toConvert) {
            Human h = (Human) e;
            Zombie newZombie = new Zombie(h);
            newZombie.syncRenderPosition(h.getRenderX(), h.getRenderY());
            entities.remove(h);
            entities.add(newZombie);
        }
    }

    public int getHumanCount() {
        int count = 0;
        for (Entity e : entities) if (e instanceof Human) count++;
        return count;
    }

    public int getZombieCount() {
        int count = 0;
        for (Entity e : entities) if (e instanceof Zombie) count++;
        return count;
    }

    // Number of humans that are carrying a cure (doctors)
    public int getDoctorCount() {
        int count = 0;
        for (Entity e : entities) {
            if (e instanceof Human) {
                Human h = (Human) e;
                if (h.hasCure()) count++;
            }
        }
        return count;
    }

    public double getDayProgress() {
        return dayNightTick / (double) DAY_LENGTH_TICKS;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double t = getDayProgress();

        drawGrass(g2);
        drawTrees(g2);
        for (Building b : buildings) {
            b.draw(g2, CELL_SIZE);
        }
        for (Cure c : cures) {
            c.draw(g2, CELL_SIZE);
        }
        drawCelestialBody(g2, t);
        drawEntities(g2);
        drawNightOverlay(g2, t);

        if (gameOver) {
            drawEndScreen(g2);
        }
    }

    // Draws the sun during the day half of the cycle, the moon during the night half,
    // arcing across the sky as t goes from 0 to 1.
    private void drawCelestialBody(Graphics2D g2, double t) {
        int w = GRID_SIZE * CELL_SIZE;
        int h = GRID_SIZE * CELL_SIZE;

        boolean isDaytime = t < 0.5;
        double half = isDaytime ? (t / 0.5) : ((t - 0.5) / 0.5);
        double arcX = half * w;
        double arcY = h * 0.18 - Math.sin(half * Math.PI) * (h * 0.18);

        if (isDaytime) {
            g2.setColor(new Color(255, 240, 160, 120));
            g2.fill(new Ellipse2D.Double(arcX - 26, arcY - 8, 52, 52));
            g2.setColor(new Color(255, 221, 89));
            g2.fill(new Ellipse2D.Double(arcX - 18, arcY, 36, 36));
        } else {
            g2.setColor(new Color(230, 230, 240));
            g2.fill(new Ellipse2D.Double(arcX - 14, arcY, 28, 28));
            g2.setColor(new Color(200, 200, 220));
            g2.fillOval((int) arcX - 8, (int) arcY + 4, 14, 14);
        }
    }

    private void drawGrass(Graphics2D g2) {
        // Base grass fill
        g2.setColor(new Color(86, 168, 74));
        g2.fillRect(0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        Color darkTuft  = new Color(70, 148, 60);
        Color lightTuft = new Color(100, 180, 88);
        Color flower    = new Color(240, 220, 100);
        Color stem      = new Color(120, 90, 40);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int tile = background[row][col];
                int px = col * CELL_SIZE;
                int py = row * CELL_SIZE;

                switch (tile) {
                    case 1:
                        g2.setColor(darkTuft);
                        g2.fillRect(px + 6,  py + 8,  3, 3);
                        g2.fillRect(px + 18, py + 22, 3, 3);
                        g2.fillRect(px + 30, py + 14, 3, 3);
                        break;
                    case 2:
                        g2.setColor(lightTuft);
                        g2.fillRect(px + 10, py + 30, 2, 2);
                        g2.fillRect(px + 26, py + 10, 2, 2);
                        break;
                    case 3:
                        g2.setColor(flower);
                        g2.fillOval(px + CELL_SIZE / 2 - 3, py + CELL_SIZE / 2 - 3, 6, 6);
                        g2.setColor(stem);
                        g2.fillRect(px + CELL_SIZE / 2 - 1, py + CELL_SIZE / 2 + 3, 2, 5);
                        break;
                    default:
                        break; // plain grass
                }
            }
        }
    }

    private void drawTrees(Graphics2D g2) {
        for (Point p : trees) {
            int px = p.x * CELL_SIZE;
            int py = p.y * CELL_SIZE;

            g2.setColor(new Color(90, 60, 30));
            g2.fillRect(px + CELL_SIZE / 2 - 3, py + CELL_SIZE / 2, 6, CELL_SIZE / 2 - 4);

            g2.setColor(new Color(30, 100, 40));
            g2.fillOval(px + 2, py - 4, CELL_SIZE - 4, CELL_SIZE - 4);
            g2.setColor(new Color(45, 130, 55));
            g2.fillOval(px + 6, py, CELL_SIZE - 14, CELL_SIZE - 14);
        }
    }

    // A translucent dark-blue tint that peaks at midnight and clears at noon
    private void drawNightOverlay(Graphics2D g2, double t) {
        double darkness = (1 - Math.cos(t * 2 * Math.PI)) / 2.0;
        int alpha = (int) (darkness * 140);
        if (alpha > 0) {
            g2.setColor(new Color(10, 15, 50, alpha));
            g2.fillRect(0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        }
    }

    private void drawEntities(Graphics2D g2) {
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, (int) (CELL_SIZE * 0.68));
        g2.setFont(emojiFont);
        FontMetrics fm = g2.getFontMetrics();

        for (Entity e : entities) {
            String emoji = e.getEmoji();
            double px = e.getRenderX() * CELL_SIZE;
            double py = e.getRenderY() * CELL_SIZE;

            int textWidth = fm.stringWidth(emoji);
            int textX = (int) (px + (CELL_SIZE - textWidth) / 2.0);
            int textY = (int) (py + (CELL_SIZE + fm.getAscent()) / 2.0 - 4 + e.getBobOffset());

            g2.drawString(emoji, textX, textY);
        }
    }

    private void drawEndScreen(Graphics2D g2) {
        int w = GRID_SIZE * CELL_SIZE;
        int h = GRID_SIZE * CELL_SIZE;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, w, h);

        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics titleFm = g2.getFontMetrics();
        int titleWidth = titleFm.stringWidth(winnerText);
        g2.setColor(Color.WHITE);
        g2.drawString(winnerText, (w - titleWidth) / 2, h / 2 - 10);

        String stats = "Cures used: " + curesUsedCount;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        FontMetrics statsFm = g2.getFontMetrics();
        int statsWidth = statsFm.stringWidth(stats);
        g2.drawString(stats, (w - statsWidth) / 2, h / 2 + 24);
    }
    public int getDayCount() {
    return dayCount;
}
}