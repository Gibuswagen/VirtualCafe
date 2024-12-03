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

    public CustomerHandler(Socket socket, Cafe cafe,HashMap<String, String> customers)
    {
        this.socket = socket;
        this.cafe = cafe;
        this.customers = customers;
    }

    @Override
    public void run()
    {
        String customerName = null;
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

                //logCafeState():

                //Send success response
                writer.println("SUCCESS");

                //DISPLAY CAFE STATUS HERE LATER


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
                            writer.println("Order placed for "+customerName+": " + teas + " tea(s), " + coffees + " coffee(s).");
                            customers.put(clientID,"WAITING");
                            //log
                            break;

                        case "order_status":
                            //Check what is where for client
                            break;

                        case "collect":
                            //Collection implementation
                            break;

                        case "exit":
                            customers.remove(clientID);
                            //logstate
                            socket.close(); //Close socket
                            break;

                        default:
                            throw new Exception("CustomerHandler received unknown request");
                    }

                }


            }catch (Exception e){
                writer.println("ERROR "+ e.getMessage());
                socket.close();
            }

        }catch (Exception e) {
        }finally {
            customers.remove(clientID);
            //logstate
            System.out.println("Customer " + customerName + " left.");
        }


    }

}
