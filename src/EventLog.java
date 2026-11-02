import java.awt.Font;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

// An observer that shows the last few game events in a scrolling text box.
// It never asks GamePanel anything - it only reacts when an event is sent to it.
public class EventLog implements GameListener {

    private static final int MAX_LINES = 60;

    private final Deque<String> lines = new ArrayDeque<>();
    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(textArea);

    public EventLog() {
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    // The Swing component to put on screen.
    public JScrollPane getComponent() {
        return scrollPane;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event.type() == GameEvent.Type.STEP) {
            return;   // sent every turn for the recorder; too noisy for the log
        }
        if (event.type() == GameEvent.Type.RESET) {
            lines.clear();
        } else {
            lines.addLast("Day " + event.day() + ": " + event.message());
            while (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
        }
        textArea.setText(String.join("\n", lines));
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}