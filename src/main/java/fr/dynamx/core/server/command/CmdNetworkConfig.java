package fr.dynamx.core.server.command;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.command.ISubCommand;
import fr.dynamx.core.common.network.packets.MessageSyncConfig;
import fr.dynamx.core.common.network.sync.variables.EntityPosVariable;
import fr.dynamx.core.server.network.PlayerSyncBuffer;
import fr.dynamx.forge.DynamXConfig;
import fr.dynamx.core.utils.debug.SyncHelper;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

import static java.lang.Float.parseFloat;
import static net.minecraft.command.CommandBase.parseInt;

public class CmdNetworkConfig implements ISubCommand {
    @Override
    public String getName() {
        return "network_config";
    }

    @Override
    public String getUsage() {
        return getName() + " <resync_params|sync_params|sync_delay|epsilon|resyncId>";
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.add("resync_params");
            r.add("sync_buff");
            r.add("sync_delay");
            r.add("epsilon");
            r.add("resyncId");
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length - 1 >= 0) System.arraycopy(args, 1, args, 0, args.length - 1);
        if (args[0].equals("resyncId") && sender instanceof EntityPlayer) {
            System.out.println("Resyncing id for " + sender);
            DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, ((EntityPlayer) sender).getEntityId()), EnumPacketTarget.PLAYER, (EntityPlayerMP) sender);
            sender.sendMessage(new TextComponentString("Resynced id for " + sender));
        } else if (args[0].equalsIgnoreCase("resync_params")) {
            if (args.length != 4) {
                throw new WrongUsageException("resync_params args are ([name=default_value]): [hard_set_radius=3] [kick_player_radius=400] [resync_radius=50]");
            }
            EntityPosVariable.CRITIC1 = parseInt(args[1]);
            EntityPosVariable.CRITIC2 = parseInt(args[2]);
            EntityPosVariable.CRITIC3 = parseInt(args[3]);
            sender.sendMessage(new TextComponentString("resync_params are " + EntityPosVariable.CRITIC1 + " " + EntityPosVariable.CRITIC2 + " " + EntityPosVariable.CRITIC3));
        } else if (args[0].equalsIgnoreCase("sync_params")) {
            if (args.length != 7) {
                throw new WrongUsageException("sync_params args are ([name=default_value]): [new_sends_limit=20] [delayed_sends_limit=10] [safe_radius=21] [safe_radius2=39] [max_skip=4] [entities_per_packet=10]");
            }
            PlayerSyncBuffer.NEW_SENDS_LIMIT = parseInt(args[1]);
            PlayerSyncBuffer.DELAYED_SENDS_LIMIT = parseInt(args[2]);
            PlayerSyncBuffer.FIRST_RADIUS = parseInt(args[3]);
            PlayerSyncBuffer.FIRST_RADIUS *= PlayerSyncBuffer.FIRST_RADIUS; //square it
            PlayerSyncBuffer.SECOND_RADIUS = parseInt(args[4]);
            PlayerSyncBuffer.SECOND_RADIUS *= PlayerSyncBuffer.SECOND_RADIUS; //square it
            PlayerSyncBuffer.MAX_SKIP = parseInt(args[5]);
            PlayerSyncBuffer.ENTITIES_PER_PACKETS = parseInt(args[6]);
            sender.sendMessage(new TextComponentString("sync_params are " + PlayerSyncBuffer.NEW_SENDS_LIMIT + " " + PlayerSyncBuffer.DELAYED_SENDS_LIMIT + " " + PlayerSyncBuffer.FIRST_RADIUS + " " + PlayerSyncBuffer.SECOND_RADIUS + " " + PlayerSyncBuffer.MAX_SKIP + " " + PlayerSyncBuffer.ENTITIES_PER_PACKETS));
        } else if (args[0].equalsIgnoreCase("sync_delay")) {
            DynamXConfig.mountedVehiclesSyncTickRate = parseInt(args[1]);
            if (server.isDedicatedServer()) {
                DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(false, -1), EnumPacketTarget.ALL);
            }
            server.getPlayerList().sendMessage(new TextComponentString("Changed sync delay to " + DynamXConfig.mountedVehiclesSyncTickRate));
        } else if (args[0].equalsIgnoreCase("epsilon")) {
            SyncHelper.EPS = parseFloat(args[1]);
            sender.sendMessage(new TextComponentString("Sync epsilon is now " + SyncHelper.EPS));
        } else {
            throw new WrongUsageException(getRootCommandUsage() + getUsage());
        }
    }
}
