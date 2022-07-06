package fr.dynamx.client.network;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessagePing;
import net.minecraft.client.Minecraft;

public class ClientPhysicsSyncManager
{
    public static int simulationTime;
    public static int pingMs = -1;
    public static long lastPing;

    public static void tick() {
        if(!Minecraft.getMinecraft().isSingleplayer()) {
            if (System.currentTimeMillis() - lastPing > 10000) {
                pingMs = -2;
                lastPing = System.currentTimeMillis();
                DynamXContext.getNetwork().sendToServer(new MessagePing(lastPing, false));
            }
            ClientPhysicsSyncManager.simulationTime++;
        }
    }

    public static String getPingMessage()
    {
        return pingMs >= 80 ? "Warning : you have a bad connection ! PING is "+ pingMs+" ms" : pingMs == -2 ? "Pinging..." : "";
    }
    public static boolean hasBadConnection()
    {
        return pingMs >= 80 || pingMs == -2;
    }
}
