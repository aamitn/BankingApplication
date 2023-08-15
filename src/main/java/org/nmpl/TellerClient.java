package org.nmpl;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TellerClient {
    private final String host = "localhost";
    private final int port = 9000;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private JFrame frame;
    private JButton depositButton;
    private JButton withdrawButton;
    private JButton balanceCheckButton;
    private JButton createAccountButton;
    private JButton updateAccountButton;
    private JButton deleteAccountButton;
    private JLabel connectionStatusLabel;
    private Timer connectionTimer;
    private JTextArea outputTextArea;

    public TellerClient() {
        initializeSocket();
        initializeUI();
        startConnectionTimer();
    }

    private void startConnectionTimer() {
        connectionTimer = new Timer(5000, e -> updateConnectionStatus());
        connectionTimer.start();
    }
    private void updateConnectionStatus() {
        boolean isConnected = socket != null && socket.isConnected();
        connectionStatusLabel.setText("Server: " + (isConnected ? "Connected" : "Disconnected"));
        frame.repaint();
    }

    private void initializeSocket() {
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performRefresh() {
        updateConnectionStatus(); // Update the connection status immediately
        frame.repaint(); // Repaint the UI to reflect the updated status
    }
    private void initializeUI() {
        frame = new JFrame("Bank Teller Client");
        frame.setLayout(new BorderLayout());

        depositButton = new JButton("Deposit");
        withdrawButton = new JButton("Withdraw");
        balanceCheckButton = new JButton("Balance Check");
        createAccountButton = new JButton("Create Account");
        updateAccountButton = new JButton("Update Account");
        deleteAccountButton = new JButton("Delete Account");

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Add the connection status label to the toolBar
        connectionStatusLabel = new JLabel("Server: Initializing");
        toolBar.add(connectionStatusLabel);


        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu aboutMenu = new JMenu("Help");

        JMenuItem refreshMenuItem = new JMenuItem("Refresh");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(refreshMenuItem);
        fileMenu.add(exitMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        JMenuItem faqMenuItem = new JMenuItem("FAQ");
        aboutMenu.add(aboutMenuItem);
        aboutMenu.add(faqMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(6, 1));

        buttonPanel.add(depositButton);
        buttonPanel.add(withdrawButton);
        buttonPanel.add(balanceCheckButton);
        buttonPanel.add(createAccountButton);
        buttonPanel.add(updateAccountButton);
        buttonPanel.add(deleteAccountButton);

        depositButton.addActionListener(e -> depositAction());
        withdrawButton.addActionListener(e -> withdrawAction());
        balanceCheckButton.addActionListener(e -> balanceCheckAction());
        createAccountButton.addActionListener(e -> createAccountAction());
        updateAccountButton.addActionListener(e -> updateAccountAction());
        deleteAccountButton.addActionListener(e -> deleteAccountAction());
        exitMenuItem.addActionListener(e -> {performExit();});
        refreshMenuItem.addActionListener(e -> performRefresh());
        faqMenuItem.addActionListener(e -> showFAQDialog());
        exitMenuItem.addActionListener(e -> performExit());
        aboutMenuItem.addActionListener(e ->showAboutDialog());

        // Create an output panel
        JPanel outputPanel = new JPanel();
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Information"));
        outputPanel.setLayout(new BorderLayout());

        outputTextArea = new JTextArea(10, 40);
        outputTextArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        outputPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(outputPanel, BorderLayout.SOUTH);

        frame.add(toolBar, BorderLayout.PAGE_START);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.setJMenuBar(menuBar);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setSize(800, 600);

        if (performAuthentication()) {
            // Enable banking operation buttons
            depositButton.setEnabled(true);
            withdrawButton.setEnabled(true);
            balanceCheckButton.setEnabled(true);
            createAccountButton.setEnabled(true);
            updateAccountButton.setEnabled(true);
            deleteAccountButton.setEnabled(true);
        }


    }

    private void showAboutDialog() {
        String aboutMessage = "Bank Teller Client\n\n"
                + "This application allows you to perform various banking operations.\n"
                + "Developed by [Your Name]\n"
                + "Version 1.0";

        JOptionPane.showMessageDialog(frame, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    private void showFAQDialog() {
        String faqMessage = "Frequently Asked Questions:\n\n"
                + "Q1: How do I perform a deposit?\n"
                + "A1: Click the 'Deposit' button and follow the prompts.\n\n"
                + "Q2: How do I check my account balance?\n"
                + "A2: Click the 'Balance Check' button and enter your user ID.\n\n"
                + "Q3: How do I create a new account?\n"
                + "A3: Click the 'Create Account' button and provide the required information.";

        JOptionPane.showMessageDialog(frame, faqMessage, "FAQ", JOptionPane.INFORMATION_MESSAGE);
    }


    private void depositAction() {
        updateOutputText("Deposit Invoked");
        String userIdInput = JOptionPane.showInputDialog(frame, "Enter user ID:");

        if (userIdInput == null || userIdInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid user ID.");
            return;
        }

        int userId = Integer.parseInt(userIdInput);

        if (!userExists(userId)) {
            JOptionPane.showMessageDialog(frame, "User with ID " + userId + " does not exist.");
            return;
        }

        String amountInput = JOptionPane.showInputDialog(frame, "Enter amount to deposit:");

        if (amountInput == null || amountInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
            return;
        }

        double amount = Double.parseDouble(amountInput);

        if (amount <= 0) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount greater than 0.");
            return;
        }

        output.println("deposit");
        output.println(userId);
        output.println(amount);

        try {
            String response = input.readLine();
            if (response != null) {
                JOptionPane.showMessageDialog(frame, response);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to deposit.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error while communicating with the server.");
        }
    }

    private void balanceCheckAction() {
        updateOutputText("BalanceCheck Invoked");
        String userIdInput = JOptionPane.showInputDialog(frame, "Enter user ID:");

        if (userIdInput != null && !userIdInput.trim().isEmpty()) {
            int userId = Integer.parseInt(userIdInput);

            if (userExists(userId)) {
                output.println("balanceCheck");
                output.println(userId);

                try {
                    double balance = Double.parseDouble(input.readLine());
                    JOptionPane.showMessageDialog(frame, "Balance: " + balance);
                    updateOutputText("Balance for UserID: "+userIdInput+" is: "+balance);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to retrieve balance.");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "User with ID " + userId + " does not exist.");
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter a valid user ID.");
        }
    }

    private void withdrawAction() {
        String userIdInput = JOptionPane.showInputDialog(frame, "Enter user ID:");

        if (userIdInput == null || userIdInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid user ID.");
            return;
        }

        int userId = Integer.parseInt(userIdInput);

        if (!userExists(userId)) {
            JOptionPane.showMessageDialog(frame, "User with ID " + userId + " does not exist.");
            return;
        }

        String amountInput = JOptionPane.showInputDialog(frame, "Enter amount to withdraw:");

        if (amountInput == null || amountInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
            return;
        }

        double amount = Double.parseDouble(amountInput);

        if (amount <= 0) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount greater than 0.");
            return;
        }

        output.println("withdraw");
        output.println(userId);
        output.println(amount);

        try {
            String response = input.readLine();
            if (response != null) {
                JOptionPane.showMessageDialog(frame, response);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to withdraw.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAccountAction() {
        String username = JOptionPane.showInputDialog(frame, "Enter username:");
        String password = JOptionPane.showInputDialog(frame, "Enter password:");

        if (username != null && !username.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
            output.println("createAccount");
            output.println(username);
            output.println(password);

            try {
                String response = input.readLine();
                if (response != null) {
                    JOptionPane.showMessageDialog(frame, response);
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to create account.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Username and password cannot be blank.");
        }
    }

    private void updateAccountAction() {
        String userId = JOptionPane.showInputDialog(frame, "Enter user ID:");
        String newUsername = JOptionPane.showInputDialog(frame, "Enter new username:");
        String newPassword = JOptionPane.showInputDialog(frame, "Enter new password:");

        output.println("updateAccount");
        output.println(userId);
        output.println(newUsername);
        output.println(newPassword);

        try {
            String response = input.readLine();
            if (response != null) {
                JOptionPane.showMessageDialog(frame, response);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to update account.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteAccountAction() {
        String userId = JOptionPane.showInputDialog(frame, "Enter user ID:");

        output.println("deleteAccount");
        output.println(userId);

        try {
            String response = input.readLine();
            if (response != null) {
                JOptionPane.showMessageDialog(frame, response);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to delete account.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean userExists(int userId) {
        output.println("checkUserExists");
        output.println(userId);

        try {
            return Boolean.parseBoolean(input.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void performExit() {
        // Clean up resources, close connections, etc. if needed
        if (socket != null) {
            try {
                socket.close();
                closeClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0); // Exit the application
    }

    private boolean performAuthentication() {
        updateOutputText("Authentication Invoked");
        boolean isAuthenticated = false;

        while (!isAuthenticated) {
            String username = JOptionPane.showInputDialog(frame, "Enter teller name:");
            String password = JOptionPane.showInputDialog(frame, "Enter teller password:");

            output.println("authenticate");
            output.println(username);
            output.println(password);

            try {
                String response = input.readLine();
                if (response != null) {
                    JOptionPane.showMessageDialog(frame, response);
                    isAuthenticated = response.equals("Authentication successful.");
                    updateOutputText(response);
                    if (!isAuthenticated) {
                        int retryOption = JOptionPane.showConfirmDialog(
                                frame,
                                "Authentication failed. Do you want to retry?",
                                "Authentication Failed",
                                JOptionPane.YES_NO_OPTION
                        );

                        if (retryOption != JOptionPane.YES_OPTION) {
                            performExit(); // Exit the program
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to authenticate.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error while communicating with the server.");
            }
        }
        return isAuthenticated;
    }
    private void updateOutputText(String text) {
        SwingUtilities.invokeLater(() -> {
            outputTextArea.append(text + "\n");
            outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
        });
    }

    public void closeClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectionTimer.stop();
    }

}
