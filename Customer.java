import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import helpers.CustomerConnection;

// The client-side application for interacting with the Virtual Cafe.
// Handles user input and leaves communication to the CustomerConnection for clarity

public class Customer{
    private static boolean normalExit = false; // Flag for normal exit
    private static boolean serverAlive = true; // Flag to track server connection status

    public static void main(String[] args) {
        System.out.println("Welcome to the Virtual Cafe!");

        System.out.println("How may I address you?\n");

        try
        {
            //Get customer's name
            Scanner input = new Scanner(System.in);
            String name = input.nextLine();


            //Intercept and gracefully handle SIGINT
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                if (!normalExit) { // Only run this logic if it's not a normal exit
                    System.out.println("*You decide to leave*");
                }
            }));

            //Try with resources to connect to the server and start ordering
            try (CustomerConnection customer = new CustomerConnection(name))
            {

                customer.receiveBaristasMessages(() -> serverAlive = false);

                String command = "";

                System.out.println("[Barista]: What would you like to do?");

                while(!command.equals("exit") && serverAlive) //Prompt input until exit or server connection is lost
                {

                    //Get command input
                    command = input.nextLine().toLowerCase();

                    //Split input to check for first command action
                    String[] parts = command.split(" ",2);
                    switch(parts[0])
                    {
                        case "order":
                            if(parts.length == 1)
                            {
                                System.out.println("[Barista]: Invalid order syntax! use 'order status' or 'order <number> <drink>'");
                            }
                            else if(parts[1].equals("status"))
                            {
                                customer.orderStatus();
                            }
                            else{
                                Pattern pattern = Pattern.compile("(\\d+)\\s*(tea|teas|coffee|coffees)");
                                Matcher matcher = pattern.matcher(parts[1]);

                                int teaCount = 0;
                                int coffeeCount = 0;

                                // Aggregate counts of teas and coffees
                                while (matcher.find()) {
                                    int count = Integer.parseInt(matcher.group(1));
                                    String drinkType = matcher.group(2).toLowerCase();

                                    if (drinkType.startsWith("tea")) {
                                        teaCount += count;
                                    } else if (drinkType.startsWith("coffee")) {
                                        coffeeCount += count;
                                    }
                                }

                                if (teaCount > 0 || coffeeCount > 0) {

                                    // Make CustomerConnection send an order request and get response
                                    customer.placeOrder(teaCount, coffeeCount);

                                } else{
                                    System.out.println("Invalid order format! Please try again.");
                                }
                            }
                            break;
                        case "collect":
                            customer.attemptCollection();
                            break;
                        case "exit":
                            System.out.println("*You exit the cafe*");
                            normalExit = true;
                            customer.exitCafe();
                            break;
                        default:
                            System.out.println("Invalid command syntax! Try again!");
                            break;
                    }

                }

            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (!serverAlive){
                System.out.println("Cafe has magically disappeared...");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }
}
