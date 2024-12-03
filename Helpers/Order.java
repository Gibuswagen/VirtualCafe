package Helpers;

import java.util.HashMap;
import java.util.Map;

public class Order
{
    private final int clientID;
    private final String customerName;

    private final Map<String, String> teas; // Map of <TeaID> <State>

    private final Map<String, String> coffees; // Map of <CoffeeID> <State>

    private int totalTeas;
    private int totalCoffees;


    public Order(int clientID, String customerName, int teaCount, int coffeeCount)
    {
        this.clientID = clientID;
        this.customerName = customerName;
        this.teas = new HashMap<>();
        this.coffees = new HashMap<>();
        this.totalTeas = teaCount;
        this.totalCoffees = coffeeCount;

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
    public int getClientID()
    {
        return clientID;
    }
    public String getCustomerName()
    {
        return customerName;
    }
    public Map<String, String> getTeas()
    {
        return teas;
    }
    public Map<String, String> getCoffees()
    {
        return coffees;
    }

    //Methods to add extra to an order
    public void AddOnTea(int addNum)
    {
        for (int i = totalTeas; i < totalTeas+addNum; i++)
        {
            teas.put("Tea"+i,"WAITING");
        }
        totalTeas+=addNum;
    }
    public void AddOnCoffee(int addNum)
    {
        for (int i = totalCoffees; i < totalCoffees+addNum; i++)
        {
            coffees.put("Tea"+i,"WAITING");
        }
        totalCoffees+=addNum;
    }


    // Update state for a specific tea
    public void updateTeaState(String teaID, String newState)
    {
        if (teas.containsKey(teaID))
        {
            teas.put(teaID, newState);
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
}
