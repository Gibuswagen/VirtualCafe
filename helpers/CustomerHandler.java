package helpers;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

// Handles individual customer requests on the server side.
// Parses commands, manages customer state, and interacts with the `Cafe` class.

public class CustomerHandler implements Runnable
{
    private final Socket socket;
    private final Cafe cafe;
    private final HashMap<String, String> customers;
    private String customerName = null;
    private volatile boolean isRunning = true; // Control flag for the command loops
    private volatile boolean isReadyCheckActive = false; // To track readiness-check status

    public CustomerHandler(Socket socket, Cafe cafe,HashMap<String, String> customers)
    {
        this.socket = socket;
        this.cafe = cafe;
        this.customers = customers;
    }

    @Override
    public void run()
    {
        String clientID = String.valueOf(socket.getPort()); //Use socket as an ID

        try(Scanner scanner = new Scanner(socket.getInputStream());
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true)){

            //Customer initialization
            customerName = scanner.nextLine();
            System.out.println(customerName+" walked into the cafe.");
            customers.put(clientID,"IDLE");

            //Send success response
            writer.println("SUCCESS");

            //Show log in terminal
            cafe.cafeLogState();

            //Main command handling loop
            while(isRunning && scanner.hasNextLine())
            {
                String request = scanner.nextLine().toLowerCase();
                handleCommand(request,writer,clientID);
            }

            } catch (IOException e){
            System.out.println("Connection error for"+customerName+": "+ e.getMessage());
        }finally {
            cleanup(clientID);
        }
    }

    // Processes customer commands (e.g., "place_order", "order_status", "collect").
    private void handleCommand(String request, PrintWriter writer, String clientID)
    {
        String[] parts = request.split(" ");
        switch(parts[0])
        {
            case "place_order":
                handlePlaceOrder(parts, writer, clientID);
                break;
            case "order_status":
                handleOrderStatus(writer, clientID);
                break;
            case "collect":
                handleCollect(writer, clientID);
                break;
            case "exit":
                handleExit(writer,clientID);
                break;
            default:
                writer.println("[Barista]: You gave me an unknown command. Please try again.");
        }
    }

    // VARIOUS COMMAND HANDLING METHODS
    // Interacts with cafe and orders to build appropriate response
    private void handlePlaceOrder(String[] parts, PrintWriter writer, String clientID)
    {
        try
        {
            //Parse order details
            int teas = Integer.parseInt(parts[1]);
            int coffees = Integer.parseInt(parts[2]);

            cafe.addOrder(clientID, customerName,teas,coffees);

            //Build the order place response
            StringBuilder response = new StringBuilder("Order placed "+customerName+": ");
            if(teas > 0)
            {
                response.append(teas).append(" tea(s)");
            }
            if(coffees > 0)
            {
                if(teas > 0)
                {
                    response.append(" and "); // Add "and" if both are present
                }
                response.append(coffees).append(" coffee(s)");
            }

            writer.println(response);
            customers.put(clientID, "WAITING");

            // Begin checking for order completion to inform a customer
            checkOrderReady(writer,clientID);
        }catch (NumberFormatException e) {
            writer.println("[Barista]: Invalid order format. Please specify numbers for teas and coffees.");
        }
    }

    private void handleOrderStatus(PrintWriter writer, String clientID)
    {
        Order order = cafe.getActiveOrder(Integer.parseInt(clientID));
        if(order != null)
        {
            writer.println(order.getOrderStatus());
        } else {
            writer.println("[Barista]: Your currently have no active orders");
        }
    }

    private void handleCollect(PrintWriter writer, String clientID)
    {
        if(cafe.isCollectable(clientID))
        {
            customers.put(clientID,"IDLE");
            cafe.cafeLogState();
            writer.println("[Barista]: You have collected your order! Enjoy!");
            System.out.println(customerName + " has collected his order.");
        }
        else if(cafe.getActiveOrder(Integer.parseInt(clientID)) == null)
        {
            writer.println("[Barista]: You didn't order yet!");
        }
        else
        {
            writer.println("[Barista]: Your order is not ready yet! Please wait.");
        }
    }

    private void handleExit(PrintWriter writer, String clientID)
    {
        try
        {
            isRunning = false; // Stop the main thread loop
            isReadyCheckActive = false; // Stop order check

            Order activeOrder = cafe.getActiveOrder(Integer.parseInt(clientID));
            if(activeOrder != null)
            {
                cafe.cancelOrder(clientID);
            }

            //Remove customer from the cafe
            customers.remove(clientID);
            cafe.cafeLogState();

            writer.println("[Barista]: Goodbye, " + customerName + "! Come again!");
            isRunning = false; // Terminate the thread loop
        } catch (Exception e){
            System.out.println("Error during exit command: "+e.getMessage());
        }
    }

    private void cleanup(String clientID)
    {
        customers.remove(clientID);
        System.out.println(customerName + " has left the cafe.");
    }


    // Runs a thread to monitor if a customer's order is ready.
    // Notifies the customer as soon as the order is ready for collection.
    private void checkOrderReady(PrintWriter writer, String clientID) {
        if (isReadyCheckActive) return; // Prevent multiple threads for readiness check

        isReadyCheckActive = true; // Mark readiness check as active

        new Thread(() -> {
            try {
                Order order = cafe.getActiveOrder(Integer.parseInt(clientID));
                if (order == null) return; // No active order

                synchronized (order.getLock()) {
                    while (isRunning && !order.isReady()) {
                        order.getLock().wait(); // Wait until order is ready or thread is stopped
                    }
                }

                // Notify customer if thread is still running
                if (isRunning) {
                    writer.println("[Barista]: " + customerName + ", your order is ready to collect!");
                }
            } catch (InterruptedException e) {
                System.out.println("Order readiness check interrupted for " + customerName);
            } finally {
                isReadyCheckActive = false; // Reset the flag
            }
        }).start();
    }
}
