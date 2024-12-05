package helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


// Represents a customer's order.
// Tracks the state of each drink (e.g., "WAITING", "BREWING", "TRAY") and provides methods to update or query these states.

public class Order
{
    private final Object lock = new Object(); //Object lock
    private final String customerName;

    // Drinks are in the hashmap to track and change their states
    private final Map<String, String> teas; // Map of <id> <state>
    private final Map<String, String> coffees; // Map of <id> <state>

    //Set of atomicintegers to keep track of total number of drinks and total drinks that a ready (for various methods to handle checks and changes)
    private final AtomicInteger totalTeas = new AtomicInteger(0);
    private final AtomicInteger totalCoffees = new AtomicInteger(0);
    private final AtomicInteger readyCount = new AtomicInteger(0);

    public Order(String customerName, int teaCount, int coffeeCount)
    {
        this.customerName = customerName;
        this.teas = new HashMap<>();
        this.coffees = new HashMap<>();
        this.totalTeas.addAndGet(teaCount);
        this.totalCoffees.addAndGet(coffeeCount);

        //Initialize all drinks as WAITING
        for (int i = 0; i < teaCount; i++)
        {
            teas.put("Tea"+i,"WAITING");
        }

        for (int i = 0; i < coffeeCount; i++)
        {
            coffees.put("Coffee"+i,"WAITING");
        }

    }

    //Getters
    public Map<String,String> getTotalTeas()
    {
        return teas;
    }
    public Map<String,String> getTotalCoffees()
    {
        return coffees;
    }
    public String getCustomerName()
    {
        return customerName;
    }


    //When order is complete, notify threads waiting on the lock to proceed (waiting for order to complete)
    public synchronized void markReady()
    {
        if(isReady())
        {
            synchronized (lock)
            {
                lock.notifyAll();
            }
        }
    }

    //Access to the internal lock object
    public Object getLock()
    {
        return lock;
    }


    //Add extra drinks to the order
    public void AddOnTea(int addNum)
    {
        for (int i = totalTeas.get(); i < totalTeas.get()+addNum; i++)
        {
            teas.put("Tea"+i,"WAITING");
        }
        totalTeas.addAndGet(addNum);
    }
    public void AddOnCoffee(int addNum)
    {
        for (int i = totalCoffees.get(); i < totalCoffees.get()+addNum; i++)
        {
            coffees.put("Coffee"+i,"WAITING");
        }
        totalCoffees.addAndGet(addNum);
    }


    // Update state for a specific tea
    public void updateTeaState(String teaID, String newState)
    {
        if (teas.containsKey(teaID))
        {
            teas.put(teaID, newState);
            if(newState.equals("TRAY")){readyCount.incrementAndGet();}
        } else {
            throw new IllegalArgumentException("TeaID " + teaID + " not found.");
        }
    }
    // Update state for a specific coffee
    public void updateCoffeeState(String coffeeID, String newState)
    {
        if (coffees.containsKey(coffeeID))
        {
            coffees.put(coffeeID, newState);
            if(newState.equals("TRAY")){readyCount.incrementAndGet();}
        } else {
            throw new IllegalArgumentException("CoffeeID " + coffeeID + " not found.");
        }
    }

    // Count teas in a specific state
    public int countTeasByState(String state)
    {
        return (int) teas.values().stream().filter(s -> s.equals(state)).count();
    }

    // Count coffees in a specific state
    public int countCoffeesByState(String state)
    {
        return (int) coffees.values().stream().filter(s -> s.equals(state)).count();
    }

    // Retrieve waiting drinks
    public String getNextWaitingTea() {
        return teas.entrySet().stream()
                .filter(entry -> "WAITING".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public String getNextWaitingCoffee() {
        return coffees.entrySet().stream()
                .filter(entry -> "WAITING".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    //Get the status of the drinks
    public String getOrderStatus()
    {
        StringBuilder status = new StringBuilder();
        status.append("Order status for ").append(customerName).append(":\n");

        // Counters for teas
        int waitingTeas = 0, brewingTeas = 0, trayTeas = 0;

        // Iterate through the teas map once
        for (String state : teas.values()) {
            switch (state) {
                case "WAITING": waitingTeas++; break;
                case "BREWING": brewingTeas++; break;
                case "TRAY": trayTeas++; break;
            }
        }

        // Counters for coffees
        int waitingCoffees = 0, brewingCoffees = 0, trayCoffees = 0;

        // Iterate through the coffees map once
        for (String state : coffees.values()) {
            switch (state) {
                case "WAITING": waitingCoffees++; break;
                case "BREWING": brewingCoffees++; break;
                case "TRAY": trayCoffees++; break;
            }
        }

        // Append information about each state to the status
        if (waitingTeas > 0 || waitingCoffees > 0) {
            status.append("- ").append(waitingTeas).append(" tea(s) and ")
                    .append(waitingCoffees).append(" coffee(s) in waiting area\n");
        }

        if (brewingTeas > 0 || brewingCoffees > 0) {
            status.append("- ").append(brewingTeas).append(" tea(s) and ")
                    .append(brewingCoffees).append(" coffee(s) currently brewing\n");
        }

        if (trayTeas > 0 || trayCoffees > 0) {
            status.append("- ").append(trayTeas).append(" tea(s) and ")
                    .append(trayCoffees).append(" coffee(s) ready on the tray\n");
        }

        // Case where there are no drinks in the order
        if (status.toString().equals("Order status for " + customerName + ":\n")) {
            status.append("- No items found in the order\n");
        }

        return status.toString().trim();
    }

    //Check if the order is fulfilled
    public boolean isReady()
    {
        return readyCount.get() == totalTeas.get() + totalCoffees.get();
    }

}
