# Silent Auction System

## Introduction
This is a simple client-server auction system where users can participate as either sellers or buyers. Sellers can create auctions for items, setting a reserve price and an auction type. Buyers can place bids on listed items. The auction system automatically ends auctions after a set time and announces the results to all connected clients.

## Features
- Sellers can create auctions with a reserve price.
- Buyers can place bids on available auctions.
- The auction system runs in real-time, broadcasting updates to all clients.
- Auctions automatically close after a specified duration.
- Uses TCP sockets for communication between clients and the server.

## Code Overview

### AuctionClient.java
This file contains the client-side code that allows users to connect to the auction server, interact as either a seller or a buyer, and send or receive updates.

#### Key Sections:

##### Connecting to the Auction Server
```java
Socket socket = new Socket("localhost", 12346);
BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
```
- Establishes a connection to the server on port `12346`.
- Creates input/output streams for communication.

##### Handling User Input
```java
System.out.print("Enter your name: ");
String name = userInput.readLine().trim();
out.println(name);
```
- Prompts the user for their name and sends it to the server.

##### Identifying Role (Seller or Buyer)
```java
System.out.print("Are you a seller? (yes/no): ");
String role = userInput.readLine().trim();
out.println(role);
boolean isSeller = role.equalsIgnoreCase("yes");
```
- Determines if the user is a seller or a buyer.

##### Listening for Server Updates
```java
new Thread(() -> {
    try {
        String serverResponse;
        while ((serverResponse = in.readLine()) != null) {
            System.out.println(serverResponse);
        }
    } catch (IOException e) {
        System.out.println("Disconnected from auction server.");
    }
}).start();
```
- Starts a background thread to listen for and display server messages in real-time.

### AuctionServer.java
This file implements the server-side logic, handling multiple client connections and managing auction processes.

#### Key Sections:

##### Starting the Auction Server
```java
ServerSocket serverSocket = new ServerSocket(12346);
System.out.println("Auction Server started on port 12346...");
```
- Binds the server to port `12346` and listens for incoming client connections.

##### Handling Client Connections
```java
Socket clientSocket = serverSocket.accept();
new Thread(new ClientHandler(clientSocket)).start();
```
- Accepts new clients and creates a separate thread for each one.

##### Managing Auctions
```java
private static final ConcurrentHashMap<String, AuctionItem> auctionItems = new ConcurrentHashMap<>();
```
- Stores all active auction items.

##### Auction Timer
```java
this.timer = new Timer();
this.timer.schedule(new TimerTask() {
    @Override
    public void run() {
        endAuction();
    }
}, 60000);
```
- Each auction runs for 60 seconds before ending automatically.

##### Processing Bids
```java
if (bid > auction.highestBid) {
    auction.highestBid = bid;
    auction.highestBidder = username;
    notifyAllClients("New highest bid for " + item + ": " + bid + " by " + username);
} else {
    out.println("Your bid must be higher than the current highest bid.");
}
```
- Ensures bids are higher than the current highest bid before accepting them.

##### Notifying Clients
```java
public static void notifyAllClients(String message) {
    for (List<PrintWriter> clientWriters : clients.values()) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }
}
```
- Sends auction updates to all connected clients.

## Running the Application
### Prerequisites
- Java Development Kit (JDK) installed
- A terminal or command prompt

### Steps
1. **Compile the server and client code:**
   ```sh
   javac AuctionServer.java AuctionClient.java
   ```

2. **Start the Auction Server:**
   ```sh
   java AuctionServer
   ```
   - The server will start listening on port `12346`.

3. **Start multiple Auction Clients:**
   ```sh
   java AuctionClient
   ```
   - Each client will be prompted to enter their name and role (seller or buyer).

4. **Create an Auction (Seller Mode):**
   - Example input:
     ```
     Laptop:500:Standard
     ```
   - This creates an auction for a `Laptop` with a reserve price of `500` and a `Standard` auction type.

5. **Place a Bid (Buyer Mode):**
   - Example input:
     ```
     Laptop:600
     ```
   - This places a bid of `600` on the `Laptop` auction.

6. **Auction Conclusion:**
   - The auction automatically ends after 60 seconds, and the winner is announced.

---
### Notes
- Sellers can create multiple auctions.
- Buyers can bid on any active auction.
- The system supports multiple concurrent clients.

This project provides a foundational auction system that can be extended with additional features such as user authentication, more auction types, and a web-based interface.

