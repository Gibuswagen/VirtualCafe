package Helpers;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class CustomerHandler implements Runnable
{
    private final Socket socket;
    private Cafe cafe;
    private HashMap<String, String> customersHM = new HashMap<>();

    public CustomerHandler(Socket socket, HashMap<String, String> customersHM)
    {
        this.socket = socket;
        this.customersHM = customersHM;
    }
    @Override
    public void run()
    {
        String customerName = null;

        try
        {
            Scanner scanner = new Scanner(socket.getInputStream());
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            try
            {
                //Get customer's name
                customerName = scanner.nextLine();
                System.out.println(customerName + " walked into the cafe.");

                //Handle command requests from customer
                while(true)
                {
                    String request = scanner.nextLine().toLowerCase();
                    String[] parts = request.split(" ");

                }


            }catch (Exception e){
                writer.println("ERROR "+ e.getMessage());
                socket.close();
            }

        }catch (Exception e) {
        }finally {
            System.out.println("Customer " + customerName + " left.");
        }


    }

}
