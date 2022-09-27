package fr.dynamx.server.command;

import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CmdReloadConfig implements ISubCommand {
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
        DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.SERVER_RUNNING, DynamXLoadingTasks.PACK).thenAccept(empty -> {
            //TODO TRANSLATE
            sender.sendMessage(new TextComponentString("Packs reloaded"));
            if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS__ERRORS))
                sender.sendMessage(new TextComponentString(TextFormatting.RED + " Some packs have errors, use the debug menu to see them"));
            //sender.sendMessage(new TextComponentString(TextFormatting.RED + " Certains packs ont des erreurs, utilisez le menu de debug pour les voir"));
        });
    }
}
