import java.awt.Point;
import java.util.Optional;

@FunctionalInterface
public interface EmergencyAlert {
    Optional<Point> findNearestDoctor(int humanX, int humanY);
}