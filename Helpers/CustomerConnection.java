package Helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class CustomerConnection implements AutoCloseable
{
    private final int port = 2610;
    private final Socket socket;
    private final Scanner reader;
    private final PrintWriter writer;

    public CustomerConnection(String name) throws Exception {

        //Connecting to the server and creating objects for communication
        socket = new Socket("localhost", port);
        reader = new Scanner(socket.getInputStream());
        writer = new PrintWriter(socket.getOutputStream(), true);

        //Send customer name
        writer.println(name);

        //Parsing the response
        String response = reader.nextLine();
        if (response.trim().compareToIgnoreCase("success") != 0)
            throw new Exception(response);
    }

    public String readResponse() {
        if (reader.hasNextLine()) {
            return reader.nextLine();
        }
        return "No reply...";
    }

    public String placeOrder(int teaCount,int coffeeCount)
    {
        //Send request to place an Order
        writer.println("PLACE_ORDER "+teaCount+" "+coffeeCount);

        System.out.println("request sent - PLACE_ORDER "+teaCount+" "+coffeeCount);

        return readResponse();
    }

    public void exitCafe()
    {
        //Send exit status
        writer.println("EXIT");

    }

    @Override
    public void close() throws Exception {
        //Close reader and writer
        reader.close();
        writer.close();

    }
}
