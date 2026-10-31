import java.util.stream.Collector;

// Snapshot of the counts shown in the sidebar, produced in a single pass over the
// entities by a custom Collector (see collector()).
public final class WorldStats {
    private final int humans;
    private final int zombies;
    private final int doctors; // humans currently carrying a cure

    public WorldStats(int humans, int zombies, int doctors) {
        this.humans = humans;
        this.zombies = zombies;
        this.doctors = doctors;
    }

    public int getHumans()  { return humans; }
    public int getZombies() { return zombies; }
    public int getDoctors() { return doctors; }

    // Custom collector: counts humans, zombies and doctors in ONE pass instead of three.
    // Thread safety: every thread gets its own int[3] accumulator from the supplier, so
    // threads never share mutable state; the combiner merges two accumulators once the
    // threads finish. That makes it safe to use with a parallel stream.
    public static Collector<Entity, int[], WorldStats> collector() {
        return Collector.of(
                () -> new int[3],                                   // supplier: [humans, zombies, doctors]
                (acc, e) -> {                                       // accumulator
                    if (e instanceof Human) {
                        acc[0]++;
                        if (((Human) e).hasCure()) acc[2]++;
                    } else if (e instanceof Zombie) {
                        acc[1]++;
                    }
                },
                (a, b) -> {                                         // combiner (parallel merge)
                    a[0] += b[0];
                    a[1] += b[1];
                    a[2] += b[2];
                    return a;
                },
                acc -> new WorldStats(acc[0], acc[1], acc[2]));     // finisher
    }
}
