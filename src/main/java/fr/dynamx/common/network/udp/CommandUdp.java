package fr.dynamx.common.network.udp;

import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.client.network.udp.UdpClientNetworkHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessagePing;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import static fr.dynamx.client.handlers.ClientDebugSystem.MOVE_DEBUG;

public class CommandUdp extends CommandBase {
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
        return "udpdynamx";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "udpdynamx <ping/test> [totalpackets] [packetspertick]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 3 && args[0].matches("test")) {
            if (tester != null && tester.isAlive())
                sender.sendMessage(new TextComponentString("Already testing udp !"));
            else if (!(DynamXContext.getNetwork().getQuickNetwork() instanceof UdpClientNetworkHandler))
                sender.sendMessage(new TextComponentString("UDP isn't started !"));
            else {
                remainingPackets = parseInt(args[1], 1, 100000);
                packetsPerTick = parseInt(args[2], 1, 100);
                remainingTicks = remainingPackets / packetsPerTick;
                received = new boolean[remainingPackets];
                packetId = 0;
                tester = new Thread(TESTER);
                tester.setName("UDP tester " + sender.getCommandSenderEntity().ticksExisted);
                sender.sendMessage(new TextComponentString("UDP is in test, " + remainingTicks + " ticks remaining..."));
                tester.start();
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("ping")) {
            sender.sendMessage(new TextComponentString("[DynamX] Pinging..."));
            ClientPhysicsSyncManager.pingMs = -3;
            DynamXContext.getNetwork().sendToServer(new MessagePing(System.currentTimeMillis(), true));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("move_debug")) {
            MOVE_DEBUG++;
            if (MOVE_DEBUG > 2)
                MOVE_DEBUG = 0;
            sender.sendMessage(new TextComponentString("Move debug is " + MOVE_DEBUG));
        } else
            throw new WrongUsageException(getUsage(sender));
    }
}
