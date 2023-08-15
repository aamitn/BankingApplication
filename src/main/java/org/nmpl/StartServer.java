package org.nmpl;

public class StartServer {
    public StartServer()
    {
        BackendServer server = new BackendServer();
        server.start();
    }
}
