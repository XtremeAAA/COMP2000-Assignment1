// OBSERVER pattern: anything that wants to hear about game events implements this.
// It has only one method, so it is also a functional interface - a listener can be
// written as a lambda:  gamePanel.addListener(event -> System.out.println(event));
@FunctionalInterface
public interface GameListener {
    void onEvent(GameEvent event);
}