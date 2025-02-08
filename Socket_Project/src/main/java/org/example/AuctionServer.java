package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {
    private static final ConcurrentHashMap<String, AuctionItem> auctionItems = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<PrintWriter>> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12346);
        System.out.println("Auction Server started on port 12346...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class AuctionItem {
        String highestBidder;
        int highestBid;
        int reservePrice;
        Timer timer;
        long endTime;
        String auctionType;
        boolean sold;

        public AuctionItem(int reservePrice, String auctionType) {
            this.highestBid = 0;
            this.highestBidder = "No bids yet";
            this.reservePrice = reservePrice;
            this.auctionType = auctionType;
            this.sold = false;
            setTimer();
        }

        private void setTimer() {
            this.endTime = System.currentTimeMillis() + 60000;
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    endAuction();
                }
            }, 60000);
        }

        public void endAuction() {
            String result;
            if (highestBid >= reservePrice) {
                sold = true;
                result = "Auction for item ended! Winner: " + highestBidder + " for " + highestBid;
            } else {
                result = "Auction ended! No winning bid, reserve price was not met.";
            }
            ClientHandler.notifyAllClients(result);
            auctionItems.remove(this.auctionType); // Remove item from memory
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String username;
        private boolean isSeller;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                this.out = out;

                out.println("Welcome to Silent Auction! Please enter your name:");
                username = in.readLine();

                out.println("Are you a seller? (yes/no):");
                isSeller = in.readLine().trim().equalsIgnoreCase("yes");
                out.println("Hello, " + username + "! " + (isSeller ? "You can create auctions." : "You can bid on items."));

                clients.putIfAbsent(username, new ArrayList<>());
                clients.get(username).add(out);

                if (!isSeller) {
                    out.println("Available Auctions:");
                    auctionItems.forEach((item, details) -> out.println(item + " - Highest bid: " + details.highestBid + " by " + details.highestBidder));
                    out.println("To bid, enter: item:bidAmount");
                }

                String input;
                while ((input = in.readLine()) != null) {
                    if (isSeller) {
                        processSellerInput(input);
                    } else {
                        processBuyerInput(input);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected.");
            }
        }

        private void processSellerInput(String input) {
            String[] parts = input.split(":");
            if (parts.length == 3) {
                String item = parts[0].trim();
                int reservePrice;
                String auctionType = parts[2].trim();

                try {
                    reservePrice = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    out.println("Invalid reserve price.");
                    return;
                }

                auctionItems.putIfAbsent(item, new AuctionItem(reservePrice, auctionType));
                notifyAllClients("New auction started: " + item + " (Type: " + auctionType + ")");
            }
        }

        private void processBuyerInput(String input) {
            String[] parts = input.split(":");
            if (parts.length == 2) {
                String item = parts[0].trim();
                int bid;

                try {
                    bid = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    out.println("Invalid bid amount.");
                    return;
                }

                if (auctionItems.containsKey(item)) {
                    AuctionItem auction = auctionItems.get(item);
                    if (!auction.sold) { // Prevent bidding on ended auctions
                        if (bid > auction.highestBid) {
                            auction.highestBid = bid;
                            auction.highestBidder = username;
                            notifyAllClients("New highest bid for " + item + ": " + bid + " by " + username);
                        } else {
                            out.println("Your bid must be higher than the current highest bid.");
                        }
                    } else {
                        out.println("This auction has ended. No further bids are allowed.");
                    }
                } else {
                    out.println("No such auction exists.");
                }
            }
        }

        public static void notifyAllClients(String message) {
            for (List<PrintWriter> clientWriters : clients.values()) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }
}
