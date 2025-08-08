package fr.dynamx.common.command;

import fr.dynamx.utils.DynamXConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.List;

public class CmdTerrainDebug implements ISubCommand {
    private final boolean isClient;

    public CmdTerrainDebug(boolean isClient) {
        this.isClient = isClient;
    }

    private String prefix() {
        if(isClient) {
            return TextFormatting.GOLD + "[DynamX-Client] " + TextFormatting.RESET;
        }
        return TextFormatting.GREEN + "[DynamX-Server] " + TextFormatting.RESET;
    }

    @Override
    public String getName() {
        return "terrain_debug";
    }

    @Override
    public String getUsage() {
        return "terrain_debug [false|true] - enables terrain debug in console. Has a performance impact.";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2) {
            boolean enableDebug;
            try {
                enableDebug = CommandBase.parseBoolean(args[1]);
            } catch (CommandException e) {
                throw new WrongUsageException(getUsage());
            }
            DynamXConfig.enableDebugTerrainManager = enableDebug;
            sender.sendMessage(new TextComponentString(prefix() + (enableDebug ? "Enabled" : "Disabled") + " terrain debug. Enabling it can have some performance impact. This configuration will not persist after game restart."));
        } else if (args.length == 1) {
            sender.sendMessage(new TextComponentString(prefix() + "Terrain debug is " + (DynamXConfig.enableDebugTerrainManager ? "enabled" : "disabled")));
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "false", "true"));
        }
    }
}
