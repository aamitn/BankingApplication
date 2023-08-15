package org.nmpl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class StartApp {

    private static final Logger logger = Logger.getLogger(StartApp.class.getName());

    public static void main(String[] args) {
        // Configure the logger
        configureLogger();

        Thread serverThread = new Thread(() -> {
            logInfo("Starting server...");
            new StartServer();
            logInfo("Server started.");
        }, "ServerThread");

        Thread clientThread = new Thread(() -> {
            logInfo("Starting client...");
            new StartClient();
            logInfo("Client started.");
        }, "ClientThread");

        serverThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logSevere("Interrupted during sleep", e);
            System.out.println("FATAL ERROR");
        }
        clientThread.start();
    }

    private static void configureLogger() {
        try {
            // Create a custom formatter for the logs
            Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String timestamp = dateFormat.format(new Date(record.getMillis()));
                    String threadName = Thread.currentThread().getName();
                    String logLevel = record.getLevel().getName();
                    return "[" + timestamp + "] [" + threadName + "] [" + logLevel + "] [app] " + record.getMessage() + "\n";
                }
            };

            // Create a file handler for saving logs to a text file
            FileHandler fileHandler = new FileHandler("app_logs.log");
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);

            // Add the file handler to the logger
            logger.addHandler(fileHandler);

            // Set the logger level to ALL
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to configure logger", e);
        }
    }

    private static void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    private static void logSevere(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
