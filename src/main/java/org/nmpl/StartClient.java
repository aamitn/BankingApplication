package org.nmpl;

import javax.swing.*;

public class StartClient {

    public  StartClient()
    {
        SwingUtilities.invokeLater(() -> {
            TellerClient client = new TellerClient();
        });
    }
}
