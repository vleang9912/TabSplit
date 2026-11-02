import static org.junit.jupiter.api.Assertions.assertEquals;

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
        System.out.println(userGraph.getUserList().toString());
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
    public void AddUserInTwoMapsTest(){
        userGraph.addUser("Bret");
        assertEquals(userGraph.IntegerStringMap.get(userGraph.V()-1), "Bret");
        assertEquals(userGraph.StringIntegerMap.get("Bret"), 4);
    }


}
