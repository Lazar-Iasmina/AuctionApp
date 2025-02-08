package org.example;

import java.io.*;
import java.net.*;

public class AuctionClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12346);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            // Receive welcome message
            System.out.println(in.readLine());

            // Ask for the user's name
            System.out.print("Enter your name: ");
            String name = userInput.readLine().trim();
            out.println(name);

            // Read but ignore the seller question from the server
            in.readLine();  // Skip the "Are you a seller?" message from the server

            // Ask the user directly if they are a seller
            String role;
            do {
                System.out.print("Are you a seller? (yes/no): ");
                role = userInput.readLine().trim();
            } while (!role.equalsIgnoreCase("yes") && !role.equalsIgnoreCase("no"));
            out.println(role);
            boolean isSeller = role.equalsIgnoreCase("yes");

            // Receive server's response after setting the role
            System.out.println(in.readLine());

            // Start a thread to listen for updates from the server
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        if (!serverResponse.trim().isEmpty()) { // Prevents printing empty lines
                            if (!serverResponse.startsWith("To bid, enter:") && !serverResponse.startsWith("Available Auctions:")) {
                                System.out.println(serverResponse);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from auction server.");
                }
            }).start();

            // Instructions based on user type
            if (isSeller) {
                System.out.println("To create an auction, enter: item:reservePrice:auctionType");
                System.out.println("Auction types: Standard, Sealed Bid");
                System.out.println("Example: Laptop:500:Standard");
            } else {
                System.out.println("To place a bid, enter: item:bidAmount");
                System.out.println("Example: Laptop:600");
            }

            // Send user inputs
            String input;
            while ((input = userInput.readLine()) != null) {
                if (!input.trim().isEmpty()) { // Prevent sending empty messages
                    out.println(input);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not connect to auction server.");
        }
    }
}
