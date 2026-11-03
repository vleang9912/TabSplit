import java.awt.*;
import java.io.IOException;

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
    private static GroupStore store;

    public TabSplit() {
        // Create the window and show the menu
        frame = new JFrame("TabSplit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        frame.setMinimumSize(new Dimension(520, 560));
        frame.setLocationRelativeTo(null); // center on screen
        store = new GroupStore(GroupStore.defaultFile());
        userGraph = loadGroup();
        userGraph.setOnChange(TabSplit::saveGroup); // set after loading so loading doesn't save
        menu = new Menu(frame, userGraph);
        frame.setContentPane(menu);
        frame.setVisible(true);
    }

    /**
     * Loads the saved group, or starts a new one if there's no save file or it's damaged
     * @return the group
     */
    private static UserGraph loadGroup() {
        if (store.exists()) {
            try {
                return store.load();
            } catch (IOException e) {
                String kept = "";
                try {
                    kept = "\nThe old file was kept as " + store.moveAside() + ".";
                } catch (IOException moveFailed) {
                    // leave it where it is, the next save will replace it
                }
                JOptionPane.showMessageDialog(frame, "Couldn't read your saved group, so TabSplit started fresh."
                    + kept, "TabSplit", JOptionPane.WARNING_MESSAGE);
            }
        }
        UserGraph group = new UserGraph();
        // Sample friends so the app can be tried right away. Added here, not in UserGraph,
        // so the tests still start with a group of just "Me".
        group.addUser("Vin Leang");
        group.addUser("John Doe");
        return group;
    }

    // called after every change, shows a message if the save fails so changes aren't lost silently
    private static void saveGroup() {
        try {
            store.save(userGraph);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Couldn't save your changes to " + store.file() + ":\n"
                + e.getMessage(), "TabSplit", JOptionPane.ERROR_MESSAGE);
        }
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
