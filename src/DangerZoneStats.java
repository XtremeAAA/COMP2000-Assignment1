import java.util.stream.Collector;

public final class DangerZoneStats {
    private final int humansInDanger;
    private final int zombiesInDanger;

    public DangerZoneStats(int humansInDanger, int zombiesInDanger) {
        this.humansInDanger = humansInDanger;
        this.zombiesInDanger = zombiesInDanger;
    }

    public int getHumansInDanger() { return humansInDanger; }
    public int getZombiesInDanger() { return zombiesInDanger; }
    public static Collector<Entity, int[], DangerZoneStats> collector(int dangerX, int dangerY) {
        return Collector.of(
                () -> new int[2], // supplier: [humansInDanger, zombiesInDanger]
                (acc, e) -> {     // accumulator
                    if (e.getX() == dangerX && e.getY() == dangerY) {
                        if (e instanceof Human) {
                            acc[0]++;
                        } else if (e instanceof Zombie) {
                            acc[1]++;
                        }
                    }
                },
                (a, b) -> { 
                    a[0] += b[0];
                    a[1] += b[1];
                    return a;
                },
                acc -> new DangerZoneStats(acc[0], acc[1]) // finisher
        );
    }
}