package fr.dynamx.server.command;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.computing.TerrainCollisionsCalculator;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.Profiler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CmdChunkControl implements ISubCommand {
    @Override
    public String getName() {
        return "chunkcontrol";
    }

    @Override
    public String getUsage() {
        return getName() + " <getelements|getslopes|clearslopes|graph|getgraph|resetstate|fullinfo> [pos|mode]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 3) {
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(sender.getEntityWorld());
            if (args[1].equalsIgnoreCase("graph") && args.length == 3) {
                int mode = CommandBase.parseInt(args[2]);
                switch (mode) {
                    case -1:
                        sender.sendMessage(new TextComponentString("Totally disabling data gathering..."));
                        break;
                    case 0:
                        sender.sendMessage(new TextComponentString("Stopping data gathering and printing graph in the log"));
                        break;
                    case 1:
                        sender.sendMessage(new TextComponentString("Starting data gathering for debug chunks"));
                        break;
                    case 2:
                        sender.sendMessage(new TextComponentString("/!\\ Starting data gathering for all chunks : potential memory leak"));
                        break;
                    case 3:
                        sender.sendMessage(new TextComponentString("There is " + physicsWorld.getDynamicsWorld().countRigidBodies() + " " + physicsWorld.getDynamicsWorld().countJoints() + " " + physicsWorld.getDynamicsWorld().countCollisionObjects()));
                        break;
                    default:
                        throw new WrongUsageException("Invalid mode " + mode);
                }
                ChunkGraph.start(mode);
            } else if (args.length == 5) {
                int x = CommandBase.parseInt(args[2]);
                int y = CommandBase.parseInt(args[3]);
                int z = CommandBase.parseInt(args[4]);
                VerticalChunkPos pos = new VerticalChunkPos(x, y, z);
                ChunkCollisions collisions = physicsWorld.getTerrainManager().getChunkAt(pos);
                if (collisions == null) {
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "[CHUNK-CONTROL] Force-load chunk " + pos));
                    collisions = physicsWorld.getTerrainManager().loadChunkCollisionsNow(physicsWorld.getTerrainManager().getTicket(pos), Profiler.get());
                }
                if (args[1].equalsIgnoreCase("getelements")) {
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "All elements : " + collisions.getElements().getElements(TerrainElementType.ALL)));
                }
                if (args[1].equalsIgnoreCase("fullinfo")) {
                    System.out.println("PRINTING CHUNK DATA AT " + pos);
                    System.out.println("Ticket is " + physicsWorld.getTerrainManager().getTicket(pos));
                    List<ITerrainElement> elems = collisions.getElements().getElements(TerrainElementType.ALL);
                    if (elems.isEmpty()) {
                        System.out.println("Is empty");
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "No elements found !"));
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Printing all elements in the log..."));
                        for (ITerrainElement el : elems) {
                            String msg = el.toString();
                            System.out.println("Element : " + msg);
                            //sender.sendMessage(new TextComponentString(msg));
                        }
                    }
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Now simulating collisions calculus..."));
                    List<ITerrainElement> elements = TerrainCollisionsCalculator.computeCollisionFaces(pos, sender.getEntityWorld(), Profiler.get(), true);
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Finished. Got " + elements.size() + " elements. Check the log for details."));
                } else if (args[1].equalsIgnoreCase("getslopes")) {
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Slopes : " + collisions.getElements().getElements(TerrainElementType.PERSISTENT_ELEMENTS)));
                } else if (args[1].equalsIgnoreCase("clear")) {
                    int size = collisions.getElements().getPersistentElements().size();
                    if (size > 0) {
                        collisions.removePersistentElements(physicsWorld.getTerrainManager(), new ArrayList<>(collisions.getElements().getPersistentElements()));
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Removed all slopes of chunk " + pos + " (" + size + " slopes)"));
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "No slopes were found at " + pos));
                    }
                } else if (args[1].equalsIgnoreCase("getgraph")) {
                    ChunkGraph graph = ChunkGraph.getAt(pos);
                    if (graph == null)
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Graph not found !"));
                    else {
                        graph.prettyPrint();
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Printed the graph in console"));
                    }
                } else if (args[1].equalsIgnoreCase("resetstate")) {
                    ChunkLoadingTicket graph = physicsWorld.getTerrainManager().getTicket(pos);
                    if (graph == null)
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Chunk ticket not found !"));
                    else {
                        graph.setLoaded(physicsWorld.getTerrainManager().getTerrainState(), collisions);
                        physicsWorld.getTerrainManager().onChunkChanged(pos);
                        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Reloading this chunk..."));
                    }
                } else
                    throw new WrongUsageException("/dynamx " + getUsage());
            } else
                throw new WrongUsageException("/dynamx " + getUsage());
        } else
            throw new WrongUsageException("/dynamx " + getUsage());
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            List<String> props = new ArrayList<>();
            props.add("getelements");
            props.add("getslopes");
            props.add("clear");
            props.add("graph");
            props.add("getgraph");
            props.add("resetstate");
            props.add("fullinfo");
            props.add("entity_report");
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, props));
        }
    }
}
