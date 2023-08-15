package org.nmpl;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class BackendServer {
    private int PORT_SOCKET;
    private int PORT_WEB;
    private String DB_NAME;
    private String DB_IP;
    private String DB_USERNAME;
    private String DB_PASSWORD;
    private Connection connection;
    private final ExecutorService threadPool;
        public BackendServer() {

            // Get the configs
            readConfig();

            // Initialize database connection
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://" + DB_IP + "/" + DB_NAME, DB_USERNAME, DB_PASSWORD);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }

            // Create a thread pool for handling client connections
            threadPool = Executors.newFixedThreadPool(10); // Adjust the pool size as needed
        }

    public void start() {
        WebServer webServer = new WebServer(PORT_WEB, this);
        try {
            //Webserver
            webServer.start();
            System.out.println("Web Server started and Listening on port: " + PORT_WEB + "...");

            //Socket Server
            ServerSocket serverSocket = new ServerSocket(PORT_SOCKET);
            System.out.println("Socket Server started and Listening on port: " + PORT_SOCKET + "...");

            System.out.println("Socket Server URL: http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT_SOCKET);
            System.out.println("Web Server URL: http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT_WEB);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress() + " on port " + socket.getPort());

                System.out.println("Local Address: " + socket.getLocalAddress() + " Local Port: " + socket.getLocalPort());
                System.out.println("Remote Address: " + socket.getRemoteSocketAddress() + " Remote Port: " + socket.getPort());

                System.out.println("Socket Timeout: " + socket.getSoTimeout() + " ms");
                System.out.println("Socket Bound to Local Address: " + socket.isBound());

                threadPool.execute(new ServerThread(socket, this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean authenticate(String username, String password) {
        String query = "SELECT teller_id FROM teller_master WHERE teller_name = ? AND teller_password = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next(); // If there's a row, the user is authenticated
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean authenticateUser(String username, String password) {
        String query = "SELECT id FROM User_Account_Master WHERE username = ? AND password = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println("User authenticated!");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public int getAccountIdByUsername(String username) {
        String query = "SELECT id FROM User_Account_Master WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return a negative value to indicate an error
    }
    public boolean transferMoney(int senderId, int recipientId, double amount) {
        try {
            connection.setAutoCommit(false); // Start a transaction

            // Get the current balance of the sender
            double senderBalance = checkBalance(senderId);
            if (senderBalance < 0 || senderBalance < amount) {
                return false; // Sender not found, error occurred, or insufficient balance
            }

            // Get the current balance of the recipient
            double recipientBalance = checkBalance(recipientId);
            if (recipientBalance < 0) {
                return false; // Recipient not found or error occurred
            }

            // Update sender's balance
            double newSenderBalance = senderBalance - amount;
            String updateSenderQuery = "UPDATE User_Account_Master SET balance = ? WHERE id = ?";
            try (PreparedStatement updateSenderStatement = connection.prepareStatement(updateSenderQuery)) {
                updateSenderStatement.setDouble(1, newSenderBalance);
                updateSenderStatement.setInt(2, senderId);
                updateSenderStatement.executeUpdate();
            }

            // Update recipient's balance
            double newRecipientBalance = recipientBalance + amount;
            String updateRecipientQuery = "UPDATE User_Account_Master SET balance = ? WHERE id = ?";
            try (PreparedStatement updateRecipientStatement = connection.prepareStatement(updateRecipientQuery)) {
                updateRecipientStatement.setDouble(1, newRecipientBalance);
                updateRecipientStatement.setInt(2, recipientId);
                updateRecipientStatement.executeUpdate();
            }

            // Create sender's transaction record
            String insertSenderQuery = "INSERT INTO Transaction_Master (user_id, transaction_type, amount, updated_balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertSenderStatement = connection.prepareStatement(insertSenderQuery)) {
                insertSenderStatement.setInt(1, senderId);
                insertSenderStatement.setString(2, "Transfer to " + recipientId);
                insertSenderStatement.setDouble(3, -amount);
                insertSenderStatement.setDouble(4, newSenderBalance);
                insertSenderStatement.executeUpdate();
            }

            // Create recipient's transaction record
            String insertRecipientQuery = "INSERT INTO Transaction_Master (user_id, transaction_type, amount, updated_balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertRecipientStatement = connection.prepareStatement(insertRecipientQuery)) {
                insertRecipientStatement.setInt(1, recipientId);
                insertRecipientStatement.setString(2, "Transfer from " + senderId);
                insertRecipientStatement.setDouble(3, amount);
                insertRecipientStatement.setDouble(4, newRecipientBalance);
                insertRecipientStatement.executeUpdate();
            }

            connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback the transaction on error
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit mode
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public double checkBalance(int userId) {
        String query = "SELECT balance FROM User_Account_Master WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1.0; // Return a negative value to indicate an error
    }

    public boolean deposit(int userId, double amount) {
        if (amount <= 0) {
            return false; // Reject 0 or negative deposit values
        }
        try {
            connection.setAutoCommit(false); // Start a transaction

            // Get the current balance of the user
            double currentBalance = checkBalance(userId);
            if (currentBalance < 0) {
                return false; // User not found or error occurred
            }

            // Update the balance
            double newBalance = currentBalance + amount;
            String updateQuery = "UPDATE User_Account_Master SET balance = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setDouble(1, newBalance);
                updateStatement.setInt(2, userId);
                updateStatement.executeUpdate();
            }

            // Create a transaction record
            String insertQuery = "INSERT INTO Transaction_Master (user_id, transaction_type, amount, updated_balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setInt(1, userId);
                insertStatement.setString(2, "Deposit");
                insertStatement.setDouble(3, amount);
                insertStatement.setDouble(4, newBalance);
                insertStatement.executeUpdate();
            }

            connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback the transaction on error
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit mode
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean withdraw(int userId, double amount) {
        try {
            connection.setAutoCommit(false); // Start a transaction

            // Get the current balance of the user
            double currentBalance = checkBalance(userId);
            if (currentBalance < 0) {
                return false; // User not found or error occurred
            }

            // Check if the withdrawal amount is valid
            if (amount <= 0 || amount > currentBalance) {
                return false; // Invalid amount
            }

            // Update the balance
            double newBalance = currentBalance - amount;
            String updateQuery = "UPDATE User_Account_Master SET balance = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setDouble(1, newBalance);
                updateStatement.setInt(2, userId);
                updateStatement.executeUpdate();
            }

            // Create a transaction record
            String insertQuery = "INSERT INTO Transaction_Master (user_id, transaction_type, amount, updated_balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setInt(1, userId);
                insertStatement.setString(2, "Withdraw");
                insertStatement.setDouble(3, amount);
                insertStatement.setDouble(4, newBalance);
                insertStatement.executeUpdate();
            }

            connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback the transaction on error
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit mode
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean createUser(String username, String password) {
        String insertQuery = "INSERT INTO User_Account_Master (username, password) VALUES (?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
            insertStatement.setString(1, username);
            insertStatement.setString(2, password);
            int rowsAffected = insertStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(int userId, String username, String password) {
        String updateQuery = "UPDATE User_Account_Master SET username = ?, password = ? WHERE id = ?";
        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setString(1, username);
            updateStatement.setString(2, password);
            updateStatement.setInt(3, userId);
            int rowsAffected = updateStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try {
            connection.setAutoCommit(false); // Start a transaction

            // Delete the user's account from User_Account_Master
            String deleteQuery = "DELETE FROM User_Account_Master WHERE id = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                deleteStatement.setInt(1, userId);
                int rowsAffected = deleteStatement.executeUpdate();
                if (rowsAffected <= 0) {
                    return false; // No rows were affected, user not found
                }
            }

            // Delete the user's transactions from Transaction_Master
            String deleteTransactionsQuery = "DELETE FROM Transaction_Master WHERE user_id = ?";
            try (PreparedStatement deleteTransactionsStatement = connection.prepareStatement(deleteTransactionsQuery)) {
                deleteTransactionsStatement.setInt(1, userId);
                deleteTransactionsStatement.executeUpdate();
            }

            connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback the transaction on error
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit mode
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    private void readConfig() {
        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader("bank_config.json")) {
            // Parse JSON data from the file
            JSONObject config = (JSONObject) parser.parse(reader);

            // Access values from JSON
            PORT_SOCKET = Integer.parseInt(config.get("PORT_SOCKET").toString());
            PORT_WEB = Integer.parseInt(config.get("PORT_WEB").toString());
            DB_NAME = (String) config.get("DB_NAME");
            DB_USERNAME = (String) config.get("DB_USERNAME");
            DB_PASSWORD = (String) config.get("DB_PASSWORD");
            DB_IP = (String) config.get("DB_IP");

            // Print the values
            System.out.println("PORT_SOCKET: " + PORT_SOCKET);
            System.out.println("PORT_WEB: " + PORT_WEB);
            System.out.println("DB_NAME: " + DB_NAME);
            System.out.println("DB_USERNAME: " + DB_USERNAME);
            System.out.println("DB_PASSWORD: " + DB_PASSWORD);
            System.out.println("DB_IP: " + DB_IP);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    public boolean handleCheckUserExists(int userId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM user_account_master WHERE id = ?")) {
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            return count > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}