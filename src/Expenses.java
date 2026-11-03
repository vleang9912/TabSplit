import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Expenses class manages the "Expenses" tab in a user interface.
 * It allows users to add financial transactions, view expense history,
 * and calculate cash flow minimization based on a user graph.
 */
public class Expenses extends ParentTab {
    // big enough for any real expense, small enough that cents can't overflow
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private JComboBox<String> paidByBox;
    private List<SplitRow> splitRows = new ArrayList<>(); // same order as the group
    private JPanel splitPanel;
    private JLabel remainingLabel;
    private JButton clearButton;
    // true while the code fills in share boxes, so it doesn't count as typing
    private boolean updatingShares = false;
    private JTextField valueBox, descBox, dateBox;
    private JLabel errorLabel;
    private JButton addTransactionButton, cancelEditButton;
    private JButton addFriendsLink; // shown instead of the form when there's nobody to split with
    private int editingIndex = -1; // which expense the form is editing, -1 when adding a new one
    private JPanel historyRows = rowPanel();
    private DefaultListModel<String> balancesModel = new DefaultListModel<>();
    private DefaultListModel<String> settleModel = new DefaultListModel<>();
    private UserGraph userGraph;

    /**
     * One person's line in the split: name, share box and a remove/add back link
     */
    private class SplitRow {
        final int person;
        final JLabel nameLabel = new JLabel();
        final JLabel noteLabel = new JLabel();
        final JTextField shareField = new JTextField(7);
        final JButton toggleButton = linkButton("Add back");
        boolean included = true;
        boolean typed = false; // typed in by hand, so the other shares work around it

        SplitRow(int person) {
            this.person = person;
            nameLabel.setText(userGraph.IntegerStringMap.get(person));
            noteLabel.setForeground(MUTED);
            shareField.setHorizontalAlignment(JTextField.RIGHT);
            shareField.getDocument().addDocumentListener(onChange(() -> {
                if (!updatingShares) {
                    clearError();
                    // clearing a box makes it an even share again
                    typed = !shareField.getText().isBlank();
                    // swing won't let a box change while it's reporting a change, so wait a moment
                    SwingUtilities.invokeLater(Expenses.this::recalculateShares);
                }
            }));
            // a cleared box stays blank while you're in it, then gets its share when you leave
            shareField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    recalculateShares();
                }
            });

            // keep the width of the longer text so the column doesn't jump
            toggleButton.setPreferredSize(toggleButton.getPreferredSize());
            toggleButton.setHorizontalAlignment(SwingConstants.RIGHT);
            toggleButton.addActionListener(e -> {
                clearError();
                included = !included;
                typed = false;
                updateRows();
            });
        }
    }

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
     * Sets what the "Add friends first" link does, ie open the Friends screen
     * @param showFriends the code to run
     */
    public void setShowFriends(Runnable showFriends) {
        addFriendsLink.addActionListener(e -> showFriends.run());
    }

    /**
     * Sets up the user interface components for the Expenses tab.
     */
    private void setupUI() {
        content.setLayout(new BorderLayout(0, 12));

        paidByBox = new JComboBox<>();
        paidByBox.addActionListener(e -> {
            clearError();
            updateRows(); // moves the "paid the bill" note
        });
        splitPanel = new JPanel(new GridBagLayout());
        splitPanel.setOpaque(false);
        remainingLabel = new JLabel(" ");
        remainingLabel.setForeground(ERROR);
        clearButton = linkButton("Clear");
        clearButton.setToolTipText("Split evenly between everyone again");
        clearButton.addActionListener(e -> {
            clearError();
            clearSplit();
        });
        valueBox = new JTextField(8);
        valueBox.getDocument().addDocumentListener(onChange(this::recalculateShares));
        descBox = new JTextField(12);
        dateBox = new JTextField(UserGraph.formatDate(LocalDate.now()), 7);
        // an error goes away as soon as you change what it was about
        for (JTextField box : new JTextField[] {valueBox, descBox, dateBox}) {
            box.getDocument().addDocumentListener(onChange(this::clearError));
        }
        dateBox.setToolTipText("MM/DD/YY, or use the up and down arrows to change it by a day");
        // up and down arrows move the date by a day
        dateBox.getInputMap().put(KeyStroke.getKeyStroke("UP"), "nextDay");
        dateBox.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "previousDay");
        dateBox.getActionMap().put("nextDay", runAction(() -> shiftDate(1)));
        dateBox.getActionMap().put("previousDay", runAction(() -> shiftDate(-1)));
        // enter in any text field adds the expense
        valueBox.addActionListener(e -> handleAddTransaction());
        descBox.addActionListener(e -> handleAddTransaction());
        dateBox.addActionListener(e -> handleAddTransaction());

        addTransactionButton = new JButton("Add expense");
        addTransactionButton.addActionListener(e -> handleAddTransaction());
        cancelEditButton = linkButton("Cancel");
        cancelEditButton.addActionListener(e -> stopEditing());
        cancelEditButton.setVisible(false); // only while editing
        addFriendsLink = linkButton("Add friends first");
        addFriendsLink.setVisible(false); // only while it's just you
        errorLabel = errorLabel();

        // two columns of "Label: field"
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, 0, 1, "Paid by:", paidByBox);
        addField(form, 2, 0, 1, "Amount ($):", valueBox);
        addField(form, 0, 1, 1, "Description:", descBox);
        addField(form, 2, 1, 1, "Date:", dateBox);

        // split heading and rows, full width under the fields
        JLabel splitHeading = new JLabel("SPLIT");
        splitHeading.setFont(splitHeading.getFont().deriveFont(Font.BOLD, 11f));
        splitHeading.setForeground(MUTED);
        addFullWidth(form, 2, splitHeading, 12);
        addFullWidth(form, 3, splitPanel, 2);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addTransactionButton);
        buttons.add(cancelEditButton);
        buttons.add(addFriendsLink);
        JPanel buttonRow = new JPanel(new BorderLayout(10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(buttons, BorderLayout.WEST);
        buttonRow.add(errorLabel, BorderLayout.CENTER);
        addFullWidth(form, 4, buttonRow, 8);
        content.add(card("Add an expense", form), BorderLayout.NORTH);

        // history on top, balances and repayments side by side below it
        JPanel results = new JPanel(new GridLayout(1, 2, 12, 0));
        results.setOpaque(false);
        JScrollPane balances = plainList(balancesModel, null);
        colorBalances((JList<?>) balances.getViewport().getView());
        results.add(card("Balances", balances));
        results.add(card("Settle up", plainList(settleModel, null)));

        JPanel lists = new JPanel(new GridLayout(2, 1, 0, 12));
        lists.setOpaque(false);
        lists.add(card("History", rowList(historyRows)));
        lists.add(results);
        content.add(lists, BorderLayout.CENTER);

        refreshResults();
    }

    private void addField(JPanel form, int column, int row, int width, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.anchor = GridBagConstraints.BASELINE_TRAILING; // line up the label's text with the field's
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

    private void addFullWidth(JPanel form, int row, JComponent component, int topGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(topGap, 0, 0, 0);
        form.add(component, c);
    }

    /**
     * Lays out one line per person (name, note, $ box, link), then the remaining/Clear line
     */
    private void layoutSplitPanel() {
        splitPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 6);
        for (int i = 0; i < splitRows.size(); i++) {
            SplitRow row = splitRows.get(i);
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 1; // name takes the spare space so everything after it sits on the right
            c.anchor = GridBagConstraints.WEST;
            splitPanel.add(row.nameLabel, c);
            c.weightx = 0;
            c.gridx = 1;
            c.anchor = GridBagConstraints.EAST; // note sits right before the $
            splitPanel.add(row.noteLabel, c);
            c.gridx = 2;
            JLabel dollar = new JLabel("$");
            dollar.setForeground(MUTED);
            splitPanel.add(dollar, c);
            c.gridx = 3;
            splitPanel.add(row.shareField, c);
            c.gridx = 4;
            splitPanel.add(row.toggleButton, c);
        }

        c.gridy = splitRows.size();
        c.gridx = 0;
        c.gridwidth = 4;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        splitPanel.add(remainingLabel, c);
        c.gridx = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        splitPanel.add(clearButton, c);
        splitPanel.revalidate();
    }

    /**
     * Refreshes the "Paid by" dropdown and split rows in case people were added, renamed or removed
     */
    @Override
    public void onShow() {
        List<String> rowNames = new ArrayList<>();
        for (SplitRow row : splitRows) {
            rowNames.add(row.nameLabel.getText());
        }
        if (!rowNames.equals(userGraph.getUserList())) {
            // group changed, so start the form fresh with a row per person
            int paidBy = paidByBox.getSelectedIndex();
            paidByBox.removeAllItems();
            for (int i = 0; i < userGraph.V(); i++) {
                paidByBox.addItem(userGraph.IntegerStringMap.get(i));
            }
            paidByBox.setSelectedIndex(paidBy >= 0 && paidBy < userGraph.V() ? paidBy : 0);

            splitRows.clear();
            for (int i = 0; i < userGraph.V(); i++) {
                splitRows.add(new SplitRow(i));
            }
            layoutSplitPanel();
            stopEditing();
        }

        boolean hasFriends = userGraph.V() > 1;
        for (JComponent field : new JComponent[] {paidByBox, valueBox, descBox, dateBox, addTransactionButton}) {
            field.setEnabled(hasFriends);
        }
        addFriendsLink.setVisible(!hasFriends);
        errorLabel.setText(" ");
        // enter anywhere in the form adds the expense, and the button shows it's the main one
        getRootPane().setDefaultButton(addTransactionButton);
        updateRows();
        refreshResults();
    }

    /**
     * Updates how each row looks (left out or not, who paid), then recalculates the shares
     */
    private void updateRows() {
        int payer = paidByBox.getSelectedIndex();
        updatingShares = true;
        for (SplitRow row : splitRows) {
            row.toggleButton.setText(row.included ? "Remove" : "Add back");
            row.shareField.setEnabled(row.included);
            row.nameLabel.setForeground(row.included ? Color.BLACK : MUTED);
            row.noteLabel.setText(row.person == payer ? "paid the bill" : "");
            if (!row.included) {
                row.shareField.setText("");
            }
        }
        updatingShares = false;
        recalculateShares();
    }

    /**
     * Fills in every share that wasn't typed in so they add up to the amount, ie $80 with John at $32
     * gives the other two $24 each
     */
    private void recalculateShares() {
        if (updatingShares) {
            return;
        }
        List<SplitRow> included = includedRows();
        Long total = parseCents(valueBox.getText());

        // typed shares stay as they are, null means work it out
        Long[] typedIn = new Long[included.size()];
        boolean typedSharesValid = true;
        for (int i = 0; i < included.size(); i++) {
            SplitRow row = included.get(i);
            if (row.typed) {
                typedIn[i] = parseCents(row.shareField.getText());
                if (typedIn[i] == null) {
                    typedSharesValid = false;
                    typedIn[i] = 0L;
                }
            }
        }

        updatingShares = true;
        try {
            if (total == null) {
                // no amount yet, nothing to split
                for (SplitRow row : included) {
                    if (!row.typed) {
                        row.shareField.setText("");
                    }
                }
                remainingLabel.setText(" ");
                return;
            }
            long[] shares = UserGraph.fillShares(total, typedIn);
            long sum = 0;
            for (int i = 0; i < included.size(); i++) {
                SplitRow row = included.get(i);
                if (!row.typed && !row.shareField.isFocusOwner()) {
                    row.shareField.setText(plainAmount(shares[i]));
                }
                row.shareField.setForeground(row.typed ? Color.BLACK : MUTED); // worked out = grey
                sum += shares[i];
            }

            long left = total - sum;
            if (!typedSharesValid) {
                remainingLabel.setText("Check the shares you typed.");
            } else if (left > 0) {
                remainingLabel.setText(UserGraph.formatCents(left) + " left to assign");
            } else if (left < 0) {
                remainingLabel.setText(UserGraph.formatCents(-left) + " over the amount");
            } else {
                remainingLabel.setText(" ");
            }
        } finally {
            updatingShares = false;
        }
    }

    /**
     * Includes everyone again and goes back to an even split
     */
    private void clearSplit() {
        for (SplitRow row : splitRows) {
            row.included = true;
            row.typed = false;
        }
        updateRows();
    }

    private List<SplitRow> includedRows() {
        List<SplitRow> included = new ArrayList<>();
        for (SplitRow row : splitRows) {
            if (row.included) {
                included.add(row);
            }
        }
        return included;
    }

    /**
     * Handles the "Add expense" button click event.
     * Adds a new transaction if the input is valid, updates the user graph,
     * and displays the updated cash flow minimization result. Otherwise shows what's wrong.
     */
    private void handleAddTransaction() {
        if (userGraph.V() < 2) {
            return; // the form is turned off then, the "Add friends first" link shows instead
        }
        int payer = paidByBox.getSelectedIndex();
        List<SplitRow> included = includedRows();
        if (included.isEmpty()) {
            errorLabel.setText("Pick who to split it with.");
            return;
        }
        if (included.size() == 1 && included.get(0).person == payer) {
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
        // anything under half a cent rounds to $0.00
        if (amount.compareTo(new BigDecimal("0.005")) < 0) {
            errorLabel.setText("Amount must be greater than 0.");
            return;
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            errorLabel.setText("Amount is too large.");
            return;
        }
        long totalCents = parseCents(valueBox.getText());

        List<Integer> splitWith = new ArrayList<>();
        long[] shares = new long[included.size()];
        for (int i = 0; i < included.size(); i++) {
            Long share = parseCents(included.get(i).shareField.getText());
            if (share == null) {
                errorLabel.setText("Each share must be a number of 0 or more.");
                return;
            }
            splitWith.add(included.get(i).person);
            shares[i] = share;
        }

        LocalDate date = UserGraph.parseDate(dateBox.getText(), LocalDate.now());
        if (date == null) {
            errorLabel.setText("Date should look like " + UserGraph.formatDate(LocalDate.now()) + ".");
            return;
        }

        String description = descBox.getText().trim();
        try {
            if (editingIndex >= 0) {
                userGraph.replaceExpense(editingIndex, payer, splitWith, totalCents, shares, description, date);
            } else {
                userGraph.addSplit(payer, splitWith, totalCents, shares, description, date);
            }
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            return;
        }
        stopEditing();
        valueBox.requestFocusInWindow();
    }

    /**
     * Loads expense index back into the form, the button then saves over it instead of adding
     */
    private void startEditing(int index) {
        Expense expense = userGraph.getExpenses().get(index);
        long[] shares = expense.shares();
        boolean even = isEvenSplit(expense);

        editingIndex = index;
        paidByBox.setSelectedIndex(expense.payer());
        descBox.setText(expense.description());
        dateBox.setText(UserGraph.formatDate(expense.date()));
        // even splits stay worked out, uneven ones come back as typed shares
        for (SplitRow row : splitRows) {
            int position = expense.splitWith().indexOf(row.person);
            row.included = position >= 0;
            row.typed = !even && position >= 0;
            updatingShares = true;
            row.shareField.setText(row.typed ? plainAmount(shares[position]) : "");
            updatingShares = false;
        }
        valueBox.setText(plainAmount(expense.totalCents()));
        updateRows();

        addTransactionButton.setText("Save changes");
        cancelEditButton.setVisible(true);
        errorLabel.setText(" ");
        refreshResults();
    }

    /**
     * Leaves edit mode (or just resets after adding), empties the form and goes back to "Add expense"
     */
    private void stopEditing() {
        editingIndex = -1;
        addTransactionButton.setText("Add expense");
        cancelEditButton.setVisible(false);
        errorLabel.setText(" ");
        valueBox.setText("");
        descBox.setText("");
        dateBox.setText(UserGraph.formatDate(LocalDate.now())); // back to today for the next one
        clearSplit();
        refreshResults();
    }

    /**
     * Asks first, then deletes expense index
     */
    private void deleteExpense(int index) {
        String text = describeExpense(userGraph.getExpenses().get(index));
        int answer = JOptionPane.showConfirmDialog(this, "Delete this expense?\n" + text, "Delete expense",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        userGraph.removeExpense(index);
        if (editingIndex == index) {
            stopEditing(); // the one being edited is gone
        } else if (editingIndex > index) {
            editingIndex--; // it moved up one place
        }
        refreshResults();
    }

    /**
     * Shows the history, everyone's balance and the payments needed to settle up.
     */
    private void refreshResults() {
        historyRows.removeAll();
        List<Expense> expenses = userGraph.getExpenses();
        if (expenses.isEmpty()) {
            historyRows.add(emptyHint("No expenses yet."));
        }
        // newest first, index is still the expense's place in the list for edit and delete
        for (int index : userGraph.historyOrder()) {
            JButton edit = linkButton("Edit");
            edit.addActionListener(e -> startEditing(index));
            JButton delete = linkButton("Delete");
            delete.addActionListener(e -> deleteExpense(index));
            JPanel row = actionRow(describeExpense(expenses.get(index)), edit, delete);
            if (index == editingIndex) {
                // highlight the one in the form
                row.setOpaque(true);
                row.setBackground(new Color(232, 241, 255));
            }
            historyRows.add(row);
        }
        historyRows.revalidate();
        historyRows.repaint();

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
     * Describes an expense in words, ie "Nov 3 · You paid $80.00 — dinner, split between you, Vin and John",
     * or with each share listed when the split isn't even
     * @return the sentence for the history list
     */
    private String describeExpense(Expense expense) {
        List<Integer> splitWith = expense.splitWith();
        long[] shares = expense.shares();
        long totalCents = expense.totalCents();
        String text = shortDate(expense.date()) + " · " + userGraph.displayName(expense.payer()) + " paid "
            + UserGraph.formatCents(totalCents);
        String what = expense.description().isEmpty() ? "" : " — " + expense.description();

        if (splitWith.size() == 1) {
            return text + " for " + lowercaseYou(splitWith.get(0)) + what;
        }
        boolean even = isEvenSplit(expense);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < splitWith.size(); i++) {
            String name = lowercaseYou(splitWith.get(i));
            parts.add(even ? name : name + " " + UserGraph.formatCents(shares[i]));
        }
        String last = parts.remove(parts.size() - 1);
        String people = String.join(", ", parts) + " and " + last;
        return text + what + (even ? ", split between " + people : ": " + people);
    }

    // same shares an even split would give, extra cents included
    private static boolean isEvenSplit(Expense expense) {
        long[] shares = expense.shares();
        return Arrays.equals(shares, UserGraph.splitEvenly(expense.totalCents(), shares.length));
    }

    /**
     * Shows each balance line green if they're owed money, red if they owe, black if settled
     */
    private void colorBalances(JList<?> list) {
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // line i is person i, same order as the group
                long net = index < userGraph.V() ? userGraph.getNetCents(index) : 0;
                setForeground(net > 0 ? ParentTab.OWED : net < 0 ? ParentTab.ERROR : Color.BLACK);
                return this;
            }
        });
    }

    /** Hides the error message, called when the input changes */
    private void clearError() {
        errorLabel.setText(" ");
    }

    /** "Nov 3" for this year, "Nov 3, 2025" for other years */
    private static String shortDate(LocalDate date) {
        String pattern = date.getYear() == LocalDate.now().getYear() ? "MMM d" : "MMM d, yyyy";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Moves the date box by some days, starting from today if what's typed isn't a date
     */
    private void shiftDate(int days) {
        LocalDate date = UserGraph.parseDate(dateBox.getText(), LocalDate.now());
        dateBox.setText(UserGraph.formatDate((date == null ? LocalDate.now() : date).plusDays(days)));
    }

    /** "you" for the user "Me", otherwise the person's name */
    private String lowercaseYou(int person) {
        return person == 0 ? "you" : userGraph.displayName(person);
    }

    /**
     * Reads an amount like "26.67", "$1,000" or "5" as cents
     * @return the cents, or null if it's blank, not a number, negative or too large
     */
    private static Long parseCents(String text) {
        String cleaned = text.trim().replace("$", "").replace(",", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            BigDecimal dollars = new BigDecimal(cleaned);
            if (dollars.signum() < 0 || dollars.compareTo(MAX_AMOUNT) > 0) {
                return null;
            }
            return dollars.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    /** Formats cents for a share box without the $, ie 2667 becomes "26.67" */
    private static String plainAmount(long cents) {
        return String.format("%d.%02d", cents / 100, cents % 100);
    }

    /** A key action that runs the given code */
    private static Action runAction(Runnable code) {
        return new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { code.run(); }
        };
    }

    /** A listener that runs the action whenever a field's text changes */
    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}
