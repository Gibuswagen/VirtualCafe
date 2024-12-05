package Helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Cafe
{
    private final ExecutorService brewingPool = Executors.newFixedThreadPool(4); // Max 4 concurrent brewing tasks
    private final HashMap<String,String> customers; // HashMap to keep track of customers and their state (IDLE, WAITING)
    private final Map<Integer,Order> activeOrders = new ConcurrentHashMap<>(); // <clientID, Order>

    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>(); // Queue for appropriate serving approach

    private static final int teaBrewTime = 30000; //30 seconds to brew tea

    private static final int coffeeBrewTime = 45000; //45 seconds to brew coffee


    //AtomicInteger for logState methods and keep track of brewing slots
    private static final AtomicInteger waitingTeas = new AtomicInteger(0);
    private static final AtomicInteger waitingCoffees = new AtomicInteger(0);
    private static final AtomicInteger brewingTeas = new AtomicInteger(0); //Cant be more than 2
    private static final AtomicInteger brewingCoffees = new AtomicInteger(0); //Cant be more than 2
    private static final AtomicInteger trayTeas = new AtomicInteger(0);
    private static final AtomicInteger trayCoffees = new AtomicInteger(0);

    public Cafe(HashMap<String,String> customers)
    {
        this.customers = customers;
    }

    public void addOrder(String clientID, String customerName, int teas, int coffees) {
        int clientIdInt = Integer.parseInt(clientID);

        synchronized (activeOrders) {
            Order order = activeOrders.get(clientIdInt);

            if (order != null) {
                // Merge new items into the existing order
                order.AddOnTea(teas);
                order.AddOnCoffee(coffees);
                waitingTeas.addAndGet(teas);
                waitingCoffees.addAndGet(coffees);

                System.out.println("Extra ordered by " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");


            } else {
                // Create a new order
                order = new Order(clientIdInt, customerName, teas, coffees);
                activeOrders.put(clientIdInt, order);
                waitingTeas.addAndGet(teas);
                waitingCoffees.addAndGet(coffees);

                System.out.println("New order place by " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");
            }

            orderQueue.add(order); // Reference the same Order instance
        }
        processOrders();
        cafeLogState();

    }

    private void processOrders()
    {
        new Thread(()->{
            try
            {
                while(!orderQueue.isEmpty())
                {
                    Order order = orderQueue.take();
                    processOrderDrinks(order);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void processOrderDrinks(Order order)
    {
        //Process teas
        while(brewingTeas.get() < 2 && order.countTeasByState("WAITING") > 0)
        {
            String teaID;
            synchronized (order)
            {
                teaID = order.getNextWaitingTea();
                if(teaID != null)
                {
                    brewingTeas.incrementAndGet();
                    order.updateTeaState(teaID,"BREWING");
                }
            }
            if(teaID != null)
            {
                startBrewingDrink(order, teaID, "Tea", teaBrewTime); // Brew the tea
                waitingTeas.decrementAndGet();
            }
        }
        //Process coffees
        while(brewingCoffees.get() < 2 && order.countCoffeesByState("WAITING") > 0)
        {
            String coffeeID;
            synchronized (order)
            {
                coffeeID = order.getNextWaitingCoffee();
                if(coffeeID != null)
                {
                    brewingCoffees.incrementAndGet();
                    order.updateCoffeeState(coffeeID,"BREWING");
                }
            }
            if(coffeeID != null)
            {
                startBrewingDrink(order, coffeeID, "Coffee", coffeeBrewTime); // Brew the coffee
                waitingCoffees.decrementAndGet();
            }
        }
    }

    private void startBrewingDrink(Order order, String drinkID, String drinkType, int brewTime)
    {
        brewingPool.submit(() ->{
            try
            {
                // "Brew" a drink
                Thread.sleep(brewTime);

                // Update drink state to TRAY
                synchronized (order)
                {
                    if("Tea".equals(drinkType))
                    {
                        order.updateTeaState(drinkID,"TRAY");
                        brewingTeas.decrementAndGet();
                    }
                    else if ("Coffee".equals(drinkType))
                    {
                        order.updateCoffeeState(drinkID,"TRAY");
                        brewingCoffees.decrementAndGet();
                    }
                }
                cafeLogState();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

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
        log.append("Items in tray area: ").append(trayTeas.get()).append(" tea(s), ").append(trayCoffees.get()).append(" coffee(s)\n");

        System.out.println(log);


    }

    public void shutdownCafe() {
        brewingPool.shutdown();
    }

    public Order getActiveOrder(int clientID)
    {
        return activeOrders.get(clientID);
    }

    public boolean isCollectable(String clientID)
    {
        int ID = Integer.parseInt(clientID);
        Order order = activeOrders.get(ID);

        if (order != null && order.isReady())
        {
            //Remove order from activeOrders
            activeOrders.remove(ID);

            //Update drink counts in Tray area
            trayTeas.addAndGet(-order.countTeasByState("TRAY"));
            trayCoffees.addAndGet(-order.countCoffeesByState("TRAY"));

            cafeLogState();
            return true;
        }
        return false;
    }
}

