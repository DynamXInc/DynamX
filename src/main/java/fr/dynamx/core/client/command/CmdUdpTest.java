package fr.dynamx.core.client.command;

import fr.dynamx.core.client.network.ClientPhysicsSyncManager;
import fr.dynamx.core.client.network.udp.UdpClientNetworkHandler;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.command.ISubCommand;
import fr.dynamx.core.common.network.packets.MessagePing;
import fr.dynamx.core.common.network.udp.UdpTestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CmdUdpTest implements ISubCommand {
    private Thread tester;
    private int packetsPerTick;
    private int remainingPackets;
    private int remainingTicks;
    private int packetId;
    public static boolean[] received;
    public static long startTime = 0;

    private final Runnable TESTER = new Runnable() {
        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            while (remainingPackets > 0) {
                remainingTicks--;
                remainingPackets -= packetsPerTick;
                for (int i = 0; i < packetsPerTick; i++) {
                    ((UdpClientNetworkHandler) DynamXContext.getNetwork().getQuickNetwork()).sendPacket(new UdpTestPacket(packetId, System.currentTimeMillis()));
                    packetId++;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int failCount = 0;
            for (boolean b : received) {
                if (!b)
                    failCount++;
            }
            if (Minecraft.getMinecraft().player != null)
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("UDP test finished over " + packetId + " sent packets with " + failCount + " loss"));
            else {
                System.out.println("UDP test finished over " + packetId + " sent packets with " + failCount + " loss");
            }
        }
    };

    @Override
    public String getName() {
        return "udp_test";
    }

    @Override
    public String getUsage() {
        return "/client_dynamx udp_test <ping/test> [totalpackets] [packetspertick]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 4 && args[1].matches("test")) {
            if (tester != null && tester.isAlive()) {
                sender.sendMessage(new TextComponentString("Already testing udp !"));
            } else if (!(DynamXContext.getNetwork().getQuickNetwork() instanceof UdpClientNetworkHandler)) {
                sender.sendMessage(new TextComponentString("UDP isn't started !"));
            } else {
                remainingPackets = CommandBase.parseInt(args[2], 1, 100000);
                packetsPerTick = CommandBase.parseInt(args[3], 1, 100);
                remainingTicks = remainingPackets / packetsPerTick;
                received = new boolean[remainingPackets];
                packetId = 0;
                tester = new Thread(TESTER);
                tester.setName("UDP tester " + sender.getCommandSenderEntity().ticksExisted);
                sender.sendMessage(new TextComponentString("UDP is in test, " + remainingTicks + " ticks remaining..."));
                tester.start();
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("ping")) {
            sender.sendMessage(new TextComponentString("[DynamX] Pinging..."));
            ClientPhysicsSyncManager.pingMs = -3;
            DynamXContext.getNetwork().sendToServer(new MessagePing(System.currentTimeMillis(), true));
        } else {
            throw new WrongUsageException(getUsage());
        }
    }
}
