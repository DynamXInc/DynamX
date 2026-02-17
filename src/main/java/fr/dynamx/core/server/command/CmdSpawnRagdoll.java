package fr.dynamx.core.server.command;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.command.ISubCommand;
import fr.dynamx.core.common.entities.RagdollEntity;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.List;

public class CmdSpawnRagdoll implements ISubCommand {
    @Override
    public String getName() {
        return "spawn_ragdoll";
    }

    @Override
    public String getUsage() {
        return "spawn_ragdoll [help|player] [life_expectancy] [velocity_x] [velocity_y] [velocity_z]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2 && args[1].equalsIgnoreCase("help")) {
            sender.sendMessage(new TextComponentString("Usage: /dynamx " + getUsage()));
            sender.sendMessage(new TextComponentString("Transforms the given 'player' in a ragdoll, during 'life_expectancy' ticks (or -1 for eternity). " +
                    "You can add a velocity to the ragdoll, the default value is 0, 0, 0."));
            return;
        }
        EntityPlayerMP player;
        if (args.length >= 2)
            player = CommandBase.getPlayer(server, sender, args[1]);
        else if (sender instanceof EntityPlayerMP)
            player = (EntityPlayerMP) sender;
        else
            throw new WrongUsageException("You're not a player !");
        JmeVector3fPool.openPool();
        int life = -1;
        if (args.length >= 3)
            life = CommandBase.parseInt(args[2]);
        RagdollEntity ragdollEntity = new RagdollEntity(player.world, DynamXUtils.toVector3f(player.getPositionVector().add(0, player.getDefaultEyeHeight(), 0)),
                player.rotationYaw + 180, player.getName(), (short) life, player);
        Vector3f velocity = new Vector3f(0, 0, 0); //should be permanent
        if (args.length == 6)
            velocity.set((float) CommandBase.parseDouble(args[3]), (float) CommandBase.parseDouble(args[4]), (float) CommandBase.parseDouble(args[5]));
        ragdollEntity.setPhysicsInitCallback((a, b) -> {
            if (b != null && b.getCollisionObject() != null) {
                ((PhysicsRigidBody) b.getCollisionObject()).setLinearVelocity(velocity);
            }
        });
        player.setInvisible(true);
        player.world.spawnEntity(ragdollEntity);
        if (DynamXContext.usesPhysicsWorld(player.world)) {
            DynamXContext.getPlayerToCollision().get(player).ragdollEntity = ragdollEntity;
            DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false, player.world);
        }
        JmeVector3fPool.closePool();
        if (player != sender) {
            sender.sendMessage(new TextComponentString("Ragdoll spawned!"));
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()));
        }
    }
}
