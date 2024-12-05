# Virtual Cafe Management System

## Overview
The Virtual Cafe Management System is a multi-threaded Java program simulating a virtual cafe environment. Customers can place orders for teas and coffees, track the status of their orders, and collect their prepared drinks. The system includes a server (`Barista`) managing the cafe's operations and clients (`Customer`) interacting with the cafe.

## Features
- **Multi-threaded Drink Preparation**: Efficiently manages up to 4 concurrent brewing tasks with separate brewing slots for teas and coffees.
- **Order Management**: Handles multiple customers with dynamic order updates, cancellations, and transfers of unfinished drinks.
- **State Logging**:
  - Real-time terminal logs display the cafe's state, including the number of items in different areas (waiting, brewing, tray).
  - JSON logging provides a persistent record of events with timestamps.
- **Graceful Shutdown**: Ensures proper cleanup of threads and resources when the cafe is closed.
- **Concurrency-Safe Design**: Implements synchronized blocks and atomic counters for thread-safe operations.

## How to Compile and Run

### Prerequisites
- Java Development Kit (JDK) 23 or later.
- Gson library (`gson-2.11.0.jar`) for JSON handling.

### Compilation
1. Place the `gson-2.11.0.jar` file in the root directory of your project.
2. Open a terminal and navigate to the project folder.
3. Compile the program using the following command:
```bash
  Linux: 
  javac -cp ".:gson-2.11.0.jar" Barista.java
  
  Windows:
  javac -cp ".;gson-2.11.0.jar" Barista.java
  ```
### How to Run
```bash
  Run the server(Linux): java -cp ".:gson-2.11.0.jar" Barista.java   

  Run the server(Windows): java -cp ".;gson-2.11.0.jar" Barista.java   

  Run the client: java Customer.java
  ```
## Customer
* Gets prompt to enter they name, upon which check for connection are being made.
* Upon successful connection, customer may enter following commands:
  ```
  >>order status
  
  Order status for Ibragim:
  - 0 tea(s) and 1 coffee(s) in waiting area
  - 0 tea(s) and 2 coffee(s) currently brewing
  - 2 tea(s) and 0 coffee(s) ready on the tray
  _____________________________________________
  >>order 1 tea and 1 coffee
  >>order 5 teas
  >>order 3 coffees
  
  Order placed Ibragim: 1 tea(s) and 1 coffee(s)
  ...
  _____________________________________________
  >>collect
  
  - [Barista]: You have collected your order! Enjoy!
  - [Barista]: Your order is not ready yet! Please wait.
  - [Barista]: You didn't order yet!
  _____________________________________________
  >>exit
  
  *You exit the cafe*
  [Barista]: Goodbye, Ibragim! Come again!
  
  >>Ctri+C (interrupt)
  *You decide to leave*
  _____________________________________________
    ```
* Receives message by Barista once Customer's order has been fully prepared.
  ```
  [Barista]: Ibragim, your order is ready to collect!
  ```
## Server
* Manages incoming client connections and routes them to appropriate handlers.
* Receives commands from the customer and perform corresponding actions
* Always logs when a customer enter/leaves to the terminal
```
CustomerOne walked into the cafe.
CustomerTwo has left the cafe
```
* Logs the current cafe state to both the terminal and a JSON file for persistent record-keeping.
```
--- Cafe Log ---
Number of clients in the cafe: 1
Number of clients waiting for orders: 1
Items in waiting area: 0 tea(s), 0 coffee(s)
Items in brewing area: 2 tea(s), 2 coffee(s)
Items in tray area: 3 tea(s), 3 coffee(s)
```
* When customer exits, their pending order is cancel and then repurposed for other orders
```
Removed 2 teas and 6 coffees from waiting area for Bob
Tea in the tray for Bob has been transferred to Ibragim's order.
Coffee in the tray for Bob has been transferred to Ibragim's order.
```
* When server terminates, it shuts down threads and streams

