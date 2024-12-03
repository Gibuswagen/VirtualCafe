import Helpers.CustomerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Barista {
    private final static int port = 2610;
    private static HashMap<String,String> customersHM = new HashMap<>();

    public static void main(String[] args)
    {
        OpenCafe();
    }

    //Method to start server
    private static void OpenCafe()
    {
        //Feed try argument with serverSocket
        try(ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Cafe is open. Waiting for customers...");
            while (true)
            {
                //Wait for connection
                Socket socket = serverSocket.accept();

                //Add customer to the hashmap
                customersHM.put(Integer.toString(socket.getPort()),"idle");

                //Start thread to handle the customer
                new Thread(new CustomerHandler(socket,customersHM)).start();
            }

        }catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
