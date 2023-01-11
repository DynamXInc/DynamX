package fr.dynamx.server.command;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.entities.vehicles.*;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CmdKillEntities implements ISubCommand {
    @Override
    public String getName() {
        return "kill";
    }

    @Override
    public String getUsage() {
        return getName() + " <all|cars|boat|helicopter|props|ragdolls|trailers|doors>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 1) {
            List<Entity> entityList;
            if (args[1].equalsIgnoreCase("cars")) {
                entityList = sender.getEntityWorld().getEntities(CarEntity.class, EntitySelectors.IS_ALIVE);
            } else if (args[1].equalsIgnoreCase("boat")) {
                entityList = sender.getEntityWorld().getEntities(BoatEntity.class, EntitySelectors.IS_ALIVE);
            }else if (args[1].equalsIgnoreCase("helicopter")) {
                entityList = sender.getEntityWorld().getEntities(HelicopterEntity.class, EntitySelectors.IS_ALIVE);
            }else if (args[1].equalsIgnoreCase("props")) {
                entityList = sender.getEntityWorld().getEntities(PropsEntity.class, EntitySelectors.IS_ALIVE);
            } else if (args[1].equalsIgnoreCase("ragdolls")) {
                entityList = sender.getEntityWorld().getEntities(RagdollEntity.class, EntitySelectors.IS_ALIVE);
            } else if (args[1].equalsIgnoreCase("trailers")) {
                entityList = sender.getEntityWorld().getEntities(TrailerEntity.class, EntitySelectors.IS_ALIVE);
            } else if (args[1].equalsIgnoreCase("doors")) {
                entityList = sender.getEntityWorld().getEntities(DoorEntity.class, EntitySelectors.IS_ALIVE);
            } else if (args[1].equalsIgnoreCase("all")) {
                entityList = new ArrayList<>();
                entityList.addAll(sender.getEntityWorld().getEntities(PhysicsEntity.class, EntitySelectors.IS_ALIVE));
            } else {
                throw new WrongUsageException(getUsage());
            }
            killEntities(entityList, sender, args[1]);
        } else {
            throw new WrongUsageException(getUsage());
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length > 1) {
            r.add("all");
            r.add("cars");
            r.add("boat");
            r.add("helicopter");
            r.add("props");
            r.add("ragdolls");
            r.add("doors");
        }
    }

    private void killEntities(List<Entity> entityList, ICommandSender sender, String name) {
        if (entityList.isEmpty()) {
            sender.sendMessage(new TextComponentString("§cNo " + name + " found"));
            return;
        }
        entityList.forEach(Entity::setDead);
        if(name.equals("all")) {
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "" + entityList.size() + " " + TextFormatting.GREEN +  "entities have been killed."));
            return;
        }
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "" + entityList.size() + " " + TextFormatting.GREEN + name + " have been killed."));
    }
}
