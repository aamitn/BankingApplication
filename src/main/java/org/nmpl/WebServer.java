package org.nmpl;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

public class WebServer extends NanoHTTPD {
    private final BackendServer backendServer;
    private final boolean isLoggedIn = false;

    public WebServer(int port, BackendServer backendServer) {
        super(port);
        this.backendServer = backendServer;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/")) {
            // Home Page with login link
            String response = "<html><body><h1>Welcome to the Server</h1>" +
                    (isLoggedIn ? "<p><a href=\"/balance\">Check Balance</a></p>" : "<p><a href=\"/login\">Login</a></p>") +
                    "</body></html>";
            Response res = newFixedLengthResponse(response);
            return res;
        } else if (uri.equals("/login")) {
            // Show login form
            String loginForm = "<html><body><h1>Login</h1>" +
                    "<form action=\"/checklogin\" method=\"POST\">" +
                    "Username: <input type=\"text\" name=\"username\"><br>" +
                    "Password: <input type=\"password\" name=\"password\"><br>" +
                    "<input type=\"submit\" value=\"Login\">" +
                    "</form></body></html>";
            return newFixedLengthResponse(loginForm);
        }else if (uri.equals("/checklogin")) {
            // Handle login form submission
            if (Method.POST.equals(session.getMethod())) {
                try {
                    // Parse the form data
                    session.parseBody(null);
                    Map<String, String> params = session.getParms();
                    String username = params.get("username");
                    String password = params.get("password");

                    // Check user credentials with backendServer
                    if (backendServer.authenticateUser(username, password)) {
                        String balanceForm = "<html><body><h1>Welcome " + session.getParms().get("username") + "!</h1>" +
                                "<p>Your balance is: $" + backendServer.checkBalance(1) + "</p>" +
                                "<h2>Transfer Money</h2>" +
                                "<form action=\"/transfer\" method=\"POST\">" +
                                "Recipient Account ID: <input type=\"number\" name=\"recipientId\"><br>" +
                                "Amount: <input type=\"number\" step=\"0.01\" name=\"amount\"><br>" +
                                "<input type=\"submit\" value=\"Transfer\">" +
                                "</form></body></html>";
                        return newFixedLengthResponse(balanceForm);
                    } else {
                        // Invalid login
                        String loginPage = "<html><body><h1>Invalid Credentials</h1><p>Please check your username and password and <a href=\"/login\">try again</a>.</p></body></html>";
                        return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_HTML, loginPage);

                    }
                } catch (IOException | ResponseException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error processing form data.");
                }
            }
        }  else if (uri.equals("/transfer")) {
            if (isLoggedIn && Method.POST.equals(session.getMethod())) {
                try {
                    // Parse the form data
                    session.parseBody(null);
                    Map<String, String> params = session.getParms();
                    String recipientIdStr = params.get("recipientId");
                    String amountStr = params.get("amount");

                    int recipientId = Integer.parseInt(recipientIdStr);
                    double amount = Double.parseDouble(amountStr);

                    // Perform money transfer
                    boolean success = backendServer.transferMoney(1, recipientId, amount);
                    String response;

                    if (success) {
                        response = "<html><body><h1>Transfer Successful</h1></body></html>";
                    } else {
                        response = "<html><body><h1>Transfer Failed</h1><p>Invalid recipient account ID or insufficient balance.</p></body></html>";
                    }

                    return newFixedLengthResponse(response);
                } catch (IOException | ResponseException | NumberFormatException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error processing form data.");
                }
            }
        }

        else {
            // Return a 404 response for other paths
            String response = "Resource not found.";
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, response);
        }
        return null;
    }
}
