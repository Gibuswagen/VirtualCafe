package helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


// The central class managing cafe operations.
// Handles drink preparation, order management, and logging system state.

public class Cafe
{
    private BufferedWriter logWriter;
    private final ExecutorService brewingPool = Executors.newFixedThreadPool(4); // Max 4 concurrent brewing tasks
    private final HashMap<String,String> customers; // HashMap to keep track of customers and their state (IDLE, WAITING)
    private final Map<Integer,Order> activeOrders = new ConcurrentHashMap<>(); // <clientID, Order>
    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>(); // Queue for appropriate serving approach
    private static final int teaBrewTime = 30000; //30 seconds to brew tea
    private static final int coffeeBrewTime = 45000; //45 seconds to brew coffee

    // These track the brewing slots to ensure no more than 2 teas or 2 coffees are brewed concurrently.
    private static final AtomicInteger brewingTeas = new AtomicInteger(0);
    private static final AtomicInteger brewingCoffees = new AtomicInteger(0);

    public Cafe(HashMap<String,String> customers)
    {
        this.customers = customers;
        // Initialize the log file
        try
        {
            logWriter = new BufferedWriter(new FileWriter("cafe_logs.json", true)); // Append mode
        }catch (IOException e) {
            logWriter = null;
            System.out.println("Couldn't create log file");
        }
    }

    //Method that checks if customer already has a pending order before adding
    public void addOrder(String clientID, String customerName, int teas, int coffees) {
        int clientIdInt = Integer.parseInt(clientID);

        synchronized (activeOrders) {
            Order order = activeOrders.get(clientIdInt);

            if (order != null) {
                // Merge new items into the existing order
                order.AddOnTea(teas);
                order.AddOnCoffee(coffees);
                System.out.println("Extra ordered by " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");


            } else {
                // Create a new order
                order = new Order(customerName, teas, coffees);
                activeOrders.put(clientIdInt, order);
                System.out.println("New order place by " + customerName + ": " + teas + " tea(s), " + coffees + " coffee(s).");
            }

            orderQueue.add(order); //Add order to the queue
        }

        processOrders(); //Start processing
        cafeLogState(); //Output log status

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

    //Method to set "waiting" drinks to brew if slot is available
    private void processOrderDrinks(Order order) {
        while (order.countTeasByState("WAITING") > 0 || order.countCoffeesByState("WAITING") > 0) {
            if (!activeOrders.containsValue(order)) {
                // Stop processing if the order has been cancelled
                break;
            }

            // Process teas
            if (brewingTeas.get() < 2 && order.countTeasByState("WAITING") > 0) {
                String teaID;
                synchronized (order) {
                    teaID = order.getNextWaitingTea();
                    if (teaID != null) {
                        brewingTeas.incrementAndGet();
                        order.updateTeaState(teaID, "BREWING");
                        cafeLogState();
                    }
                }
                if (teaID != null) {
                    startBrewingDrink(order, teaID, "Tea", teaBrewTime);
                }
            }

            // Process coffees
            if (brewingCoffees.get() < 2 && order.countCoffeesByState("WAITING") > 0) {
                String coffeeID;
                synchronized (order) {
                    coffeeID = order.getNextWaitingCoffee();
                    if (coffeeID != null) {
                        brewingCoffees.incrementAndGet();
                        order.updateCoffeeState(coffeeID, "BREWING");
                        cafeLogState();
                    }
                }
                if (coffeeID != null) {
                    startBrewingDrink(order, coffeeID, "Coffee", coffeeBrewTime);
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
                        brewingTeas.decrementAndGet();
                    } else if ("Coffee".equals(drinkType)) {
                        order.updateCoffeeState(drinkID, "TRAY");
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

    // Logs the current cafe state to both the terminal and a JSON file for persistent record-keeping.
    // JSON entries are timestamped
    public void cafeLogState() {

        int totalWaitingTeas = 0;
        int totalWaitingCoffees = 0;
        int totalBrewingTeas = brewingTeas.get(); // Brewing slot tracking remains atomic
        int totalBrewingCoffees = brewingCoffees.get(); // Brewing slot tracking remains atomic
        int totalTrayTeas = 0;
        int totalTrayCoffees = 0;

        // Iterate through activeOrders to calculate states
        for (Order order : activeOrders.values()) {
            totalWaitingTeas += order.countTeasByState("WAITING");
            totalWaitingCoffees += order.countCoffeesByState("WAITING");
            totalTrayTeas += order.countTeasByState("TRAY");
            totalTrayCoffees += order.countCoffeesByState("TRAY");
        }

        // Build and display the log
        StringBuilder log = new StringBuilder();
        log.append("--- Cafe Log ---\n");
        log.append("Number of clients in the cafe: ").append(customers.size()).append("\n");

        long clientsWaiting = customers.values().stream()
                .filter("WAITING"::equals)
                .count();
        log.append("Number of clients waiting for orders: ").append(clientsWaiting).append("\n");

        log.append("Items in waiting area: ").append(totalWaitingTeas).append(" tea(s), ")
                .append(totalWaitingCoffees).append(" coffee(s)\n");
        log.append("Items in brewing area: ").append(totalBrewingTeas).append(" tea(s), ")
                .append(totalBrewingCoffees).append(" coffee(s)\n");
        log.append("Items in tray area: ").append(totalTrayTeas).append(" tea(s), ")
                .append(totalTrayCoffees).append(" coffee(s)\n");

        System.out.println(log);

        // Write JSON log if logWriter is initialized
        if (logWriter != null) {
            try {
                JsonObject logEntry = new JsonObject();
                logEntry.addProperty("timestamp", LocalDateTime.now().toString());
                logEntry.addProperty("clients_in_cafe", customers.size());
                logEntry.addProperty("clients_waiting", clientsWaiting);
                logEntry.addProperty("waiting_teas", totalWaitingTeas);
                logEntry.addProperty("waiting_coffees", totalWaitingCoffees);
                logEntry.addProperty("brewing_teas", totalBrewingTeas);
                logEntry.addProperty("brewing_coffees", totalBrewingCoffees);
                logEntry.addProperty("tray_teas", totalTrayTeas);
                logEntry.addProperty("tray_coffees", totalTrayCoffees);

                logWriter.write(new Gson().toJson(logEntry));
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.out.println("Failed to write log to JSON file: " + e.getMessage());
            }
        }
    }

    //Shutdown brewing threads and close logWriter when cafe terminates
    public void shutdownCafe() {
        try {
            if (logWriter != null) {
                logWriter.close();
            }
        } catch (IOException e) {
            System.out.println("Failed to close log file: " + e.getMessage());
        }
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

            cafeLogState();
            return true;
        }
        return false;
    }

    //Remove order for active orders and repurpose drinks to other customers if possible
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

            System.out.println("Removed " + waitingTeasCount + " teas and " + waitingCoffeesCount + " coffees from waiting area for " + cancelledOrder.getCustomerName()+".");

            // Handle drinks in the BREWING and TRAY areas
            repurposeBrewingAndTrayDrinks(cancelledOrder);
        }
    }
    //Check all teas and coffees for their status and then transfer brewing and tray drinks to someone else
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
                    }
                    System.out.println(coffeeID + " from " + cancelledOrder.getCustomerName() + " discarded.");
                }
                return true; // Remove the coffee from cancelledOrder's map
            }
            return false; // Keep WAITING coffees, if any
        });
    }

    private boolean transferDrink(Order cancelledOrder, String drinkType, boolean isBrewing)
    {
        for (Order order : activeOrders.values()) {
            if (!order.equals(cancelledOrder)) { // Skip the canceled order
                synchronized (order) {
                    // Check if there are drinks in the waiting area to repurpose
                    if ("Tea".equals(drinkType) && order.countTeasByState("WAITING") > 0) {
                        if (isBrewing && brewingTeas.get() >= 2) {
                            continue; // Skip transfer if no brewing slots are available
                        }

                        String newTeaID = order.getNextWaitingTea();
                        if (newTeaID != null) {
                            order.updateTeaState(newTeaID, isBrewing ? "BREWING" : "TRAY");

                            // Update atomic counters
                            if (isBrewing) {
                                brewingTeas.incrementAndGet();
                            }

                            transferLog(drinkType, cancelledOrder, order, isBrewing);
                            return true; // Transfer successful
                        }
                    } else if ("Coffee".equals(drinkType) && order.countCoffeesByState("WAITING") > 0) {
                        if (isBrewing && brewingCoffees.get() >= 2) {
                            continue; // Skip transfer if no brewing slots are available
                        }

                        String newCoffeeID = order.getNextWaitingCoffee();
                        if (newCoffeeID != null) {
                            order.updateCoffeeState(newCoffeeID, isBrewing ? "BREWING" : "TRAY");

                            // Update atomic counters
                            if (isBrewing) {
                                brewingCoffees.incrementAndGet();
                            }

                            transferLog(drinkType, cancelledOrder, order, isBrewing);
                            return true; // Transfer successful
                        }
                    }
                }
            }
        }
        return false; // Transfer not successful
    }

    //Terminal output for transfers
    private void transferLog(String drinkType, Order cancelledOrder, Order recipientOrder, boolean isBrewing) {
        String sourceCustomer = cancelledOrder.getCustomerName();
        String targetCustomer = recipientOrder.getCustomerName();
        String location = isBrewing ? "currently brewing" : "in the tray";
        System.out.println(drinkType + " " + location + " for " + sourceCustomer + " has been transferred to " + targetCustomer + "'s order.");
    }

}

