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

    private JComboBox<String> paidByBox, paidForBox;
    private JTextField valueBox, descBox;
    private JLabel errorLabel;
    private JButton addTransactionButton;
    private DefaultListModel<String> historyModel = new DefaultListModel<>();
    private DefaultListModel<String> balancesModel = new DefaultListModel<>();
    private DefaultListModel<String> settleModel = new DefaultListModel<>();
    private UserGraph userGraph;
    private List<Transaction> transactionList = new ArrayList<>();

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
        paidForBox = new JComboBox<>();
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
        addField(form, 0, 0, "Paid by:", paidByBox);
        addField(form, 0, 1, "Paid for:", paidForBox);
        addField(form, 2, 0, "Amount ($):", valueBox);
        addField(form, 2, 1, "Description:", descBox);

        JPanel buttonRow = new JPanel(new BorderLayout(10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(addTransactionButton, BorderLayout.WEST);
        buttonRow.add(errorLabel, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
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

    private void addField(JPanel form, int column, int row, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(4, column == 0 ? 0 : 16, 4, 6);
        form.add(new JLabel(labelText), c);

        c.gridx = column + 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        form.add(field, c);
    }

    /**
     * Refreshes the "Paid by" and "Paid for" dropdowns with the current group, keeping the people
     * that were already picked.
     */
    @Override
    public void onShow() {
        int paidBy = paidByBox.getSelectedIndex();
        int paidFor = paidForBox.getSelectedIndex();
        paidByBox.removeAllItems();
        paidForBox.removeAllItems();
        for (int i = 0; i < userGraph.V(); i++) {
            String name = i == 0 ? "Me" : userGraph.IntegerStringMap.get(i);
            paidByBox.addItem(name);
            paidForBox.addItem(name);
        }
        // By default: I paid for the first friend
        paidByBox.setSelectedIndex(paidBy >= 0 ? paidBy : 0);
        if (userGraph.V() > 1) {
            paidForBox.setSelectedIndex(paidFor >= 0 ? paidFor : 1);
        }

        boolean hasFriends = userGraph.V() > 1;
        for (JComponent field : new JComponent[] {paidByBox, paidForBox, valueBox, descBox, addTransactionButton}) {
            field.setEnabled(hasFriends);
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
        int vCredit = paidByBox.getSelectedIndex();
        int vDebit = paidForBox.getSelectedIndex();
        if (vCredit == vDebit) {
            errorLabel.setText("Pick two different people.");
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

        Transaction transac = new Transaction(vCredit, vDebit, amount.doubleValue(), descBox.getText().trim());
        transactionList.add(transac);
        userGraph.updateEdge(transac);

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
        for (Transaction transac : transactionList) {
            historyModel.addElement(describeTransaction(transac));
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
     * Describes a transaction in words, e.g. "Harold paid $20.00 for you — pizza".
     *
     * @param transac the transaction to describe.
     * @return the sentence.
     */
    private String describeTransaction(Transaction transac) {
        String payer = userGraph.displayName(transac.getVCredit());
        String paidFor = transac.getVDebit() == 0 ? "you" : userGraph.displayName(transac.getVDebit());
        String text = payer + " paid " + UserGraph.formatCents(transac.getCents()) + " for " + paidFor;
        if (!transac.getDescription().isEmpty()) {
            text += " — " + transac.getDescription();
        }
        return text;
    }

    public List<Transaction> getTransactions() {
        return transactionList;
    }
}
