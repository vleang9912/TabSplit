import javax.swing.*;

/**
 * The main application class for the TabSplit App.
 *
 * This class initializes the graphical user interface (GUI), including the window and menu, and
 * integrates it with the `UserGraph` to simulate the TabSplit application.
 */
public class TabSplit {

    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 800;

    private static JFrame frame;
    private static Menu menu;
    public static UserGraph userGraph;

        public TabSplit() {
            // Create the window and show the menu
            userGraph = new UserGraph();
            frame = new JFrame("TabSplit");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
            menu = new Menu(frame, userGraph);
            frame.setContentPane(menu);
            frame.setVisible(true);
        }

        public static void main(String[] args) {
            // Swing components must be created on the event dispatch thread
            SwingUtilities.invokeLater(TabSplit::new);
    }
}
