// The kinds of weather the simulation can have. Each value carries its own effects,
// so the rest of the code just asks the weather what it does (no big if/else chains).
//   zombieSpeed = chance (0.0 to 1.0) that a zombie takes a step on a given turn
//   sightRange  = how many tiles away a human can notice a zombie
public enum Weather {
    CLEAR("Clear", 1.0, 5),
    FOG("Fog", 1.0, 2),     // bad for humans: they spot zombies late
    RAIN("Rain", 0.75, 4),  // zombies slip around and move less often
    STORM("Storm", 0.5, 3); // zombies are slowed a lot

    private final String label;
    private final double zombieSpeed;
    private final int sightRange;

    Weather(String label, double zombieSpeed, int sightRange) {
        this.label = label;
        this.zombieSpeed = zombieSpeed;
        this.sightRange = sightRange;
    }

    public String getLabel() { return label; }
    public double getZombieSpeed() { return zombieSpeed; }
    public int getSightRange() { return sightRange; }
}