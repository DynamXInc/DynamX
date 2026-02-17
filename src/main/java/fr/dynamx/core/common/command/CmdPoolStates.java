package fr.dynamx.core.common.command;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.utils.optimization.*;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.List;

public class CmdPoolStates implements ISubCommand {
    @Override
    public String getName() {
        return "pool_states";
    }

    @Override
    public String getUsage() {
        return "/[client_]dynamx pool_states [inspect]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        boolean client = sender.getEntityWorld().isRemote;
        if (client) {
            sender.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "Side: client"));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.BLUE + "Side: server"));
        }

        if (args.length == 3 && args[1].matches("inspect")) {
            String target = args[2];
            String result;
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(sender.getEntityWorld());
            switch (target) {
                case "Vector3f":
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Vector3f pool inspection:"));
                    result = JmeVector3fPool.getPool().getExpandedDebugInfo();
                    sender.sendMessage(new TextComponentString(result));

                    if (physicsWorld == null) {
                        return;
                    }
                    physicsWorld.schedule(() -> {
                        String result2 = JmeVector3fPool.getPool().getExpandedDebugInfo();

                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Physics thread:"));
                        sender.sendMessage(new TextComponentString(result2));
                    });
                    break;
                case "Quaternion":
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Quaternion pool inspection:"));
                    result = QuaternionPool.getPool().getExpandedDebugInfo();
                    sender.sendMessage(new TextComponentString(result));

                    if (physicsWorld == null) {
                        return;
                    }
                    physicsWorld.schedule(() -> {
                        String result2 = QuaternionPool.getPool().getExpandedDebugInfo();

                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Physics thread:"));
                        sender.sendMessage(new TextComponentString(result2));
                    });
                    break;
            }
            return;
        }

        String result = "BoundingBox: " + BoundingBoxPool.getPool().getDebugInfo() + "\n" +
                "HashMap: " + HashMapPool.getINSTANCE().getDebugInfo() + "\n" +
                "Quaternion: " + QuaternionPool.getPool().getDebugInfo() + "\n" +
                "UDPByteArray: " + UDPByteArrayPool.getINSTANCE().getDebugInfo() + "\n" +
                "Vector3f: " + JmeVector3fPool.getPool().getDebugInfo() + "\n";

        if (client) {
            result += getClientResult();
        }

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Current thread pools:"));
        sender.sendMessage(new TextComponentString(result));

        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(sender.getEntityWorld());
        if (physicsWorld == null) {
            return;
        }
        physicsWorld.schedule(() -> {
            String result2 = "BoundingBox: " + BoundingBoxPool.getPool().getDebugInfo() + "\n" +
                    "HashMap: " + HashMapPool.getINSTANCE().getDebugInfo() + "\n" +
                    "Quaternion: " + QuaternionPool.getPool().getDebugInfo() + "\n" +
                    "Vector3f: " + JmeVector3fPool.getPool().getDebugInfo() + "\n";

            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Physics thread pools:"));
            sender.sendMessage(new TextComponentString(result2));
        });
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.add("inspect");
        } else if (args.length == 3) {
            r.add("Vector3f");
            r.add("Quaternion");
        }
    }

    private String getClientResult() {
        return "GlQuaternion: " + GlQuaternionPool.getINSTANCE().getDebugInfo() + "\n";
    }
}
