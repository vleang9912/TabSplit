import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TabSplitTest {
    
    private UserGraph userGraph;

    @BeforeEach
    void init(){
        userGraph = new UserGraph();

        userGraph.addUser("Isauro");
        userGraph.addUser("Harold");
        userGraph.addUser("Vin");
    }
    
    @Test
    public void testGraphSize(){
        assertEquals(userGraph.getLimit(), 10);
    }

    @Test
    public void testUserSize(){
        assertEquals(userGraph.V(), 4);
    }

    @Test
    public void testUsersinList(){
        List<String> testList = new ArrayList<>();
        testList.add("Me");
        testList.add("Isauro");
        testList.add("Harold");
        testList.add("Vin");
        assertEquals(userGraph.getUserList(), testList);
    }

    @Test
    public void testOneTransac(){
        Transaction transac1 = new Transaction(0, 1, 10, "I paid Isauro $10");
        userGraph.updateEdge(transac1);
        assertEquals(userGraph.getNetAmt(0), 10);
        assertEquals(userGraph.getNetAmt(1), -10);
        assertEquals(userGraph.IntegerStringMap.get(1), "Isauro");
    }

    @Test
    public void testDirectionSwitchAndNetAmt(){
        Transaction transac2 = new Transaction(2, 0, 15, "I owe Harold $15");
        userGraph.updateEdge(transac2);
        assertEquals(userGraph.getNetAmt(0), -15);
        assertEquals(userGraph.getNetAmt(2), 15);
        Transaction transac3 = new Transaction(0, 2, 25, "I paid Harold $25");
        userGraph.updateEdge(transac3);
        assertEquals(userGraph.getNetAmt(0), 10);
        assertEquals(userGraph.getNetAmt(2), -10);
        Transaction transac4 = new Transaction(2, 3, 10, "Harold paid Vin $10");
        userGraph.updateEdge(transac4);
        assertEquals(userGraph.getNetAmt(0), 10);
        assertEquals(userGraph.getNetAmt(2), 0);
        assertEquals(userGraph.getNetAmt(3), -10);
    }

    @Test
    public void payYourselfTest(){
        Transaction transac5 = new Transaction(0, 0, 10, "I'm paying myself");
        userGraph.updateEdge(transac5);
        assertEquals(userGraph.getNetAmt(0), 0);
    }

    @Test
    public void MapNameTestBothWays(){
        assertEquals(userGraph.IntegerStringMap.get(2), "Harold");
        assertEquals(userGraph.StringIntegerMap.get("Harold"), 2);
    }

    @Test
    public void cashFlowMinTest(){
        Transaction transac2 = new Transaction(2, 0, 15, "Harold paid me $15");
        userGraph.updateEdge(transac2);
        Transaction transac2_5 = new Transaction(2, 0, 5, "Harold paid me $5");
        userGraph.updateEdge(transac2_5);
        Transaction transac4 = new Transaction(2, 3, 10, "Harold paid Vin $10");
        userGraph.updateEdge(transac4);
        Transaction transac6 = new Transaction(0, 1, 5, "I paid Isauro $5");
        userGraph.updateEdge(transac6);

        assertEquals(userGraph.cashFlowMin(),
            "You should pay Harold $15.00\n"
            + "Vin should pay Harold $10.00\n"
            + "Isauro should pay Harold $5.00\n");
    }

    @Test
    public void cashFlowMinTest2AndMapsTest(){
        Transaction transac7 = new Transaction(userGraph.StringIntegerMap.get("Harold"), userGraph.StringIntegerMap.get("Vin"), 20, null);
        userGraph.updateEdge(transac7);
        assertEquals(userGraph.getNetAmt(userGraph.StringIntegerMap.get("Harold")), 20);
        assertEquals(userGraph.getNetAmt(userGraph.StringIntegerMap.get("Vin")), -20);
        assertEquals(userGraph.cashFlowMin(), "Vin should pay Harold $20.00" + "\n");
    }

    @Test
    public void someoneShouldPayYouTest(){
        Transaction transac8 = new Transaction(0, 3, 12.5, "I paid for Vin");
        userGraph.updateEdge(transac8);
        assertEquals(userGraph.cashFlowMin(), "Vin should pay you $12.50\n");
    }

    @Test
    public void describeBalanceTest(){
        userGraph.updateEdge(new Transaction(2, 0, 15, "Harold paid for me"));
        assertEquals(userGraph.describeBalance(0), "You owe $15.00");
        assertEquals(userGraph.describeBalance(2), "Harold is owed $15.00");
        assertEquals(userGraph.describeBalance(3), "Vin is settled up");
        userGraph.updateEdge(new Transaction(0, 2, 20, "I paid for Harold"));
        assertEquals(userGraph.describeBalance(0), "You are owed $5.00");
    }

    @Test
    public void floatingPointLeftoversAreNotPayments(){
        userGraph.updateEdge(new Transaction(0, 1, 0.1, "a"));
        userGraph.updateEdge(new Transaction(0, 1, 0.2, "b"));
        userGraph.updateEdge(new Transaction(1, 0, 0.3, "c"));
        assertEquals(userGraph.getNetAmt(0), 0);
        assertEquals(userGraph.cashFlowMin(), "");
    }

    @Test
    public void cashFlowMinFindsFewestPayments(){
        // Balances +6, +5, -5, -3, -3: the greedy match alone needs 4 payments, the minimum is 3
        userGraph.addUser("Bret");
        userGraph.updateEdge(new Transaction(0, 3, 3, null));
        userGraph.updateEdge(new Transaction(0, 4, 3, null));
        userGraph.updateEdge(new Transaction(1, 2, 5, null));
        assertEquals(userGraph.settleUp().size(), 3);
        assertEquals(userGraph.cashFlowMin(),
            "Vin should pay you $3.00\n"
            + "Bret should pay you $3.00\n"
            + "Harold should pay Isauro $5.00\n");
    }

    @Test
    public void addUserRejectsBadNames(){
        assertEquals(userGraph.checkNewUser("  "), "Enter a name first.");
        assertEquals(userGraph.checkNewUser("vin"), "Vin is already in the group.");
        assertEquals(userGraph.checkNewUser("me"), "Me is already in the group.");
        assertNull(userGraph.checkNewUser(" Bret "));
        assertThrows(IllegalArgumentException.class, () -> userGraph.addUser("Harold"));
    }

    @Test
    public void groupIsCappedAtLimit(){
        for (int i = userGraph.V(); i < userGraph.getLimit(); i++) {
            userGraph.addUser("Friend " + i);
        }
        assertEquals(userGraph.checkNewUser("One more"), "The group is full (10 people max).");
        assertThrows(IllegalArgumentException.class, () -> userGraph.addUser("One more"));
    }

    @Test
    public void negativeTransactionIsRejected(){
        assertThrows(IllegalArgumentException.class, () -> new Transaction(0, 1, -5, "refund"));
    }

    @Test
    public void formatsCents(){
        assertEquals(UserGraph.formatCents(5), "$0.05");
        assertEquals(UserGraph.formatMoney(1234.5), "$1234.50");
    }

    @Test
    public void splitEvenlyGivesExtraCentsToFirstShares(){
        assertArrayEquals(UserGraph.splitEvenly(8000, 4), new long[] {2000, 2000, 2000, 2000});
        assertArrayEquals(UserGraph.splitEvenly(8000, 3), new long[] {2667, 2667, 2666});
        assertArrayEquals(UserGraph.splitEvenly(2, 3), new long[] {1, 1, 0});
        assertThrows(IllegalArgumentException.class, () -> UserGraph.splitEvenly(100, 0));
    }

    @Test
    public void evenSplitDinnerForFour(){
        // I paid $80 for dinner for all 4 of us: everyone else owes me $20
        userGraph.addEvenSplit(0, List.of(0, 1, 2, 3), 80, "dinner");
        assertEquals(userGraph.getNetAmt(0), 60);
        assertEquals(userGraph.getNetAmt(1), -20);
        assertEquals(userGraph.getNetAmt(2), -20);
        assertEquals(userGraph.getNetAmt(3), -20);
    }

    @Test
    public void evenSplitWithoutThePayer(){
        // Harold paid $10 for Isauro and Vin only
        userGraph.addEvenSplit(2, List.of(1, 3), 10, "snacks");
        assertEquals(userGraph.getExpenses().size(), 1); // one expense, not one per person
        assertEquals(userGraph.getExpenses().get(0).splitWith(), List.of(1, 3));
        assertEquals(userGraph.getNetAmt(2), 10);
        assertEquals(userGraph.getNetAmt(1), -5);
        assertEquals(userGraph.getNetAmt(3), -5);
        assertEquals(userGraph.getNetAmt(0), 0);
    }

    @Test
    public void evenSplitUnevenCentsStillAddUp(){
        // $10 three ways: $3.34, $3.33, $3.33. I'm listed first, so I keep the extra cent
        userGraph.addEvenSplit(0, List.of(0, 1, 2), 10, "coffee");
        assertEquals(userGraph.getNetAmt(0), 6.66);
        assertEquals(userGraph.getNetAmt(1), -3.33);
        assertEquals(userGraph.getNetAmt(2), -3.33);
    }

    @Test
    public void evenSplitRejectsBadInputWithoutRecordingAnything(){
        assertThrows(IllegalArgumentException.class, () -> userGraph.addEvenSplit(0, List.of(), 10, ""));
        assertThrows(IllegalArgumentException.class, () -> userGraph.addEvenSplit(0, List.of(1, 1), 10, ""));
        assertThrows(IllegalArgumentException.class, () -> userGraph.addEvenSplit(0, List.of(1, 2), 0, ""));
        assertThrows(IllegalArgumentException.class, () -> userGraph.addEvenSplit(0, List.of(1, 99), 10, ""));
        assertEquals(userGraph.cashFlowMin(), "");
    }

    @Test
    public void fillSharesSplitsWhatIsLeftEvenly(){
        // $80, Harold's share typed in as $32: the other two split the remaining $48
        assertArrayEquals(UserGraph.fillShares(8000, new Long[] {3200L, null, null}), new long[] {3200, 2400, 2400});
        // Nothing typed in: the same as an even split
        assertArrayEquals(UserGraph.fillShares(8000, new Long[] {null, null, null}), new long[] {2667, 2667, 2666});
        // Everything typed in: kept exactly as typed, even if it doesn't add up
        assertArrayEquals(UserGraph.fillShares(8000, new Long[] {5000L, 2000L}), new long[] {5000, 2000});
        // Typed-in shares already over the total: everyone else gets $0
        assertArrayEquals(UserGraph.fillShares(8000, new Long[] {9000L, null}), new long[] {9000, 0});
        assertThrows(IllegalArgumentException.class, () -> UserGraph.fillShares(8000, new Long[] {-100L, null}));
    }

    @Test
    public void addSplitWithUnevenShares(){
        // I paid $80: Harold's share is $32, Isauro's and mine are $24 each
        userGraph.addSplit(0, List.of(2, 1, 0), 8000, new long[] {3200, 2400, 2400}, "dinner");
        assertEquals(userGraph.getNetAmt(0), 56);
        assertEquals(userGraph.getNetAmt(2), -32);
        assertEquals(userGraph.getNetAmt(1), -24);
    }

    @Test
    public void addSplitRejectsSharesThatDontAddUp(){
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> userGraph.addSplit(0, List.of(1, 2), 8000, new long[] {5000, 2600}, "dinner"));
        assertEquals(e.getMessage(), "The shares add up to $76.00, not $80.00.");
        assertThrows(IllegalArgumentException.class,
            () -> userGraph.addSplit(0, List.of(1, 2), 8000, new long[] {9000, -1000}, "dinner"));
        assertEquals(userGraph.cashFlowMin(), "");
        assertEquals(userGraph.getExpenses().size(), 0);
    }

    @Test
    public void expensesAreKeptInOrder(){
        userGraph.addSplit(0, List.of(0, 1, 2), 9000, new long[] {3000, 3000, 3000}, "dinner");
        userGraph.updateEdge(new Transaction(2, 0, 15, "pizza"));
        List<Expense> expenses = userGraph.getExpenses();
        assertEquals(expenses.size(), 2);
        assertEquals(expenses.get(0).payer(), 0);
        assertEquals(expenses.get(0).totalCents(), 9000);
        assertEquals(expenses.get(0).description(), "dinner");
        assertEquals(expenses.get(1).splitWith(), List.of(0));
        assertEquals(expenses.get(1).totalCents(), 1500);
        // balances add up from the list: me +90 -30 -15, Harold -30 +15
        assertEquals(userGraph.getNetAmt(0), 45);
        assertEquals(userGraph.getNetAmt(2), -15);
    }

    @Test
    public void recordedExpensesCantBeChangedFromOutside(){
        long[] shares = {5000, 3000};
        userGraph.addSplit(0, List.of(1, 2), 8000, shares, "dinner");
        shares[0] = 999999; // changing the caller's array afterwards
        userGraph.getExpenses().get(0).shares()[0] = 999999; // or the copy handed back
        assertEquals(userGraph.getNetAmt(1), -50);
        assertThrows(UnsupportedOperationException.class, () -> userGraph.getExpenses().clear());
    }

    @Test
    public void AddUserInTwoMapsTest(){
        userGraph.addUser("Bret");
        assertEquals(userGraph.IntegerStringMap.get(userGraph.V()-1), "Bret");
        assertEquals(userGraph.StringIntegerMap.get("Bret"), 4);
    }


}
