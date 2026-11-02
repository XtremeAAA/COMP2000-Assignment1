import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

// Weather is something that happens TO the simulation from outside. We do not know
// the whole future forecast, so it is modelled as an endless (infinite) stream:
// every time the simulation ticks over, it asks the stream for the next weather.
//
// Stream.iterate(seed, rule) builds the infinite stream lazily - the next value is
// only worked out when it is asked for. The rule (UnaryOperator) is a lambda, so a
// test can plug in a different rule without changing this class.
public class WeatherStream {

    private static final double CHANCE_OF_CHANGE = 0.15;

    private final Iterator<Weather> forecast;

    public WeatherStream() {
        this(WeatherStream::nextWeather);
    }

    public WeatherStream(UnaryOperator<Weather> transitionRule) {
        this.forecast = Stream.iterate(Weather.CLEAR, transitionRule).iterator();
    }

    // The next weather in the stream. The first call always returns CLEAR.
    public Weather next() {
        return forecast.next();
    }

    // Default rule: most turns the weather stays the same; sometimes it changes
    // to a different kind of weather picked at random.
    static Weather nextWeather(Weather current) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        if (rand.nextDouble() >= CHANCE_OF_CHANGE) {
            return current;
        }
        Weather[] all = Weather.values();
        Weather pick;
        do {
            pick = all[rand.nextInt(all.length)];
        } while (pick == current);
        return pick;
    }
}