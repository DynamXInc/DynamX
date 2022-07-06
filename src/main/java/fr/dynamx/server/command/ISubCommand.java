package fr.dynamx.server.command;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

/**
 * /dynamx sub commands helper
 */
public interface ISubCommand
{
    String getName();
    String getUsage();
    void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
    default void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {}
    default String getPermission() {
        return DynamXConstants.ID+".command."+getName();
    }
    default String getRootCommandUsage() { return "/dynamx "; }
}
