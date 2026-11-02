// A small read-only snapshot of "what the world is like right now".
// GamePanel makes a fresh one every step and hands it to every entity BEFORE the
// parallel phase starts. Because a record can never change after it is created,
// all the parallel threads can read it at the same time safely.
public record WorldConditions(boolean night, Weather weather) {

    public static final WorldConditions DEFAULT = new WorldConditions(false, Weather.CLEAR);
}