import java.awt.*;
import java.util.List;

import javax.swing.*;

/**
 * Represents the "Friends" tab in the user interface. This tab allows users to add new members to a
 * user group and displays the current user list.
 */
public class FriendsTab extends ParentTab {
    private JTextField newUserField;
    public List<String> users;
    public UserGraph userGraph;
    private JButton addUserButton;
    private JLabel text;

    /**
     * Creates a new FriendsTab with functionality to add users and display the user list.
     *
     * @param onBackButtonClick Callback handler for when the back button is clicked.
     * @param userGraph         The user graph managing the relationships between users.
     */
    public FriendsTab(BackButtonHandler onBackButtonClick, UserGraph userGraph) {
        super("Friends", onBackButtonClick);
        this.userGraph = userGraph;
        content.setLayout(new BorderLayout(0, 40));

        // Initialize and set up the input field for adding users
        newUserField = new JTextField("Add user", 15);

        // Initialize and set up the button for adding users
        addUserButton = new JButton("Add to group");
        addUserButton.addActionListener(e -> handleAddUser());

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        inputRow.setOpaque(false);
        inputRow.add(newUserField);
        inputRow.add(addUserButton);
        content.add(inputRow, BorderLayout.NORTH);

        // Initialize and set up the text for displaying the user list
        text = new JLabel(userGraph.getUserList().toString());
        text.setVerticalAlignment(SwingConstants.TOP);
        content.add(text, BorderLayout.CENTER);
    }

    /**
     * Handles the action of adding a new user when the "Add to group" button is clicked. This method
     * validates the input, adds the user to the `UserGraph`, and updates the displayed user list.
     *
     * @throws IllegalArgumentException If the input is invalid (e.g., empty or default text).
     */
    private void handleAddUser() {
        // Get input from the text field
        String userName = newUserField.getText();

        if (userName.isEmpty() || userName.equals("Add user")) {
            throw new IllegalArgumentException("Invalid User Name");
        } else {
            // Add the user to the graph and update the display
            userGraph.addUser(userName);
            text.setText(userGraph.getUserList().toString());
        }
    }
}
