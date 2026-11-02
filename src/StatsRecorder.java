import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// An OBSERVER that does not draw anything: it just remembers every game event.
// When the game ends, STREAM pipelines turn that list of events into an answer to a
// real question about the simulation: "which weather is the deadliest for humans?"
//
// Raw counts would be unfair (CLEAR weather lasts longer, so it would always "win"),
// so everything is measured as a RATE: infections per 10 turns spent in that weather.
public class StatsRecorder implements GameListener {

    // A weather needs at least this many turns before we trust its rate.
    private static final int MIN_TURNS = 3;

    private final List<GameEvent> events = new ArrayList<>();

    @Override
    public void onEvent(GameEvent event) {
        if (event.type() == GameEvent.Type.RESET) {
            events.clear();   // a new game starts with a clean record
        } else {
            events.add(event);
        }
    }

    // Lines of text for the end screen (easy words, short).
    public List<String> summary() {
        List<String> lines = new ArrayList<>();

        long infections = count(GameEvent.Type.HUMAN_INFECTED);
        long cures = count(GameEvent.Type.ZOMBIE_CURED);
        int days = events.stream().mapToInt(GameEvent::day).max().orElse(1);

        lines.add("Lasted " + days + " day" + (days == 1 ? "" : "s")
                + "  |  Infections: " + infections + "  |  Zombies cured: " + cures);

        // Turns spent in each weather: group the STEP events by weather and count them.
        Map<Weather, Long> turns = events.stream()
                .filter(e -> e.type() == GameEvent.Type.STEP)
                .collect(Collectors.groupingBy(GameEvent::weather,
                        () -> new EnumMap<>(Weather.class), Collectors.counting()));

        // Infections in each weather: same idea, different event type.
        Map<Weather, Long> infected = events.stream()
                .filter(e -> e.type() == GameEvent.Type.HUMAN_INFECTED)
                .collect(Collectors.groupingBy(GameEvent::weather,
                        () -> new EnumMap<>(Weather.class), Collectors.counting()));

        // Highest rate wins. Optional is empty when no weather lasted long enough.
        Optional<Map.Entry<Weather, Double>> deadliest = turns.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_TURNS)
                .map(e -> Map.entry(e.getKey(), rate(infected.getOrDefault(e.getKey(), 0L), e.getValue())))
                .max(Map.Entry.comparingByValue());

        lines.add(deadliest
                .map(e -> String.format("Deadliest weather: %s (%.1f infections per 10 turns)",
                        e.getKey().getLabel(), e.getValue()))
                .orElse("Not enough turns to compare the weather"));

        // Day against night: partitioningBy splits the events into two groups (night / not night).
        Map<Boolean, Long> turnsByNight = events.stream()
                .filter(e -> e.type() == GameEvent.Type.STEP)
                .collect(Collectors.partitioningBy(GameEvent::night, Collectors.counting()));
        Map<Boolean, Long> infectedByNight = events.stream()
                .filter(e -> e.type() == GameEvent.Type.HUMAN_INFECTED)
                .collect(Collectors.partitioningBy(GameEvent::night, Collectors.counting()));

        if (turnsByNight.get(true) >= MIN_TURNS && turnsByNight.get(false) >= MIN_TURNS) {
            lines.add(String.format("Night: %.1f  |  Day: %.1f  (infections per 10 turns)",
                    rate(infectedByNight.get(true), turnsByNight.get(true)),
                    rate(infectedByNight.get(false), turnsByNight.get(false))));
        }

        return lines;
    }

    private long count(GameEvent.Type type) {
        return events.stream().filter(e -> e.type() == type).count();
    }

    private static double rate(long infections, long turns) {
        return turns == 0 ? 0.0 : infections * 10.0 / turns;
    }
}