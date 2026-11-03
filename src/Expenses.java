import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * The Expenses class manages the "Expenses" tab in a user interface.
 * It allows users to add financial transactions, view expense history,
 * and calculate cash flow minimization based on a user graph.
 */
public class Expenses extends ParentTab {
    // Large enough for any real expense, small enough that adding up cents can't overflow
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private JComboBox<String> paidByBox;
    // One checkbox per person, in the same order as the group
    private List<JCheckBox> splitBoxes = new ArrayList<>();
    private JPanel splitPanel;
    private JTextField valueBox, descBox;
    private JLabel errorLabel;
    private JButton addTransactionButton;
    private DefaultListModel<String> historyModel = new DefaultListModel<>();
    private DefaultListModel<String> balancesModel = new DefaultListModel<>();
    private DefaultListModel<String> settleModel = new DefaultListModel<>();
    private UserGraph userGraph;
    private List<Transaction> transactionList = new ArrayList<>();
    // One line per expense, e.g. "You paid $80.00 — dinner, split between you, Harold and Vin"
    private List<String> history = new ArrayList<>();

    /**
     * Constructs an Expenses tab.
     *
     * @param onBackButtonClick Handler for the back button click event.
     * @param userGraph The graph representing the relationships between users and their transactions.
     */
    public Expenses(BackButtonHandler onBackButtonClick, UserGraph userGraph) {
        super("Expenses", onBackButtonClick);
        this.userGraph = userGraph;
        setupUI();
    }

    /**
     * Sets up the user interface components for the Expenses tab.
     */
    private void setupUI() {
        content.setLayout(new BorderLayout(0, 12));

        paidByBox = new JComboBox<>();
        splitPanel = new JPanel(new GridLayout(0, 4, 8, 0)); // rows of 4 names
        splitPanel.setOpaque(false);
        valueBox = new JTextField(8);
        descBox = new JTextField(12);
        // Enter in either text field adds the expense
        valueBox.addActionListener(e -> handleAddTransaction());
        descBox.addActionListener(e -> handleAddTransaction());

        addTransactionButton = new JButton("Add expense");
        addTransactionButton.addActionListener(e -> handleAddTransaction());
        errorLabel = errorLabel();

        // Two columns of "Label: field"
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, 0, 1, "Paid by:", paidByBox);
        addField(form, 2, 0, 1, "Amount ($):", valueBox);
        addField(form, 0, 1, 3, "Description:", descBox);
        addField(form, 0, 2, 3, "Split between:", splitPanel);

        JPanel buttonRow = new JPanel(new BorderLayout(10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(addTransactionButton, BorderLayout.WEST);
        buttonRow.add(errorLabel, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(buttonRow, c);
        content.add(card("Add an expense", form), BorderLayout.NORTH);

        // History on top, balances and repayments side by side below it
        JPanel results = new JPanel(new GridLayout(1, 2, 12, 0));
        results.setOpaque(false);
        results.add(card("Balances", plainList(balancesModel, null)));
        results.add(card("Settle up", plainList(settleModel, null)));

        JPanel lists = new JPanel(new GridLayout(2, 1, 0, 12));
        lists.setOpaque(false);
        lists.add(card("History", plainList(historyModel, "No expenses yet.")));
        lists.add(results);
        content.add(lists, BorderLayout.CENTER);

        refreshResults();
    }

    private void addField(JPanel form, int column, int row, int width, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.anchor = GridBagConstraints.BASELINE_TRAILING; // line the label's text up with the field's
        c.insets = new Insets(4, column == 0 ? 0 : 16, 4, 6);
        form.add(new JLabel(labelText), c);

        c.gridx = column + 1;
        c.gridwidth = width; // how many columns the field stretches across
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        form.add(field, c);
    }

    /**
     * Refreshes the "Paid by" dropdown and the "Split between" checkboxes with the current group,
     * keeping what was already picked. New people start out checked.
     */
    @Override
    public void onShow() {
        int paidBy = paidByBox.getSelectedIndex();
        paidByBox.removeAllItems();
        for (int i = 0; i < userGraph.V(); i++) {
            paidByBox.addItem(userGraph.IntegerStringMap.get(i));
        }
        paidByBox.setSelectedIndex(paidBy >= 0 ? paidBy : 0);

        // People are only ever added, never removed, so existing checkboxes stay in the right order
        for (int i = splitBoxes.size(); i < userGraph.V(); i++) {
            JCheckBox box = new JCheckBox(userGraph.IntegerStringMap.get(i), true);
            box.setOpaque(false);
            splitBoxes.add(box);
            splitPanel.add(box);
        }

        boolean hasFriends = userGraph.V() > 1;
        for (JComponent field : new JComponent[] {paidByBox, valueBox, descBox, addTransactionButton}) {
            field.setEnabled(hasFriends);
        }
        for (JCheckBox box : splitBoxes) {
            box.setEnabled(hasFriends);
        }
        errorLabel.setText(hasFriends ? " " : "Add friends first.");
        refreshResults();
    }

    /**
     * Handles the "Add expense" button click event.
     * Adds a new transaction if the input is valid, updates the user graph,
     * and displays the updated cash flow minimization result. Otherwise shows what's wrong.
     */
    private void handleAddTransaction() {
        if (userGraph.V() < 2) {
            errorLabel.setText("Add friends first.");
            return;
        }
        int payer = paidByBox.getSelectedIndex();
        List<Integer> splitWith = new ArrayList<>();
        for (int i = 0; i < splitBoxes.size(); i++) {
            if (splitBoxes.get(i).isSelected()) {
                splitWith.add(i);
            }
        }
        if (splitWith.isEmpty()) {
            errorLabel.setText("Pick who to split it with.");
            return;
        }
        if (splitWith.equals(List.of(payer))) {
            errorLabel.setText("Pick someone besides the person who paid.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(valueBox.getText().trim().replace("$", "").replace(",", ""));
        } catch (NumberFormatException e) {
            errorLabel.setText(valueBox.getText().isBlank() ? "Enter an amount." : "Amount must be a number.");
            return;
        }
        // Anything below half a cent would round to $0.00
        if (amount.compareTo(new BigDecimal("0.005")) < 0) {
            errorLabel.setText("Amount must be greater than 0.");
            return;
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            errorLabel.setText("Amount is too large.");
            return;
        }

        String description = descBox.getText().trim();
        try {
            transactionList.addAll(userGraph.addEvenSplit(payer, splitWith, amount.doubleValue(), description));
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            return;
        }
        history.add(describeExpense(payer, splitWith, amount, description));

        errorLabel.setText(" ");
        valueBox.setText("");
        descBox.setText("");
        valueBox.requestFocusInWindow();
        refreshResults();
    }

    /**
     * Shows the history, everyone's balance and the payments needed to settle up.
     */
    private void refreshResults() {
        historyModel.clear();
        for (String line : history) {
            historyModel.addElement(line);
        }

        balancesModel.clear();
        for (int i = 0; i < userGraph.V(); i++) {
            balancesModel.addElement(userGraph.describeBalance(i));
        }

        settleModel.clear();
        List<UserGraph.Payment> payments = userGraph.settleUp();
        if (payments.isEmpty()) {
            settleModel.addElement("Everyone is settled up.");
        }
        for (UserGraph.Payment payment : payments) {
            settleModel.addElement(userGraph.describePayment(payment));
        }
    }

    /**
     * Describes an expense in words, e.g. "You paid $80.00 — dinner, split between you, Harold and
     * Vin", or "Harold paid $20.00 for you — pizza" when it was for one other person.
     *
     * @return the sentence.
     */
    private String describeExpense(int payer, List<Integer> splitWith, BigDecimal amount, String description) {
        String money = UserGraph.formatMoney(amount.doubleValue());
        String text = userGraph.displayName(payer) + " paid " + money;
        String what = description.isEmpty() ? "" : " — " + description;

        if (splitWith.size() == 1) {
            return text + " for " + lowercaseYou(splitWith.get(0)) + what;
        }
        List<String> names = new ArrayList<>();
        for (int person : splitWith) {
            names.add(lowercaseYou(person));
        }
        String last = names.remove(names.size() - 1);
        return text + what + ", split between " + String.join(", ", names) + " and " + last;
    }

    /** "you" for the user "Me", otherwise the person's name. */
    private String lowercaseYou(int person) {
        return person == 0 ? "you" : userGraph.displayName(person);
    }

    public List<Transaction> getTransactions() {
        return transactionList;
    }
}
