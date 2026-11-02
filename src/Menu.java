import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Represents the main menu of the TabSplit App.
 * This menu provides options to navigate between the "Friends" and "Expenses" tabs or quit the application.
 */
public class Menu extends JPanel {

    private JButton friendsTab = new JButton("Friends");
    private JButton transactionsTab = new JButton("Expenses");
    private JButton quitButton = new JButton("Quit");
    private JFrame frame;

    // Each tab is built once and kept, so it remembers its state when you come back to it
    private ParentTab friends, expenses;

    /**
     * Constructs a Menu object and sets up the main menu interface.
     *
     * @param frame     The window where the menu is displayed.
     * @param userGraph The graph containing user data and relationships.
     */
    public Menu(JFrame frame, UserGraph userGraph) {
        this.frame = frame;
        friends = new FriendsTab(this::returnToMenu, userGraph);
        expenses = new Expenses(this::returnToMenu, userGraph);

        initializeListeners();

        // Background, with the menu centered in it
        setLayout(new GridBagLayout());
        setBackground(ParentTab.BACKGROUND);

        // Menu box
        JPanel menuBox = new JPanel();
        menuBox.setLayout(new BoxLayout(menuBox, BoxLayout.Y_AXIS));
        menuBox.setBackground(ParentTab.CARD);
        menuBox.setBorder(new CompoundBorder(new LineBorder(ParentTab.CARD_BORDER, 1, true), new EmptyBorder(40, 60, 40, 60)));
        add(menuBox);

        // Title text
        JLabel title = new JLabel("TabSplit");
        title.setForeground(ParentTab.ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        addCentered(menuBox, title);
        menuBox.add(Box.createVerticalStrut(6));

        // Description text
        JLabel description = new JLabel("Split expenses with the fewest payments.");
        description.setForeground(ParentTab.MUTED);
        addCentered(menuBox, description);
        menuBox.add(Box.createVerticalStrut(36));

        // Buttons, all the same width
        Dimension buttonSize = new Dimension(200, friendsTab.getPreferredSize().height);
        for (JButton button : new JButton[] {friendsTab, transactionsTab, quitButton}) {
            button.setPreferredSize(buttonSize);
            button.setMaximumSize(buttonSize);
        }
        addCentered(menuBox, friendsTab);
        menuBox.add(Box.createVerticalStrut(10));
        addCentered(menuBox, transactionsTab);
        menuBox.add(Box.createVerticalStrut(30));
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
        friendsTab.addActionListener(e -> switchToTab(friends));
        transactionsTab.addActionListener(e -> switchToTab(expenses));
        quitButton.addActionListener(e -> System.exit(0));
    }

    /**
     * Switches the window to display the specified tab.
     *
     * @param tab The tab to display in the window.
     */
    private void switchToTab(JPanel tab) {
        frame.setContentPane(tab); // Replace whatever is showing with the new tab
        if (tab instanceof ParentTab parentTab) {
            parentTab.onShow();
        }
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
