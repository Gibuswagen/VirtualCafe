package Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Cafe
{
    private final HashMap<String,String> customers;
    private final Map<Integer,Order> activeOrders = new ConcurrentHashMap<>();


    //AtomicInteger to for logState methods (To optimize complexity)
    private static final AtomicInteger waitingTeas = new AtomicInteger(0);
    private static final AtomicInteger waitingCoffees = new AtomicInteger(0);
    private static final AtomicInteger brewingTeas = new AtomicInteger(0); //Cant be more than 2
    private static final AtomicInteger brewingCoffees = new AtomicInteger(0); //Cant be more than 2
    private static final AtomicInteger readyTeas = new AtomicInteger(0);
    private static final AtomicInteger readyCoffees = new AtomicInteger(0);

    public Cafe(HashMap<String,String> customers)
    {
        this.customers = customers;
    }

    public void addOrder(String clientID, String customerName, int teas, int coffees)
    {
        {
            activeOrders.compute(Integer.parseInt(clientID), (id, existingOrder) -> {
                if (existingOrder != null) {

                    // Add drinks to the existing order
                    existingOrder.AddOnTea(teas);
                    existingOrder.AddOnCoffee(coffees);
                    System.out.println("Updated order for " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");

                    //Update waiting area drink count
                    waitingTeas.addAndGet(teas);
                    waitingCoffees.addAndGet(coffees);

                    //Display change
                    cafeLogState();
                    return existingOrder;
                } else {
                    // Create a new order
                    Order newOrder = new Order(Integer.parseInt(clientID), customerName, teas, coffees);
                    System.out.println("New order added for " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");

                    //Update waiting area drink count
                    waitingTeas.addAndGet(teas);
                    waitingCoffees.addAndGet(coffees);

                    cafeLogState();
                    return newOrder;
                }
            });
        }
    }

    //public void processOrders()



    private void cafeLogState()
    {
        StringBuilder log = new StringBuilder();
        log.append("--- Cafe Log ---\n");

        //Number of clients
        log.append("Number of clients in the cafe: ").append(customers.size()).append("\n");

        //Number of clients waiting
        long clientsWaiting = activeOrders.values().stream()
                        .filter(order -> order.countTeasByState("WAITING") > 0 || order.countCoffeesByState("WAITING") > 0)
                                .count();
        log.append("Number of clients waiting for orders: ").append(clientsWaiting).append("\n");

        // Number and type of items in each area
        log.append("Items in waiting area: ").append(waitingTeas.get()).append(" tea(s), ").append(waitingCoffees.get()).append(" coffee(s)\n");
        log.append("Items in brewing area: ").append(brewingTeas.get()).append(" tea(s), ").append(brewingCoffees.get()).append(" coffee(s)\n");
        log.append("Items in tray area: ").append(readyTeas.get()).append(" tea(s), ").append(readyCoffees.get()).append(" coffee(s)\n");

        System.out.println(log);


    }
}

