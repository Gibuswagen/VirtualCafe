package helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Facilitates communication between a customer and the cafe server.

public class CustomerConnection implements AutoCloseable
{
    private boolean isNormalExit = false;
    private final int port = 2610;
    private final Socket socket;
    private final Scanner reader;
    private final PrintWriter writer;

    public CustomerConnection(String name) throws Exception {
        try
        {
            //Connecting to the server and creating objects for communication
            socket = new Socket("localhost", port);
            reader = new Scanner(socket.getInputStream());
            writer = new PrintWriter(socket.getOutputStream(), true);

            //Send customer name
            writer.println(name);


            //Parsing the response
            String response = reader.nextLine();
            if (response.trim().compareToIgnoreCase("success") != 0)
                throw new Exception("Barista kicked you out after hearing your name...");

        }catch(IOException e){
            throw new Exception("Cafe magically disappeared...");
        }
    }

    // Listens for server messages and logs them in real-time to keep the client informed of status updates
    public void receiveBaristasMessages(Runnable onServerDisconnect) {
        new Thread(() -> {
            try {
                while (reader.hasNextLine()) { // Keep reading messages from the server
                    String message = reader.nextLine(); // Read message
                    System.out.println(message);
                }
                // check if it was a normal exit
                if (!isNormalExit) {
                    onServerDisconnect.run();//sets serverAlive = false
                }
            } catch (Exception e) {
                if (!isNormalExit) {
                    onServerDisconnect.run();//sets serverAlive = false
                }
            } finally {
                try {
                    close(); // Ensure resources are cleaned up
                } catch (Exception e) {
                    System.out.println("Error during cleanup: " + e.getMessage());
                }
            }
        }).start();
    }


    //SET OF METHODS TO SEND REQUESTS
    public void placeOrder(int teaCount,int coffeeCount)
    {
        //Send request to place an Order
        writer.println("PLACE_ORDER "+teaCount+" "+coffeeCount);
    }

    public void orderStatus()
    {
        //Send request to see order status
        writer.println("ORDER_STATUS");
    }

    public void attemptCollection()
    {
        //Send request to collect an order
        writer.println("COLLECT");
    }

    public void exitCafe()
    {
        isNormalExit = true;// Set the flag for a normal exit
        writer.println("EXIT"); //Send exit status
    }

    @Override
    public void close() {
        //Close reader and writer
        reader.close();
        writer.close();

    }
}
