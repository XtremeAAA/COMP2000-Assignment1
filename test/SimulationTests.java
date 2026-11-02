import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// Unit tests for the Week 13 additions. They run without opening a window.
public class SimulationTests {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static boolean[][] openGrid(int size) {
        return new boolean[size][size]; // nothing blocked
    }

    // ---------- PathFinder (A*) ----------

    @Test
    void pathFinderStepsTowardTheGoal() {
        int[] next = PathFinder.findNextStep(0, 0, 4, 0, openGrid(5), 5);
        assertArrayEquals(new int[] { 1, 0 }, next);
    }

    @Test
    void pathFinderWalksAroundAWall() {
        boolean[][] blocked = openGrid(5);
        for (int y = 0; y < 4; y++) blocked[y][2] = true; // wall at x=2, gap only at y=4

        int x = 0, y = 0, steps = 0;
        while ((x != 4 || y != 0) && steps < 30) {
            int[] next = PathFinder.findNextStep(x, y, 4, 0, blocked, 5);
            assertNotNull(next, "there is a path, so A* must find a step");
            assertFalse(blocked[next[1]][next[0]], "A* must never step onto a blocked cell");
            x = next[0];
            y = next[1];
            steps++;
        }
        assertEquals(4, x);
        assertEquals(0, y);
    }

    // ---------- Custom collector + parallel streams ----------

    @Test
    void collectorGivesSameAnswerSequentialAndParallel() {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            if (i % 3 == 0) {
                entities.add(new Zombie(0, 0));
            } else {
                Human h = new Human(0, 0);
                if (i % 5 == 0) h.giveCure();
                entities.add(h);
            }
        }
        WorldStats sequential = entities.stream().collect(WorldStats.collector());
        WorldStats parallel = entities.parallelStream().collect(WorldStats.collector());

        assertEquals(sequential.getHumans(), parallel.getHumans());
        assertEquals(sequential.getZombies(), parallel.getZombies());
        assertEquals(sequential.getDoctors(), parallel.getDoctors());
        assertEquals(2000, parallel.getHumans() + parallel.getZombies());
    }

    // ---------- Decorator ----------

    @Test
    void decoratorWithChanceZeroNeverCallsTheInnerStrategy() {
        AtomicInteger calls = new AtomicInteger();
        MovementStrategy<Zombie> inner = (self, w, h, all, blocked) -> calls.incrementAndGet();
        ScaledSpeedDecorator<Zombie> never = new ScaledSpeedDecorator<>(inner, c -> 0.0);
        Zombie z = new Zombie(0, 0);
        for (int i = 0; i < 200; i++) never.move(z, 5, 5, new Entity[] { z }, openGrid(5));
        assertEquals(0, calls.get());
    }

    @Test
    void decoratorWithChanceOneAlwaysCallsTheInnerStrategy() {
        AtomicInteger calls = new AtomicInteger();
        MovementStrategy<Zombie> inner = (self, w, h, all, blocked) -> calls.incrementAndGet();
        ScaledSpeedDecorator<Zombie> always = new ScaledSpeedDecorator<>(inner, c -> 1.0);
        Zombie z = new Zombie(0, 0);
        for (int i = 0; i < 200; i++) always.move(z, 5, 5, new Entity[] { z }, openGrid(5));
        assertEquals(200, calls.get());
    }

    @Test
    void stackedDecoratorsOnlyMoveWhenBothAllow() {
        AtomicInteger calls = new AtomicInteger();
        MovementStrategy<Zombie> inner = (self, w, h, all, blocked) -> calls.incrementAndGet();
        MovementStrategy<Zombie> stacked = new ScaledSpeedDecorator<>(
                new ScaledSpeedDecorator<>(inner, c -> 1.0), c -> 0.0);
        Zombie z = new Zombie(0, 0);
        for (int i = 0; i < 100; i++) stacked.move(z, 5, 5, new Entity[] { z }, openGrid(5));
        assertEquals(0, calls.get());
    }

    @Test
    void decoratorReadsTheEntitysWorldConditions() {
        AtomicInteger calls = new AtomicInteger();
        MovementStrategy<Zombie> inner = (self, w, h, all, blocked) -> calls.incrementAndGet();
        // Moves at night only.
        ScaledSpeedDecorator<Zombie> nightOnly = new ScaledSpeedDecorator<>(inner, c -> c.night() ? 1.0 : 0.0);
        Zombie z = new Zombie(0, 0);

        z.setConditions(new WorldConditions(false, Weather.CLEAR));
        nightOnly.move(z, 5, 5, new Entity[] { z }, openGrid(5));
        assertEquals(0, calls.get());

        z.setConditions(new WorldConditions(true, Weather.CLEAR));
        nightOnly.move(z, 5, 5, new Entity[] { z }, openGrid(5));
        assertEquals(1, calls.get());
    }

    // ---------- Weather stream ----------

    @Test
    void weatherStreamFollowsTheRuleItWasGiven() {
        // Rule: always go to the next weather in the enum list, wrapping round.
        WeatherStream stream = new WeatherStream(w -> Weather.values()[(w.ordinal() + 1) % Weather.values().length]);
        assertEquals(Weather.CLEAR, stream.next());
        assertEquals(Weather.FOG, stream.next());
        assertEquals(Weather.RAIN, stream.next());
        assertEquals(Weather.STORM, stream.next());
        assertEquals(Weather.CLEAR, stream.next());
    }

    @Test
    void defaultWeatherStreamStartsClearAndSometimesChanges() {
        WeatherStream stream = new WeatherStream();
        assertEquals(Weather.CLEAR, stream.next());
        long kinds = IntStream.range(0, 2000).mapToObj(i -> stream.next()).distinct().count();
        assertTrue(kinds > 1, "over 2000 turns the weather should change at least once");
    }

    // ---------- Zombie behaviour (lambdas + decorators) ----------

    @Test
    void zombieChasesAHumanWhenItCanMove() {
        Zombie z = new Zombie(0, 0);
        Human h = new Human(5, 0);
        z.setConditions(new WorldConditions(true, Weather.CLEAR)); // night + clear = full speed
        z.resetPlan();
        z.move(18, 18, new Entity[] { z, h }, openGrid(18));
        assertEquals(1, z.getNextX());
        assertEquals(0, z.getNextY());
    }

    @Test
    void zombieSidestepsInsteadOfFreezingWhenAnotherZombieBlocksTheWay() {
        Zombie front = new Zombie(1, 0);
        Zombie back = new Zombie(0, 0);
        Human h = new Human(5, 0);
        back.setConditions(new WorldConditions(true, Weather.CLEAR));
        back.resetPlan();
        back.move(18, 18, new Entity[] { front, back, h }, openGrid(18));

        boolean stayedStill = back.getNextX() == 0 && back.getNextY() == 0;
        boolean steppedOnFriend = back.getNextX() == 1 && back.getNextY() == 0;
        assertFalse(stayedStill, "zombie should step around, not freeze");
        assertFalse(steppedOnFriend, "zombies must not share a tile");
    }

    @Test
    void zombieWandersWhenThereAreNoHumans() {
        Zombie z = new Zombie(5, 5);
        z.setConditions(new WorldConditions(true, Weather.CLEAR));
        boolean moved = false;
        for (int i = 0; i < 50 && !moved; i++) {
            z.resetPlan();
            z.move(18, 18, new Entity[] { z }, openGrid(18));
            moved = z.getNextX() != 5 || z.getNextY() != 5;
        }
        assertTrue(moved, "with nobody to chase the zombie should shuffle about");
    }

    // ---------- Observer pattern ----------

    @Test
    void listenersAreToldAboutEveryInfectionAndCure() throws WorldSetupException {
        GamePanel panel = new GamePanel();
        AtomicInteger infected = new AtomicInteger();
        AtomicInteger cured = new AtomicInteger();
        AtomicInteger gameOver = new AtomicInteger();
        panel.addListener(e -> {
            switch (e.type()) {
                case HUMAN_INFECTED: infected.incrementAndGet(); break;
                case ZOMBIE_CURED: cured.incrementAndGet(); break;
                case GAME_OVER: gameOver.incrementAndGet(); break;
                default: break;
            }
        });

        for (int i = 0; i < 1500 && !panel.isGameOver(); i++) {
            panel.step();
        }

        assertEquals(panel.getInfectionCount(), infected.get());
        assertEquals(panel.getCuredCount(), cured.get());
        assertEquals(panel.isGameOver() ? 1 : 0, gameOver.get());
    }

    @Test
    void removedListenersStopHearingEvents() throws WorldSetupException {
        GamePanel panel = new GamePanel();
        AtomicInteger heard = new AtomicInteger();
        GameListener listener = e -> heard.incrementAndGet();
        panel.addListener(listener);
        panel.removeListener(listener);
        panel.resetEntities(); // sends a RESET event
        assertEquals(0, heard.get());
    }

    @Test
    void resetSendsAResetEvent() throws WorldSetupException {
        GamePanel panel = new GamePanel();
        List<GameEvent.Type> seen = new ArrayList<>();
        panel.addListener(e -> seen.add(e.type()));
        panel.resetEntities();
        assertTrue(seen.contains(GameEvent.Type.RESET));
    }
}