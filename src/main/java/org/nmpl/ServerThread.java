package org.nmpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {
    private final Socket clientSocket;
    private final BackendServer backendServer;
    private BufferedReader input;
    private PrintWriter output;

    public ServerThread(Socket socket, BackendServer backendServer) {
        this.clientSocket = socket;
        this.backendServer = backendServer;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        boolean isAuthenticated = false; // Track authentication status
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            while (true) {
                String request = input.readLine();
                if (request == null) {
                    // Client disconnected, wait for some time before attempting to reconnect
                    System.out.println("Client disconnected. Waiting for reconnection...");
                    Thread.sleep(5000); // Wait for 5 seconds before attempting to reconnect
                    continue;
                }
                System.out.println("Received request from " + clientSocket.getInetAddress().getHostAddress() + ": " + request);

                if (!isAuthenticated) {
                    // Handle authentication request
                    if (request.equals("authenticate")) {
                        String username = input.readLine();
                        String password = input.readLine();
                        isAuthenticated = backendServer.authenticate(username, password);
                        output.println(isAuthenticated ? "Authentication successful." : "Authentication failed.");
                        continue; // Skip to the next iteration
                    } else {
                        output.println("Please authenticate first.");
                        continue; // Skip to the next iteration
                    }
                }

                // Process the request and send response
                switch (request) {
                    case "balanceCheck":
                        int userId = Integer.parseInt(input.readLine());
                        double balance = backendServer.checkBalance(userId);
                        output.println(balance);
                        break;
                    case "deposit":
                        int depositUserId = Integer.parseInt(input.readLine());
                        double depositAmount = Double.parseDouble(input.readLine());
                        boolean depositSuccess = backendServer.deposit(depositUserId, depositAmount);
                        output.println(depositSuccess ? "Deposit successful." : "Failed to deposit.");
                        break;
                    case "withdraw":
                        int withdrawUserId = Integer.parseInt(input.readLine());
                        double withdrawAmount = Double.parseDouble(input.readLine());
                        boolean withdrawSuccess = backendServer.withdraw(withdrawUserId, withdrawAmount);
                        output.println(withdrawSuccess ? "Withdrawal successful." : "Failed to withdraw.");
                        break;
                    case "createAccount":
                        String username = input.readLine();
                        String password = input.readLine();
                        boolean createSuccess = backendServer.createUser(username, password);
                        output.println(createSuccess ? "Account created successfully." : "Failed to create account.");
                        break;
                    case "updateAccount":
                        int updateUserId = Integer.parseInt(input.readLine());
                        String newUsername = input.readLine();
                        String newPassword = input.readLine();
                        boolean updateSuccess = backendServer.updateUser(updateUserId, newUsername, newPassword);
                        output.println(updateSuccess ? "Account updated successfully." : "Failed to update account.");
                        break;
                    case "deleteAccount":
                        int deleteUserId = Integer.parseInt(input.readLine());
                        boolean deleteSuccess = backendServer.deleteUser(deleteUserId);
                        output.println(deleteSuccess ? "Account deleted successfully." : "Failed to delete account.");
                        break;
                    case "checkUserExists":
                        int userIds = Integer.parseInt(input.readLine());
                        boolean userExists = backendServer.handleCheckUserExists(userIds);
                        output.println(userExists);
                        break;
                    default:
                        output.println("Invalid request.");
                        break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() {
        try {
            String username = input.readLine();
            String password = input.readLine();
            return backendServer.authenticateUser(username, password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
