import java.util.*;

/**
 * A class representing a graph with an adjacency matrix implementation to track financial
 * transactions between users.
 *
 * Money is stored as whole cents ({@code long}) so floating-point leftovers such as
 * 0.1 + 0.2 - 0.3 can never show up as a "$0.00" payment.
 */
public class UserGraph {

    private static final int LIMIT = 10;

    private int V;

    // matrix[a][b] is how much person a owes person b, in cents. Values are never negative, and at
    // most one of matrix[a][b] and matrix[b][a] is nonzero.
    long[][] matrix = new long[LIMIT][LIMIT];

    private List<String> userList = new ArrayList<String>();

    public Map<Integer, String> IntegerStringMap = new TreeMap<>();
    public Map<String, Integer> StringIntegerMap = new TreeMap<>();

    /**
     * One payment needed to settle up: {@code payer} sends {@code cents} to {@code receiver}.
     */
    public record Payment(int payer, int receiver, long cents) {}

    /**
     * Constructs a new UserGraph with a fixed matrix size. Initializes the graph with a default user
     * "Me".
     */
    public UserGraph() {
        addUser("Me");
    }

    /**
     * Returns the maximum size limit of the adjacency matrix.
     *
     * @return the size limit.
     */
    public int getLimit() {
        return LIMIT;
    }

    /**
     * Returns the number of users (vertices) in the graph.
     *
     * @return the number of vertices.
     */
    public int V() {
        return V;
    }

    /**
     * Validates that a vertex index is within valid bounds.
     *
     * @param v the vertex index to validate.
     * @throws IllegalArgumentException if the vertex index is out of bounds.
     */
    private void validateVertex(int v) {
        if (v < 0 || v >= V)
            throw new IllegalArgumentException("vertex " + v + " is not between 0 and " + (V - 1));
    }

    /**
     * Returns the list of user names in the graph.
     *
     * @return a copy of the list of user names.
     */
    public List<String> getUserList() {
        return new ArrayList<>(userList);
    }

    /**
     * Returns the name to show for a user: "You" for the user "Me", otherwise their name.
     *
     * @param v the vertex index of the user.
     * @return the display name.
     */
    public String displayName(int v) {
        return v == 0 ? "You" : IntegerStringMap.get(v);
    }

    /**
     * Checks whether a name can be added to the group.
     *
     * @param name the name to check.
     * @return a message explaining why the name can't be added, or {@code null} if it can.
     */
    public String checkNewUser(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return "Enter a name first.";
        }
        if (V >= LIMIT) {
            return "The group is full (" + LIMIT + " people max).";
        }
        for (String existing : userList) {
            if (existing.equalsIgnoreCase(trimmed)) {
                return existing + " is already in the group.";
            }
        }
        return null;
    }

    /**
     * Adds a user to the graph. Each user is assigned a vertex index, and mappings between user names
     * and vertex indices are updated. Leading and trailing spaces are removed from the name.
     *
     * @param name the name of the user to add.
     * @throws IllegalArgumentException if the name is empty, already taken, or the group is full.
     */
    public void addUser(String name) {
        String problem = checkNewUser(name);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        name = name.trim();
        userList.add(name);
        IntegerStringMap.put(V, name);
        StringIntegerMap.put(name, V);
        V += 1;
    }

    /**
     * Updates the edge values (financial transaction) between two users.
     *
     * @param transac the transaction object containing credit, debit, and value details.
     */
    public void updateEdge(Transaction transac) {
        int credit = transac.vCredit;
        int debit = transac.vDebit;
        validateVertex(credit);
        validateVertex(debit);
        if (credit == debit) {
            return; // paying for yourself doesn't change anyone's balance
        }

        // How much debit owes credit after this transaction. Negative means credit owes debit.
        long owed = matrix[debit][credit] - matrix[credit][debit] + transac.getCents();
        matrix[debit][credit] = Math.max(owed, 0);
        matrix[credit][debit] = Math.max(-owed, 0);
    }

    /**
     * Divides an amount into equal shares. When it doesn't divide evenly, the first shares get one
     * extra cent each, so the shares always add up to exactly the amount.
     * For example, 8000 cents split 3 ways is 2667, 2667 and 2666.
     *
     * @param cents the amount to divide, in cents.
     * @param ways  how many shares to divide it into.
     * @return the shares, in cents.
     * @throws IllegalArgumentException if ways is less than 1.
     */
    public static long[] splitEvenly(long cents, int ways) {
        if (ways < 1) {
            throw new IllegalArgumentException("Pick at least one person to split with.");
        }
        long[] shares = new long[ways];
        for (int i = 0; i < ways; i++) {
            shares[i] = cents / ways + (i < cents % ways ? 1 : 0);
        }
        return shares;
    }

    /**
     * Records one expense split evenly: {@code payer} paid {@code amount}, and everyone in
     * {@code splitWith} shares it equally. The payer can be in {@code splitWith}; their own share
     * doesn't change any balance. Each other person becomes one transaction for their share.
     *
     * @param payer       the person who paid.
     * @param splitWith   the people sharing the expense, in the order extra cents are given out.
     * @param amount      the total amount, in dollars.
     * @param description what the expense was for, e.g. "dinner".
     * @return the transactions that were recorded, one per person other than the payer.
     * @throws IllegalArgumentException if nobody is picked, someone is picked twice, or the amount
     *                                  isn't greater than 0. Nothing is recorded in that case.
     */
    public List<Transaction> addEvenSplit(int payer, List<Integer> splitWith, double amount, String description) {
        // Check everything first, so a bad input can't leave the expense half-recorded
        validateVertex(payer);
        if (new HashSet<>(splitWith).size() != splitWith.size()) {
            throw new IllegalArgumentException("Each person can only be picked once.");
        }
        for (int person : splitWith) {
            validateVertex(person);
        }
        long cents = Math.round(amount * 100);
        if (cents <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        long[] shares = splitEvenly(cents, splitWith.size());

        List<Transaction> recorded = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) {
            int person = splitWith.get(i);
            if (person != payer && shares[i] > 0) {
                Transaction transac = new Transaction(payer, person, shares[i] / 100.0, description);
                updateEdge(transac);
                recorded.add(transac);
            }
        }
        return recorded;
    }

    /**
     * Calculates the net balance for a specific user in cents: what everyone else owes them minus
     * what they owe everyone else.
     *
     * @param vNum the vertex number representing the user.
     * @return the net balance of the user, in cents.
     */
    public long getNetCents(int vNum) {
        long net = 0;
        for (int i = 0; i < V; i++) {
            net += matrix[i][vNum] - matrix[vNum][i];
        }
        return net;
    }

    /**
     * Calculates the net balance for a specific user (vertex): what everyone else owes them minus
     * what they owe everyone else.
     *
     * Positive means the group owes this person money. Negative means this person owes the group
     * money. Zero means they are settled up.
     *
     * @param vNum the vertex number representing the user.
     * @return the net balance of the user, in dollars.
     */
    public double getNetAmt(int vNum) {
        return getNetCents(vNum) / 100.0;
    }

    /**
     * Describes a user's net balance in words, so nobody has to interpret a negative sign.
     * For example "Harold is owed $30.00", "You owe $15.00" or "Isauro is settled up".
     *
     * @param vNum the vertex number representing the user.
     * @return a sentence describing the user's balance.
     */
    public String describeBalance(int vNum) {
        long net = getNetCents(vNum);
        boolean isMe = vNum == 0;
        String name = displayName(vNum);

        if (net > 0) {
            return name + (isMe ? " are owed " : " is owed ") + formatCents(net);
        } else if (net < 0) {
            return name + (isMe ? " owe " : " owes ") + formatCents(-net);
        } else {
            return name + (isMe ? " are settled up" : " is settled up");
        }
    }

    /**
     * Describes every user's net balance, one per line.
     *
     * @return the balance sentences for all users.
     */
    public String balanceSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < V; i++) {
            sb.append(describeBalance(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Finds the fewest payments that settle everyone's balance.
     *
     * A group of k people whose balances add up to zero can always be settled with k - 1 payments.
     * So the fewest payments is (people who owe or are owed) minus (the most zero-sum groups those
     * people can be split into). With at most 10 people every split can be checked quickly, so this
     * finds the true minimum. Each group is then settled by repeatedly matching the person owed the
     * most with the person who owes the most.
     *
     * @return the payments, in the order they should be listed. Empty if everyone is settled up.
     */
    public List<Payment> settleUp() {
        // People with a nonzero balance, and their balances (> 0: should receive, < 0: should pay)
        List<Integer> people = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            if (getNetCents(i) != 0) {
                people.add(i);
            }
        }
        int n = people.size();
        long[] balance = new long[n];
        for (int i = 0; i < n; i++) {
            balance[i] = getNetCents(people.get(i));
        }

        // sum[mask] = total balance of the people in mask.
        // groups[mask] = most zero-sum groups the people in mask can be split into (people left over
        // in a set that doesn't add up to zero don't count as a group).
        int full = (1 << n) - 1;
        long[] sum = new long[full + 1];
        int[] groups = new int[full + 1];
        for (int mask = 1; mask <= full; mask++) {
            int lowest = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + balance[lowest];
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    groups[mask] = Math.max(groups[mask], groups[mask ^ (1 << i)]);
                }
            }
            if (sum[mask] == 0) {
                groups[mask]++;
            }
        }

        // Walk back from everyone to nobody, removing one person at a time without losing a group.
        // Read in reverse, the removed people form an order where each zero-sum group is one block.
        int[] order = new int[n];
        int mask = full;
        for (int k = n - 1; k >= 0; k--) {
            int target = groups[mask] - (sum[mask] == 0 ? 1 : 0);
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0 && groups[mask ^ (1 << i)] == target) {
                    order[k] = i;
                    mask ^= 1 << i;
                    break;
                }
            }
        }

        // Cut the order wherever the running total returns to zero, then settle each group
        List<List<Integer>> zeroSumGroups = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        long running = 0;
        for (int i : order) {
            current.add(i);
            running += balance[i];
            if (running == 0) {
                Collections.sort(current);
                zeroSumGroups.add(current);
                current = new ArrayList<>();
            }
        }
        zeroSumGroups.sort(Comparator.comparing(group -> group.get(0)));

        List<Payment> payments = new ArrayList<>();
        for (List<Integer> group : zeroSumGroups) {
            settleGroup(group, balance, people, payments);
        }
        return payments;
    }

    /**
     * Settles one zero-sum group by repeatedly having the person who owes the most pay the person
     * who is owed the most. Ties go to whoever joined the group first.
     */
    private void settleGroup(List<Integer> group, long[] balance, List<Integer> people, List<Payment> payments) {
        while (true) {
            int receiver = -1, payer = -1;
            for (int i : group) {
                if (balance[i] > 0 && (receiver == -1 || balance[i] > balance[receiver])) {
                    receiver = i;
                }
                if (balance[i] < 0 && (payer == -1 || balance[i] < balance[payer])) {
                    payer = i;
                }
            }
            if (receiver == -1 || payer == -1) {
                return;
            }
            long amount = Math.min(balance[receiver], -balance[payer]);
            balance[receiver] -= amount;
            balance[payer] += amount;
            payments.add(new Payment(people.get(payer), people.get(receiver), amount));
        }
    }

    /**
     * Describes one payment in words, e.g. "Vin should pay Harold $10.00". The user "Me" is
     * written as "You"/"you".
     *
     * @param payment the payment to describe.
     * @return the sentence.
     */
    public String describePayment(Payment payment) {
        String amount = formatCents(payment.cents());
        if (payment.payer() == 0) {
            return "You should pay " + displayName(payment.receiver()) + " " + amount;
        } else if (payment.receiver() == 0) {
            return displayName(payment.payer()) + " should pay you " + amount;
        }
        return displayName(payment.payer()) + " should pay " + displayName(payment.receiver()) + " " + amount;
    }

    /**
     * Minimizes cash flow between users by calculating the fewest payments that settle all
     * balances. Each payment is one line, e.g. "Vin should pay Harold $10.00".
     *
     * @return a string with one repayment per line, or an empty string if everyone is settled up.
     */
    public String cashFlowMin() {
        StringBuilder sb = new StringBuilder();
        for (Payment payment : settleUp()) {
            sb.append(describePayment(payment)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Formats an amount as dollars and cents, e.g. 15 becomes "$15.00".
     *
     * @param amount the amount to format.
     * @return the formatted amount.
     */
    public static String formatMoney(double amount) {
        return formatCents(Math.round(amount * 100));
    }

    /**
     * Formats an amount of cents as dollars and cents, e.g. 1500 becomes "$15.00".
     *
     * @param cents the amount to format, in cents.
     * @return the formatted amount.
     */
    public static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        cents = Math.abs(cents);
        return String.format("%s$%d.%02d", sign, cents / 100, cents % 100);
    }
}
