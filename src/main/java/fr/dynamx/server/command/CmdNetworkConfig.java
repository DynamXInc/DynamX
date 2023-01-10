package fr.dynamx.server.command;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import fr.dynamx.common.network.sync.variables.EntityPosVariable;
import fr.dynamx.server.network.PlayerSyncBuffer;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.debug.SyncTracker;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

import static java.lang.Float.parseFloat;
import static net.minecraft.command.CommandBase.parseInt;

public class CmdNetworkConfig implements ISubCommand {
    public static boolean sync_buff;
    public static boolean TRACK_SYNC;
    //public static boolean SERVER_INTERPOL;
    public static int SERVER_NET_DEBUG;
    public static float SMOOTHY;

    @Override
    public String getName() {
        return "network_config";
    }

    @Override
    public String getUsage() {
        return getName() + " <doTrackSync|syncCrit|sync_buff|syncDelay|SMOOTHY|epsilon|printNetDebug> - for Aym'";
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            //r.add("doServerInterpol");
            r.add("doTrackSync");
            r.add("syncCrit");
            r.add("sync_buff");
            r.add("syncDelay");
            r.add("SMOOTHY");
            r.add("epsilon");
            r.add("printNetDebug");
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length - 1 >= 0) System.arraycopy(args, 1, args, 0, args.length - 1);
        if (args[0].equalsIgnoreCase("doTrackSync")) {
            TRACK_SYNC = !TRACK_SYNC;
            sender.sendMessage(new TextComponentString("TRACK_SYNC is " + TRACK_SYNC));
        } else if (args[0].equalsIgnoreCase("syncCrit")) {
            if (args.length != 4)
                throw new WrongUsageException("To be used by aym");
            EntityPosVariable.CRITIC1 = parseInt(args[1]);
            EntityPosVariable.CRITIC2 = parseInt(args[2]);
            EntityPosVariable.CRITIC3 = parseInt(args[3]);
            sender.sendMessage(new TextComponentString("SyncCrit are " + EntityPosVariable.CRITIC1 + " " + EntityPosVariable.CRITIC2 + " " + EntityPosVariable.CRITIC3));
        } else if (args[0].equalsIgnoreCase("sync_buff") && args.length == 7) {
            PlayerSyncBuffer.NEW_SENDS_LIMIT = parseInt(args[1]);
            PlayerSyncBuffer.DELAYED_SENDS_LIMIT = parseInt(args[2]);
            PlayerSyncBuffer.FIRST_RADIUS = parseInt(args[3]);
            PlayerSyncBuffer.FIRST_RADIUS *= PlayerSyncBuffer.FIRST_RADIUS; //square it
            PlayerSyncBuffer.SECOND_RADIUS = parseInt(args[4]);
            PlayerSyncBuffer.SECOND_RADIUS *= PlayerSyncBuffer.SECOND_RADIUS; //square it
            PlayerSyncBuffer.MAX_SKIP = parseInt(args[5]);
            PlayerSyncBuffer.ENTITIES_PER_PACKETS = parseInt(args[6]);
            sender.sendMessage(new TextComponentString("sync_buff is " + PlayerSyncBuffer.NEW_SENDS_LIMIT + " " + PlayerSyncBuffer.DELAYED_SENDS_LIMIT + " " + PlayerSyncBuffer.FIRST_RADIUS + " " + PlayerSyncBuffer.SECOND_RADIUS + " " + PlayerSyncBuffer.MAX_SKIP + " " + PlayerSyncBuffer.ENTITIES_PER_PACKETS));
        } else if (args[0].equalsIgnoreCase("sync_buff")) {
            sync_buff = !sync_buff;
            sender.sendMessage(new TextComponentString("sync_buff is " + sync_buff + " [limit] [limit2] [safe_radius] [safe_radius2] [max_skip] [entity per packet]"));
        } else if (args[0].equalsIgnoreCase("syncDelay")) {
            DynamXConfig.mountedVehiclesSyncTickRate = parseInt(args[1]);
            if (server.isDedicatedServer()) {
                DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode(Side.CLIENT)), EnumPacketTarget.ALL);
            }
            server.getPlayerList().sendMessage(new TextComponentString("Changed sync delay to " + DynamXConfig.mountedVehiclesSyncTickRate));
        } else if (args[0].equalsIgnoreCase("SMOOTHY")) {
            SMOOTHY = parseFloat(args[1]);
            sender.sendMessage(new TextComponentString("SMOOTHY is " + SMOOTHY));
        } else if (args[0].equalsIgnoreCase("epsilon")) {
            SyncTracker.EPS = parseFloat(args[1]);
            sender.sendMessage(new TextComponentString("Sync epsilon is now " + SyncTracker.EPS));
        } else if (args[0].equalsIgnoreCase("printNetDebug")) {
            CmdNetworkConfig.SERVER_NET_DEBUG++;
            if (CmdNetworkConfig.SERVER_NET_DEBUG > 2)
                CmdNetworkConfig.SERVER_NET_DEBUG = 0;
            sender.sendMessage(new TextComponentString("SERVER_NET_DEBUG is " + CmdNetworkConfig.SERVER_NET_DEBUG + ", may be laggy"));
        } else
            throw new WrongUsageException(getRootCommandUsage() + getUsage());
    }
}
