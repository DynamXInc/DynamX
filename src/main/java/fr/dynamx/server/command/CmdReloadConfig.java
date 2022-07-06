package fr.dynamx.server.command;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CmdReloadConfig implements ISubCommand
{
    @Override
    public String getName() {
        return "reload_config";
    }

    @Override
    public String getUsage() {
        return getName();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        ITextComponent msg = new TextComponentString("/!\\ Reloading DynamX packs, you may lag");
        msg.getStyle().setColor(TextFormatting.GOLD);
        server.getPlayerList().sendMessage(msg);
        DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.SERVER_RUNNING, () -> {
            sender.sendMessage(new TextComponentString("Packs reloaded"));
            if (DynamXContext.getErrorTracker().hasErrors(DynamXLoadingTasks.INIT, DynamXLoadingTasks.PACK)) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED+" Certains packs ont des erreurs, utilisez le menu de debug pour les voir"));
            }
        }, DynamXLoadingTasks.PACK);
    }
}
