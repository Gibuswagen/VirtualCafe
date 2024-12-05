import helpers.Cafe;
import helpers.CustomerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

// The main server application for the Virtual Cafe.
// Manages incoming client connections and routes customer requests to appropriate handlers.

public class Barista {
    private final static int port = 2610;
    private static final HashMap<String,String> customers = new HashMap<>(); //HashMap to keep track of clients and their activity

    public static void main(String[] args)
    {
        OpenCafe();
    }

    //Start server
    private static void OpenCafe()
    {
        final Cafe cafe = new Cafe(customers);

        //Handle SIGINT signal
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down cafe...");
            cafe.shutdownCafe();
        }));

        //Try with resources
        try(ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Cafe is open. Waiting for customers...");
            while (true)
            {
                //Wait for connection
                Socket socket = serverSocket.accept();

                //Add customer to the hashmap
                customers.put(Integer.toString(socket.getPort()),"IDLE");

                //Start thread to handle the customer
                new Thread(new CustomerHandler(socket,cafe,customers)).start();
            }

        }catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
