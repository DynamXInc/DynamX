package fr.dynamx.server.command;

import fr.dynamx.common.command.CmdChunkControl;
import fr.dynamx.common.command.CmdOpenDebugGui;
import fr.dynamx.common.command.CmdTerrainDebug;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamXServerCommands extends CommandBase {
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public DynamXServerCommands() {
        addCommand(new CmdSlopes());
        addCommand(new CmdReloadConfig());
        addCommand(new CmdRefreshChunks());
        addCommand(new CmdNetworkConfig());
        addCommand(new CmdChunkControl());
        addCommand(new CmdSpawnObjects());
        addCommand(new CmdKillEntities());
        addCommand(new CmdOpenDebugGui());
        addCommand(new CmdSpawnRagdoll());
        addCommand(new CmdShockWave());
        addCommand(new CmdTerrainDebug());
        addCommand(new CmdPhysicsMode());
    }

    public void addCommand(ISubCommand command) {
        commands.put(command.getName(), command);
        PermissionAPI.registerNode(command.getPermission(), DefaultPermissionLevel.OP, "/dynamx " + command.getUsage());
    }

    @Override
    public String getName() {
        return "dynamx";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        StringBuilder usage = new StringBuilder();
        commands.keySet().forEach(s -> usage.append("|").append(s));
        return "/dynamx <" + usage.substring(1) + ">";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && commands.containsKey(args[0])) {
            ISubCommand command = commands.get(args[0]);
            if (!(sender instanceof EntityPlayer) || PermissionAPI.hasPermission((EntityPlayer) sender, command.getPermission())) {
                command.execute(server, sender, args);
            } else {
                throw new CommandException("You don't have permission to use this command !");
            }
        } else
            throw new WrongUsageException(this.getUsage(sender));
    }

    public EntityPlayerMP getPlayer(String name) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(name);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> r = new ArrayList<String>();
        if (args.length == 1) {
            r.addAll(commands.keySet());
        } else if (args.length > 1 && commands.containsKey(args[0])) {
            commands.get(args[0]).getTabCompletions(server, sender, args, targetPos, r);
        }
        return getListOfStringsMatchingLastWord(args, r);
    }
}
