import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Helpers.CustomerConnection;
public class Customer{
    private static final int port = 2610;

    public static void main(String[] args) {
        System.out.println("Welcome to the Virtual Cafe!");

        System.out.println("How may I address you?\n");

        try
        {
            //Get customer's name
            Scanner input = new Scanner(System.in);
            String name = input.nextLine();

            //Try with resources to connect to the server and start ordering
            try (CustomerConnection customer = new CustomerConnection(name))
            {
                String command = "";
                while(!command.equals("exit"))
                {
                    System.out.println(name + ", what would you like to do?\n");

                    //Get command input
                    command = input.nextLine().toLowerCase();

                    //Split input to check for first command action
                    String[] parts = command.split(" ",2);
                    switch(parts[0])
                    {
                        case "order":
                            if(parts.length > 1 && parts[1].equals("status"))
                            {
                                //HANDLE STATUS COMMAND
                                System.out.println("ORDER STATUS TRIGGERED");
                            }else{
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
                                    String response = customer.placeOrder(teaCount, coffeeCount);
                                    System.out.println(response);
                                } else{
                                    System.out.println("Invalid order format! Please try again.");
                                }
                            }
                            break;
                        case "collect":
                            //Implement collection
                            break;
                        case "exit":
                            customer.exitCafe();
                            break;
                        default:
                            System.out.println("Invalid command syntax.");
                            break;
                    }

                }

            }catch (Exception e) {
                System.out.println(e.getMessage());
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }
}
