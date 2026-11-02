import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

// Tests for the stream pipelines inside StatsRecorder, using made-up events.
public class StatsRecorderTests {

    private static GameEvent event(GameEvent.Type type, Weather weather, boolean night) {
        return new GameEvent(type, "test", 1, weather, night);
    }

    private static void turns(StatsRecorder r, int count, Weather weather, boolean night) {
        IntStream.range(0, count).forEach(i -> r.onEvent(event(GameEvent.Type.STEP, weather, night)));
    }

    private static void infections(StatsRecorder r, int count, Weather weather, boolean night) {
        IntStream.range(0, count).forEach(i -> r.onEvent(event(GameEvent.Type.HUMAN_INFECTED, weather, night)));
    }

    @Test
    void deadliestWeatherIsTheHighestRateNotTheHighestCount() {
        StatsRecorder r = new StatsRecorder();
        // CLEAR: 8 infections over 100 turns = 0.8 per 10 turns
        turns(r, 100, Weather.CLEAR, false);
        infections(r, 8, Weather.CLEAR, false);
        // FOG: 4 infections over 10 turns = 4.0 per 10 turns (fewer infections, but a much higher rate)
        turns(r, 10, Weather.FOG, false);
        infections(r, 4, Weather.FOG, false);

        List<String> lines = r.summary();
        assertTrue(lines.stream().anyMatch(l -> l.contains("Deadliest weather: Fog")), lines.toString());
    }

    @Test
    void weatherThatLastedTooFewTurnsIsIgnored() {
        StatsRecorder r = new StatsRecorder();
        turns(r, 50, Weather.CLEAR, false);
        infections(r, 1, Weather.CLEAR, false);
        turns(r, 1, Weather.STORM, false);          // only one turn: not enough to trust
        infections(r, 5, Weather.STORM, false);

        List<String> lines = r.summary();
        assertTrue(lines.stream().anyMatch(l -> l.contains("Deadliest weather: Clear")), lines.toString());
    }

    @Test
    void saysSoWhenThereAreNotEnoughTurns() {
        StatsRecorder r = new StatsRecorder();
        turns(r, 2, Weather.CLEAR, false);
        assertTrue(r.summary().stream().anyMatch(l -> l.contains("Not enough turns")));
    }

    @Test
    void comparesNightAndDayRates() {
        StatsRecorder r = new StatsRecorder();
        turns(r, 10, Weather.CLEAR, true);
        infections(r, 5, Weather.CLEAR, true);      // night: 5.0 per 10 turns
        turns(r, 10, Weather.CLEAR, false);
        infections(r, 1, Weather.CLEAR, false);     // day: 1.0 per 10 turns

        List<String> lines = r.summary();
        assertTrue(lines.stream().anyMatch(l -> l.contains("Night: 5.0") && l.contains("Day: 1.0")), lines.toString());
    }

    @Test
    void resetEventClearsTheRecord() {
        StatsRecorder r = new StatsRecorder();
        turns(r, 20, Weather.FOG, false);
        infections(r, 6, Weather.FOG, false);
        r.onEvent(event(GameEvent.Type.RESET, Weather.CLEAR, false));

        List<String> lines = r.summary();
        assertTrue(lines.get(0).contains("Infections: 0"), lines.toString());
    }

    @Test
    void firstLineCountsInfectionsAndCures() {
        StatsRecorder r = new StatsRecorder();
        infections(r, 3, Weather.CLEAR, false);
        r.onEvent(event(GameEvent.Type.ZOMBIE_CURED, Weather.CLEAR, false));
        assertTrue(r.summary().get(0).contains("Infections: 3"));
        assertTrue(r.summary().get(0).contains("Zombies cured: 1"));
    }
}