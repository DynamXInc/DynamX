package fr.dynamx.server.network.udp;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.server.network.DynamXServerNetworkSystem;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class ServerIPAdressRetriever {
    private String externalAddress;

    public String getAddress()
    {
        return this.externalAddress;
    }

    public String[] getPlayerIPs() {
        List players = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().playerEntities;
        String[] ips = new String[players.size()];

        for (int i = 0; i < players.size(); ++i) {
            EntityPlayerMP p = (EntityPlayerMP)players.get(i);
            ips[i] = p.getPlayerIP();
        }
        return ips;
    }

    public void init() {
        if (DynamXConfig.usingProxy) {
            (new Thread(() -> ServerIPAdressRetriever.this.externalAddress = ServerIPAdressRetriever.this.retrieveExternalAddress(), "Extrernal Address Retriver Process")).start();
        }
    }

    private String retrieveExternalAddress() {
        DynamXMain.log.info("Retrieving server address.");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "0.0.0.0";
        }
    }
}