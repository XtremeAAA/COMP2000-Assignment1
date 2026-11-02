// Something that happened in the simulation. GamePanel (the subject) sends these
// to every GameListener (the observers).
// Every event also records the weather and whether it was night when it happened, so
// listeners (like StatsRecorder) can analyse events later without asking GamePanel.
public record GameEvent(Type type, String message, int day, Weather weather, boolean night) {

    public enum Type {
        STEP,            // sent once at the start of every simulation turn
        HUMAN_INFECTED,
        ZOMBIE_CURED,
        CURE_PICKED_UP,
        CURE_SPAWNED,
        DAY_BROKE,
        NIGHT_FELL,
        WEATHER_CHANGED,
        GAME_OVER,
        RESET
    }
}