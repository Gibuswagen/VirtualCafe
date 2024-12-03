package Helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class CustomerConnection implements AutoCloseable
{
    private final int port = 2610;

    public CustomerConnection(String name) throws Exception {

        //Connecting to the server and creating objects for communication
        Socket socket = new Socket("localhost", port);
        Scanner reader = new Scanner(socket.getInputStream());
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        //Send customer name
        writer.println(name);

        //Parsing the response
        String response = reader.nextLine();
        if (response.trim().compareToIgnoreCase("success") != 0)
            throw new Exception(response);
    }

    @Override
    public void close() throws Exception {

    }
}
