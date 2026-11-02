/**
 * One expense: {@code vCredit} paid {@code val} dollars on behalf of {@code vDebit}.
 * Every expense entered in the UI becomes one transaction (e.g. one receipt).
 */
public class Transaction {
    // vCredit paid up front, so they are owed money back. vDebit was paid for, so they owe vCredit.
    public int vCredit, vDebit;
    public double val;
    public String description;

    /**
     * Constructor for transaction object
     * @param vCredit person who paid (is owed money back)
     * @param vDebit person who was paid for (owes vCredit money)
     * @param val value of transaction, in dollars
     * @param description description of transaction ie pizza
     * @throws IllegalArgumentException if the value is negative
     */
    public Transaction(int vCredit, int vDebit, double val, String description) {
        if (val < 0) {
            throw new IllegalArgumentException("Amount can't be negative.");
        }
        this.vCredit = vCredit;
        this.vDebit = vDebit;
        this.val = val;
        this.description = description == null ? "" : description;
    }

    /**
     * Getter for the vertex number of the person lending the money
     * @return int creditor vertex number
     */
    public int getVCredit() {
        return vCredit;
    }

    /**
     * Getter for the vertex number of the person owing the money
     * @return int debitor vertex number
     */
    public int getVDebit() {
        return vDebit;
    }

    /**
     * Getter for the value of the transaction
     * @return double value, in dollars
     */
    public double getVal() {
        return val;
    }

    /**
     * Getter for the value of the transaction, rounded to the nearest cent
     * @return long value, in cents
     */
    public long getCents() {
        return Math.round(val * 100);
    }

    /**
     * Getter for transaction description
     * @return String description, or an empty string if there is none
     */
    public String getDescription() {
        return description;
    }
}
