import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * The Expenses class manages the "Expenses" tab in a user interface.
 * It allows users to add financial transactions, view expense history,
 * and calculate cash flow minimization based on a user graph.
 */
public class Expenses extends ParentTab {
    private JTextField nameCreditBox, descBox, nameDebitBox, valueBox;
    private JTextArea balances, minimizer;
    private UserGraph userGraph;
    private JButton addTransactionButton;
    private List<Transaction> transactionList;

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
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        nameCreditBox = new JTextField("Paid by", 10);
        nameDebitBox = new JTextField("Paid for", 10);
        valueBox = new JTextField("Value", 8);
        content.add(row(nameCreditBox, nameDebitBox, valueBox));

        descBox = new JTextField("Description", 10);
        content.add(row(descBox));

        addTransactionButton = new JButton("Add Transaction");
        addTransactionButton.addActionListener(e -> handleAddTransaction());
        content.add(row(addTransactionButton));

        // Each person's balance in words, next to the minimized repayments
        balances = resultArea("Balances");
        minimizer = resultArea("Settle up");
        JPanel results = new JPanel(new GridLayout(1, 2, 10, 0));
        results.setOpaque(false);
        results.add(balances);
        results.add(minimizer);
        content.add(results);

        transactionList = new ArrayList<>();
    }

    private JPanel row(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        row.setOpaque(false);
        for (JComponent component : components) {
            row.add(component);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JTextArea resultArea(String title) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBorder(BorderFactory.createTitledBorder(title));
        return area;
    }

    /**
     * Handles the "Add Transaction" button click event.
     * Adds a new transaction if the input is valid, updates the user graph,
     * and displays the updated cash flow minimization result.
     */
    private void handleAddTransaction() {
        String nameCredit = nameCreditBox.getText();
        String nameDebit = nameDebitBox.getText();

        if (userGraph.StringIntegerMap.containsKey(nameCredit) && userGraph.StringIntegerMap.containsKey(nameDebit)) {
            int vCredit = userGraph.StringIntegerMap.get(nameCredit);
            int vDebit = userGraph.StringIntegerMap.get(nameDebit);

            System.out.println(vCredit);
            System.out.println(vDebit);
            System.out.println(userGraph.IntegerStringMap.get(vCredit));
            System.out.println(userGraph.IntegerStringMap.get(vDebit));

            String valueText = valueBox.getText();
            String description = descBox.getText();

            if (valueText.isEmpty()) {
                return; // Invalid inputs
            }

            double value = Double.parseDouble(valueText);
            Transaction transac = new Transaction(vCredit, vDebit, value, description);
            transactionList.add(transac);
            userGraph.updateEdge(transac);
            balances.setText(userGraph.balanceSummary());
            minimizer.setText(userGraph.cashFlowMin());

            UserGraph.matrixPrint(userGraph.matrix);
        }
    }

    public List<Transaction> getTransactions() {
        return transactionList;
    }
}
