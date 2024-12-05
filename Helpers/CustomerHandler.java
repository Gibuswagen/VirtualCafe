package Helpers;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class CustomerHandler implements Runnable
{
    private final Socket socket;
    private Cafe cafe;
    private HashMap<String, String> customers;
    private String customerName = null;
    private volatile boolean isCheckingReady = false; // To track readiness-check status

    public CustomerHandler(Socket socket, Cafe cafe,HashMap<String, String> customers)
    {
        this.socket = socket;
        this.cafe = cafe;
        this.customers = customers;
    }

    @Override
    public void run()
    {
        String clientID = String.valueOf(socket.getPort()); //Use socket's port as an ID
        try
        {
            Scanner scanner = new Scanner(socket.getInputStream());
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            try
            {
                //Get customer's name
                customerName = scanner.nextLine();
                System.out.println(customerName + " walked into the cafe.");

                customers.put(clientID,"IDLE");

                //Send success response
                writer.println("SUCCESS");

                //Handle command requests from customer
                while(true)
                {
                    String request = scanner.nextLine().toLowerCase();
                    String[] parts = request.split(" ");

                    switch(parts[0])
                    {
                        case "place_order":
                            //Parse order details
                            int teas = Integer.parseInt(parts[1]);
                            int coffees = Integer.parseInt(parts[2]);

                            cafe.addOrder(clientID, customerName, teas, coffees);

                            // Build the response message
                            StringBuilder response = new StringBuilder("Order placed for " + customerName + ": ");
                            if (teas > 0) {
                                response.append(teas).append(" tea(s)");
                            }
                            if (coffees > 0) {
                                if (teas > 0) {
                                    response.append(" and "); // Add "and" if both teas and coffees are present
                                }
                                response.append(coffees).append(" coffee(s)");
                            }

                            writer.println(response);
                            customers.put(clientID,"WAITING");
                            break;

                        case "order_status":
                            // Retrieve the active order for the customer
                            Order order = cafe.getActiveOrder(Integer.parseInt(clientID));
                            if (order != null) {
                                writer.println(order.getOrderStatus());
                            } else {
                                writer.println("You currently have no active orders.");
                            }
                            break;

                        case "collect":
                            if(cafe.isCollectable(clientID))
                            {
                                customers.put(clientID,"IDLE");
                                writer.println("You have collected your order! Enjoy!");
                            }else{
                                writer.println("Your order is not ready yet! Please wait!");
                            }
                            break;

                        case "exit":
                            try
                            {
                                // Check if the customer has an active order
                                if (cafe.getActiveOrder(Integer.parseInt(clientID)) != null)
                                {
                                    //Cancel their order
                                    //cafe.cancelOrder(clientID);
                                }

                                //Remove customer from the cafe
                                customers.remove(clientID);

                                writer.println("[Barista]: "+"Goodbye, "+customerName+"! Come again!");
                                System.out.println(customerName + " has left the cafe.");

                                socket.close(); //Close socket

                            } catch (Exception e){
                                System.out.println("Error during exit command: "+ e.getMessage());
                            }

                            break;

                        default:
                            throw new Exception("CustomerHandler received unknown request");
                    }

                }


            }catch (Exception e){
                System.out.println("ERROR "+ e.getMessage());
                socket.close();
            }

        }catch (Exception e) {
            System.out.println("ERROR "+ e.getMessage());
        }finally {
            customers.remove(clientID);
            System.out.println("Customer " + customerName + " left.");
        }
    }
    private void checkOrderReady(String clientID) {
        if (isCheckingReady) return; // Prevent multiple threads for the same customer

        isCheckingReady = true; // Mark as running

        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(3000); // Poll every 3 seconds
                    Order order = cafe.getActiveOrder(Integer.parseInt(clientID));
                    if (order != null && order.isReady()) {
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        writer.println(customerName + ", your order is ready to collect!");
                        isCheckingReady = false; // Reset the flag
                        break; // Exit loop once notification is sent
                    }
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("Order readiness check interrupted for " + customerName);
            } finally {
                isCheckingReady = false; // Ensure flag is reset in case of errors
            }
        }).start();
    }
}
