import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * One expense: payer paid the total, and splitWith.get(i) owes shares[i] of it, ie dinner
 * @param payer person who paid
 * @param splitWith people sharing it (can include the payer)
 * @param shares each person's share in cents, same order as splitWith
 * @param description what it was for, can be empty
 * @param date the day it happened
 */
public record Expense(int payer, List<Integer> splitWith, long[] shares, String description, LocalDate date) {

    public Expense {
        // copy so changing the caller's list or array later can't change a recorded expense
        splitWith = List.copyOf(splitWith);
        shares = shares.clone();
        description = description == null ? "" : description;
        Objects.requireNonNull(date, "date");
    }

    /**
     * Getter for the shares, a copy so nobody can edit a recorded expense through it
     * @return each person's share in cents
     */
    @Override
    public long[] shares() {
        return shares.clone();
    }

    /**
     * Getter for the total paid
     * @return total in cents
     */
    public long totalCents() {
        long total = 0;
        for (long share : shares) {
            total += share;
        }
        return total;
    }
}
