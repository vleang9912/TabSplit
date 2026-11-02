

public class Transaction {
	// vCredit paid up front, so they are owed money back. vDebit was paid for, so they owe vCredit.
	public int vCredit, vDebit;
    public double val;
    public String description;


    // every box filled in the UI is one transaction object
    // the way this transaction class works is that every box filled for one thing (ie receipt) equals one transaction

    /**
     * Constructor for transaction object
     * @param vCredit person who paid (is owed money back)
     * @param vDebit person who was paid for (owes vCredit money)
     * @param val value of transaction
     * @param description description of transaction ie pizza
     */
    public Transaction (int vCredit, int vDebit, double val, String description){
        if (val >= 0){
            this.vCredit = vCredit;
            this.vDebit = vDebit;
            this.val = val;
            this.description = description;
        }
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
    public int VDebit() {
        return vDebit;
    }

    /**
     * Getter for the value of the transaction
     * @return int value
     */
    public double getVal() {
        return val;
    }

    /**
     * Getter for transaction description
     * @return String description
     */
    public String getDescription() {
        return description;
    }
}

