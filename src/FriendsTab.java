import java.awt.*;

import javax.swing.*;

/**
 * Represents the "Friends" tab in the user interface. This tab allows users to add new members to a
 * user group and displays the current user list.
 */
public class FriendsTab extends ParentTab {
    private JTextField newUserField;
    public UserGraph userGraph;
    private JButton addUserButton;
    private JLabel errorLabel, countLabel;
    private DefaultListModel<String> groupModel = new DefaultListModel<>();

    /**
     * Creates a new FriendsTab with functionality to add users and display the user list.
     *
     * @param onBackButtonClick Callback handler for when the back button is clicked.
     * @param userGraph         The user graph managing the relationships between users.
     */
    public FriendsTab(BackButtonHandler onBackButtonClick, UserGraph userGraph) {
        super("Friends", onBackButtonClick);
        this.userGraph = userGraph;
        content.setLayout(new BorderLayout(0, 12));

        // Input row: "Name: [field] [Add to group]", with errors underneath
        newUserField = new JTextField(16);
        newUserField.addActionListener(e -> handleAddUser()); // Enter adds the name
        addUserButton = new JButton("Add to group");
        addUserButton.addActionListener(e -> handleAddUser());
        errorLabel = errorLabel();

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(new JLabel("Name:"), BorderLayout.WEST);
        inputRow.add(newUserField, BorderLayout.CENTER);
        inputRow.add(addUserButton, BorderLayout.EAST);

        JPanel form = new JPanel(new BorderLayout(0, 4));
        form.setOpaque(false);
        form.add(inputRow, BorderLayout.NORTH);
        form.add(errorLabel, BorderLayout.SOUTH);
        content.add(card("Add a friend", form), BorderLayout.NORTH);

        // The group, one person per line
        countLabel = new JLabel();
        countLabel.setForeground(MUTED);
        JPanel groupPanel = new JPanel(new BorderLayout(0, 6));
        groupPanel.setOpaque(false);
        groupPanel.add(plainList(groupModel, null), BorderLayout.CENTER);
        groupPanel.add(countLabel, BorderLayout.SOUTH);
        content.add(card("Group", groupPanel), BorderLayout.CENTER);

        refreshGroup();
    }

    @Override
    public void onShow() {
        errorLabel.setText(" ");
        newUserField.requestFocusInWindow();
    }

    /**
     * Handles the action of adding a new user when the "Add to group" button is clicked or Enter is
     * pressed. Shows a message instead of adding the name if it is empty, already in the group, or
     * the group is full.
     */
    private void handleAddUser() {
        String userName = newUserField.getText();
        String problem = userGraph.checkNewUser(userName);
        if (problem != null) {
            errorLabel.setText(problem);
            return;
        }

        userGraph.addUser(userName);
        errorLabel.setText(" ");
        newUserField.setText("");
        newUserField.requestFocusInWindow();
        refreshGroup();
    }

    /**
     * Shows the current group in the list, with "(you)" after your own name.
     */
    private void refreshGroup() {
        groupModel.clear();
        for (int i = 0; i < userGraph.V(); i++) {
            String name = userGraph.IntegerStringMap.get(i);
            groupModel.addElement(i == 0 ? name + " (you)" : name);
        }
        int size = userGraph.V();
        countLabel.setText(size + " of " + userGraph.getLimit() + " people"
            + (size >= userGraph.getLimit() ? " — the group is full" : ""));
        addUserButton.setEnabled(size < userGraph.getLimit());
    }
}
