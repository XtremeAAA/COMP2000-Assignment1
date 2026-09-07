import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel {

    private static final int GRID_SIZE = 18;
    private static final int CELL_SIZE = 48;
    private static final int DAY_LENGTH_TICKS = 300; // how many render ticks make one full day/night cycle

    private final List<Entity> entities = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Point> trees = new ArrayList<>();
    private final Random rand = new Random();
    private final List<Cure> cures = new ArrayList<>();
    private int dayNightTick = 0;
    private boolean paused = false;
    
    public int getCureCount() {
        return cures.size();
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
        repaint();
    }
    public GamePanel() {
        setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));
        setupWorld();
        resetEntities();
    }

    private void setupWorld() {
        buildings.add(new Building(2, 1, 2, 2));
        buildings.add(new Building(12, 2, 3, 2));
        buildings.add(new Building(6, 6, 2, 3));
        buildings.add(new Building(13, 11, 2, 2));
        buildings.add(new Building(2, 12, 3, 2));
        buildings.add(new Building(9, 14, 2, 2));

        while (trees.size() < 30) {
            int tx = rand.nextInt(GRID_SIZE);
            int ty = rand.nextInt(GRID_SIZE);
            if (!isBlocked(tx, ty)) {
                trees.add(new Point(tx, ty));
            }
        }
    }

    private boolean isBlocked(int col, int row) {
        for (Building b : buildings) {
            if (b.occupiesCell(col, row)) return true;
        }
        return false;
    }

    public void resetEntities() {
        entities.clear();

        int placed = 0;
        while (placed < 18) {
            int x = rand.nextInt(GRID_SIZE);
            int y = rand.nextInt(GRID_SIZE);
            if (!isBlocked(x, y)) {
                entities.add(new Human(x, y));
                placed++;
            }
        }

        placed = 0;
        while (placed < 5) {
            int x = rand.nextInt(GRID_SIZE);
            int y = rand.nextInt(GRID_SIZE);
            if (!isBlocked(x, y)) {
                entities.add(new Zombie(x, y));
                placed++;
            }
        }
            cures.clear();
        int placedCures = 0;
        while (placedCures < 6) {
            int cx = rand.nextInt(GRID_SIZE);
            int cy = rand.nextInt(GRID_SIZE);
        if (!isBlocked(cx, cy)) {
            cures.add(new Cure(cx, cy));
        placedCures++;
    }
}

        dayNightTick = 0;
        repaint();
    }
  private void handleCures() {
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

    
    List<Zombie> curedZombies = new ArrayList<>();
    for (Entity e : entities) {
        if (e instanceof Human) {
            Human h = (Human) e;
            if (h.hasCure()) {
                for (Entity other : entities) {
                    if (other instanceof Zombie && other.getX() == h.getX() && other.getY() == h.getY()) {
                        curedZombies.add((Zombie) other);
                        h.useCure(); 
                        break;
                    }
                }
            }
        }
    }

    // Chuyển các Zombie được chữa thành Human mới
    for (Zombie z : curedZombies) {
        Human newHuman = new Human(z.getX(), z.getY());
        newHuman.syncRenderPosition(z.getRenderX(), z.getRenderY());
        entities.remove(z);
        entities.add(newHuman);
    }
}

    // Slower logic tick: each entity decides its next grid cell
    public void step() {
        boolean[][] blocked = computeBlockedGrid();
        Entity[] snapshot = entities.toArray(new Entity[0]);
        for (Entity e : entities) {
            e.move(GRID_SIZE, GRID_SIZE, snapshot, blocked);
        }
        handleInfections();
        handleCures();
    }

    // Fast render tick: advances day/night and glides entities toward their targets
    public void tick() {
        dayNightTick = (dayNightTick + 1) % DAY_LENGTH_TICKS;
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
                        if (!h.hasCure()) {
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
drawCelestialBody(g2, t);
drawEntities(g2);
drawNightOverlay(g2, t);
if (paused) {
    drawPausedOverlay(g2);
}
}

    private void drawGrass(Graphics2D g2) {
        g2.setColor(new Color(86, 168, 74));
        g2.fillRect(0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        // Scatter darker tufts for texture - fixed seed so it doesn't flicker every frame
        Random tuftRand = new Random(42);
        g2.setColor(new Color(70, 148, 60));
        for (int i = 0; i < 700; i++) {
            int x = tuftRand.nextInt(GRID_SIZE * CELL_SIZE);
            int y = tuftRand.nextInt(GRID_SIZE * CELL_SIZE);
            g2.fillRect(x, y, 3, 3);
        }

        // A second, lighter layer of tufts for extra depth
        Random tuftRand2 = new Random(99);
        g2.setColor(new Color(100, 180, 88));
        for (int i = 0; i < 500; i++) {
            int x = tuftRand2.nextInt(GRID_SIZE * CELL_SIZE);
            int y = tuftRand2.nextInt(GRID_SIZE * CELL_SIZE);
            g2.fillRect(x, y, 2, 2);
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

    // A translucent dark-blue tint that peaks at midnight and clears at noon
    private void drawNightOverlay(Graphics2D g2, double t) {
        double darkness = (1 - Math.cos(t * 2 * Math.PI)) / 2.0;
        int alpha = (int) (darkness * 140);
        if (alpha > 0) {
            g2.setColor(new Color(10, 15, 50, alpha));
            g2.fillRect(0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        }
    }

    private void drawPausedOverlay(Graphics2D g2) {
        int w = GRID_SIZE * CELL_SIZE;
        int h = GRID_SIZE * CELL_SIZE;

        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, w, h);

        String text = "PAUSED";
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (w - textWidth) / 2;
        int y = h / 2;

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
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
}