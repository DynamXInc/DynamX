package fr.dynamx.core.server.network.udp;

import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.forge.DynamXConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class ServerIPAdressRetriever {
    private String externalAddress;

    public String getAddress() {
        return this.externalAddress;
    }

    public void init() {
        if (DynamXConfig.usingProxy) {
            (new Thread(() -> ServerIPAdressRetriever.this.externalAddress = ServerIPAdressRetriever.this.retrieveExternalAddress(), "Extrernal Address Retriver Process")).start();
        }
    }

    private String retrieveExternalAddress() {
        DynamXMain.log.info("Retrieving server IP address.");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "0.0.0.0";
        }
    }
}