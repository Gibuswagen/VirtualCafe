package helpers;

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

    private static final int teaBrewTime = 2000; //30 seconds to brew tea

    private static final int coffeeBrewTime = 4000; //45 seconds to brew coffee


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

    //Start threads for orders in queue
    private void processOrders()
    {
        new Thread(()->{
            try
            {
                while(true)
                {
                    //Wait and take next order in the queue
                    Order order = orderQueue.take();
                    processOrderDrinks(order);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void processOrderDrinks(Order order) {
        while (order.countTeasByState("WAITING") > 0 || order.countCoffeesByState("WAITING") > 0) {
            if (!activeOrders.containsValue(order)) {
                // Stop processing if the order has been canceled
                break;
            }

            // Process teas
            if (brewingTeas.get() < 2 && order.countTeasByState("WAITING") > 0) {
                String teaID;
                synchronized (order) {
                    teaID = order.getNextWaitingTea();
                    if (teaID != null) {

                        order.updateTeaState(teaID, "BREWING");
                        cafeLogState();
                    }
                }
                if (teaID != null) {
                    startBrewingDrink(order, teaID, "Tea", teaBrewTime);
                    brewingTeas.incrementAndGet();
                    waitingTeas.decrementAndGet();
                }
            }

            // Process coffees
            if (brewingCoffees.get() < 2 && order.countCoffeesByState("WAITING") > 0) {
                String coffeeID;
                synchronized (order) {
                    coffeeID = order.getNextWaitingCoffee();
                    if (coffeeID != null) {
                        order.updateCoffeeState(coffeeID, "BREWING");
                        cafeLogState();
                    }
                }
                if (coffeeID != null) {
                    startBrewingDrink(order, coffeeID, "Coffee", coffeeBrewTime);
                    brewingCoffees.incrementAndGet();
                    waitingCoffees.decrementAndGet();
                }
            }

            // Add a short pause to avoid busy-waiting
            try {
                Thread.sleep(100); // Small delay to allow threads to progress
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startBrewingDrink(Order order, String drinkID, String drinkType, int brewTime) {
        brewingPool.submit(() -> {
            try {
                // Simulate brewing
                Thread.sleep(brewTime);

                // Update drink state to TRAY
                synchronized (order) {
                    if ("Tea".equals(drinkType)) {
                        order.updateTeaState(drinkID, "TRAY");
                        trayTeas.incrementAndGet();
                        brewingTeas.decrementAndGet();
                    } else if ("Coffee".equals(drinkType)) {
                        order.updateCoffeeState(drinkID, "TRAY");
                        trayCoffees.incrementAndGet();
                        brewingCoffees.decrementAndGet();
                    }

                    order.markReady(); // Notify if the entire order is ready
                    cafeLogState();
                }

                synchronized (orderQueue) {
                    // Notify processOrders that a brewing slot is free
                    orderQueue.notify();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Handle thread interruption
            }
        });
    }

    public void cafeLogState()
    {
        StringBuilder log = new StringBuilder();
        log.append("--- Cafe Log ---\n");

        //Number of clients
        log.append("Number of clients in the cafe: ").append(customers.size()).append("\n");

        //Number of clients waiting
        long clientsWaiting = customers.values().stream()
                        .filter("WAITING"::equals)
                                .count();
        log.append("Number of clients waiting for orders: ").append(clientsWaiting).append("\n");

        // Number and type of items in each area
        log.append("Items in waiting area: ").append(waitingTeas.get()).append(" tea(s), ").append(waitingCoffees.get()).append(" coffee(s)\n");
        log.append("Items in brewing area: ").append(brewingTeas.get()).append(" tea(s), ").append(brewingCoffees.get()).append(" coffee(s)\n");
        log.append("Items in tray area: ").append(trayTeas.get()).append(" tea(s), ").append(trayCoffees.get()).append(" coffee(s)\n");

        System.out.println(log);


    }

    //Shutdown brewing threads when cafe is closes
    public void shutdownCafe() {
        brewingPool.shutdown();
    }

    //Get order from active orders
    public Order getActiveOrder(int clientID)
    {
        return activeOrders.get(clientID);
    }

    //Attempt collection
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

    public void cancelOrder(String clientID)
    {

        int ID = Integer.parseInt(clientID);
        Order cancelledOrder;

        //Cancel and repurpose brewing or tray drinks
        synchronized (activeOrders)
        {
            cancelledOrder = activeOrders.remove(ID);
        }

        if(cancelledOrder != null)
        {
            System.out.println("Cancelling order for "+cancelledOrder.getCustomerName());

            //Remove waiting drinks
            int waitingTeasCount = cancelledOrder.countTeasByState("WAITING");
            int waitingCoffeesCount = cancelledOrder.countCoffeesByState("WAITING");

            waitingTeas.addAndGet(-waitingTeasCount);
            waitingCoffees.addAndGet(-waitingCoffeesCount);

            System.out.println("Removed " + waitingTeasCount + " teas and " + waitingCoffeesCount + " coffees from waiting area for " + cancelledOrder.getCustomerName());

            // Handle drinks in the BREWING and TRAY areas
            repurposeBrewingAndTrayDrinks(cancelledOrder);
        }
    }
    private void repurposeBrewingAndTrayDrinks(Order cancelledOrder) {
        // Repurpose teas
        cancelledOrder.getTotalTeas().entrySet().removeIf(entry -> {
            String teaID = entry.getKey();
            String state = entry.getValue();
            if ("BREWING".equals(state) || "TRAY".equals(state)) {
                boolean isBrewing = "BREWING".equals(state);
                boolean transferred = transferDrink(cancelledOrder, "Tea", isBrewing);

                if (!transferred) {
                    // If not repurposed, discard the drink
                    if (isBrewing) {
                        brewingTeas.decrementAndGet();
                    } else {
                        trayTeas.decrementAndGet();
                    }
                    System.out.println(teaID + " from " + cancelledOrder.getCustomerName() + " discarded.");
                }
                return true; // Remove the tea from cancelledOrder's map
            }
            return false; // Keep WAITING teas, if any
        });

        // Repurpose coffees
        cancelledOrder.getTotalCoffees().entrySet().removeIf(entry -> {
            String coffeeID = entry.getKey();
            String state = entry.getValue();
            if ("BREWING".equals(state) || "TRAY".equals(state)) {
                boolean isBrewing = "BREWING".equals(state);
                boolean transferred = transferDrink(cancelledOrder, "Coffee", isBrewing);

                if (!transferred) {
                    // If not repurposed, discard the drink
                    if (isBrewing) {
                        brewingCoffees.decrementAndGet();
                    } else {
                        trayCoffees.decrementAndGet();
                    }
                    System.out.println(coffeeID + " from " + cancelledOrder.getCustomerName() + " discarded.");
                }
                return true; // Remove the coffee from cancelledOrder's map
            }
            return false; // Keep WAITING coffees, if any
        });
    }

    private boolean transferDrink(Order canceledOrder, String drinkType, boolean isBrewing) {
        for (Order order : activeOrders.values()) {
            if (!order.equals(canceledOrder)) { // Skip the canceled order
                synchronized (order) {
                    // Check if there are drinks in the waiting area to repurpose
                    if ("Tea".equals(drinkType) && order.countTeasByState("WAITING") > 0) {
                        String newTeaID = order.getNextWaitingTea();
                        if (newTeaID != null) {
                            order.updateTeaState(newTeaID, isBrewing ? "BREWING" : "TRAY");

                            // Update atomic counters
                            if (isBrewing) {
                                brewingTeas.incrementAndGet();
                            } else {
                                trayTeas.incrementAndGet();
                            }

                            transferLog(drinkType, canceledOrder, order, isBrewing);
                            return true; // Transfer successful
                        }
                    } else if ("Coffee".equals(drinkType) && order.countCoffeesByState("WAITING") > 0) {
                        String newCoffeeID = order.getNextWaitingCoffee();
                        if (newCoffeeID != null) {
                            order.updateCoffeeState(newCoffeeID, isBrewing ? "BREWING" : "TRAY");

                            // Update atomic counters
                            if (isBrewing) {
                                brewingCoffees.incrementAndGet();
                            } else {
                                trayCoffees.incrementAndGet();
                            }

                            transferLog(drinkType, canceledOrder, order, isBrewing);
                            return true; // Transfer successful
                        }
                    }
                }
            }
        }
        return false; // Transfer not successful
    }
    private void transferLog(String drinkType, Order cancelledOrder, Order recipientOrder, boolean isBrewing) {
        String sourceCustomer = cancelledOrder.getCustomerName();
        String targetCustomer = recipientOrder.getCustomerName();
        String location = isBrewing ? "currently brewing" : "in the tray";
        System.out.println(drinkType + " " + location + " for " + sourceCustomer + " has been transferred to " + targetCustomer + "'s order.");
    }

}

