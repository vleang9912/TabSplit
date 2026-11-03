import java.awt.*;

import javax.swing.*;

/**
 * The main application class for the TabSplit App.
 *
 * This class initializes the graphical user interface (GUI), including the window and menu, and
 * integrates it with the `UserGraph` to simulate the TabSplit application.
 */
public class TabSplit {

    private static final int CANVAS_WIDTH = 640;
    private static final int CANVAS_HEIGHT = 760;

    private static JFrame frame;
    private static Menu menu;
    public static UserGraph userGraph;

    public TabSplit() {
        // Create the window and show the menu
        userGraph = new UserGraph();
        // Sample friends so the app can be tried right away. Added here, not in UserGraph,
        // so the tests still start with a group of just "Me".
        userGraph.addUser("Vin Leang");
        userGraph.addUser("John Doe");
        frame = new JFrame("TabSplit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        frame.setMinimumSize(new Dimension(520, 560));
        frame.setLocationRelativeTo(null); // center on screen
        menu = new Menu(frame, userGraph);
        frame.setContentPane(menu);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Use the operating system's own button and text field styles
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Keep Swing's default look
        }
        // Swing components must be created on the event dispatch thread
        SwingUtilities.invokeLater(TabSplit::new);
    }
}
