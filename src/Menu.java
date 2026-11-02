import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Represents the main menu of the TabSplit App.
 * This menu provides options to navigate between the "Friends" and "Expenses" tabs or quit the application.
 */
public class Menu extends JPanel {

    private static final int MENU_WIDTH = 525;
    private static final int MENU_HEIGHT = 725;
    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 800;
    private static final int BORDER_WIDTH = (CANVAS_WIDTH - MENU_WIDTH) / 2;
    private static final int BORDER_HEIGHT = (CANVAS_HEIGHT - MENU_HEIGHT) / 2;

    private UserGraph userGraph;
    private JButton friendsTab = new JButton("Friends");
    private JButton transactionsTab = new JButton("Expenses");
    private JButton quitButton = new JButton("Quit App");
    private JFrame frame;

    /**
     * Constructs a Menu object and sets up the main menu interface.
     *
     * @param frame     The window where the menu is displayed.
     * @param userGraph The graph containing user data and relationships.
     */
    public Menu(JFrame frame, UserGraph userGraph) {
        this.frame = frame;
        this.userGraph = userGraph;

        initializeListeners();

        // Background overlay
        setLayout(new BorderLayout());
        setBackground(new Color(200, 200, 200)); // Light gray background
        setBorder(new EmptyBorder(BORDER_HEIGHT, BORDER_WIDTH, BORDER_HEIGHT, BORDER_WIDTH));

        // Menu box
        JPanel menuBox = new JPanel();
        menuBox.setLayout(new BoxLayout(menuBox, BoxLayout.Y_AXIS));
        menuBox.setBackground(Color.WHITE);
        menuBox.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 3), new EmptyBorder(30, 0, 0, 0)));
        add(menuBox, BorderLayout.CENTER);

        // Title text
        JLabel title = new JLabel("TabSplit");
        title.setForeground(Color.RED);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 30));
        addCentered(menuBox, title);
        menuBox.add(Box.createVerticalStrut(20));

        // Description text
        JLabel description = new JLabel("Welcome to TabSplit! Select a Tab:");
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        addCentered(menuBox, description);
        menuBox.add(Box.createVerticalStrut(80));

        // Add buttons
        addCentered(menuBox, friendsTab);
        menuBox.add(Box.createVerticalStrut(20));
        addCentered(menuBox, transactionsTab);
        menuBox.add(Box.createVerticalStrut(70));
        addCentered(menuBox, quitButton);
    }

    private void addCentered(JPanel panel, JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(component);
    }

    /**
     * Initializes button click listeners for navigating between tabs and quitting the app.
     */
    private void initializeListeners() {
        friendsTab.addActionListener(e -> switchToTab(new FriendsTab(this::returnToMenu, userGraph)));
        transactionsTab.addActionListener(e -> switchToTab(new Expenses(this::returnToMenu, userGraph)));
        quitButton.addActionListener(e -> System.exit(0));
    }

    /**
     * Switches the window to display the specified tab.
     *
     * @param tab The tab to display in the window.
     */
    private void switchToTab(JPanel tab) {
        frame.setContentPane(tab); // Replace whatever is showing with the new tab
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Returns to the main menu by putting this menu back in the window.
     */
    private void returnToMenu() {
        switchToTab(this);
    }

    /**
     * Returns the quit button for external access (e.g., testing).
     *
     * @return The quit button.
     */
    public JButton getQuitButton() {
        return quitButton;
    }
}
