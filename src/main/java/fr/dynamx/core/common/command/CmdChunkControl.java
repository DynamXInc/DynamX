package fr.dynamx.core.common.command;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.core.common.physics.terrain.computing.TerrainCollisionsCalculator;
import fr.dynamx.core.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.ChunkGraph;
import fr.dynamx.core.utils.debug.Profiler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CmdChunkControl implements ISubCommand {
    private final boolean isClient;

    public CmdChunkControl(boolean isClient) {
        this.isClient = isClient;
    }

    private String prefix() {
        if (isClient) {
            return TextFormatting.GOLD + "[DynamX-Client] " + TextFormatting.RESET;
        }
        return TextFormatting.GREEN + "[DynamX-Server] " + TextFormatting.RESET;
    }

    @Override
    public String getName() {
        return "chunkcontrol";
    }

    @Override
    public String getUsage() {
        return getName() + " <graphmode|getelements|getslopes|clearslopes|getgraph|resetstate|fullinfo> <pos|mode>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 3) {
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(sender.getEntityWorld());
            if (args[1].equalsIgnoreCase("graphmode")) {
                if (args.length != 3) {
                    throw new WrongUsageException("chunkcontrol graphmode <mode>");
                }
                String mode = args[2];
                int modeInt;
                switch (mode) {
                    case "print_then_disable":
                        sender.sendMessage(new TextComponentString(prefix() + "Totally disabling data gathering..."));
                        modeInt = -1;
                        break;
                    case "print_then_keep_recent":
                        sender.sendMessage(new TextComponentString(prefix() + "Stopping data gathering and printing graph in the log"));
                        modeInt = 0;
                        break;
                    case "keep_tracked":
                        sender.sendMessage(new TextComponentString(prefix() + "Starting data gathering for debug chunks"));
                        modeInt = 1;
                        break;
                    case "keep_all":
                        sender.sendMessage(new TextComponentString(prefix() + "/!\\ Starting data gathering for all chunks. Be careful, this is a memory leak."));
                        modeInt = 2;
                        break;
                    case "print_disable_and_get_physics_object_count":
                        sender.sendMessage(new TextComponentString(prefix() + "There is b:" + physicsWorld.getDynamicsWorld().countRigidBodies() + " j:" + physicsWorld.getDynamicsWorld().countJoints() + " co:" + physicsWorld.getDynamicsWorld().countCollisionObjects()));
                        modeInt = 3;
                        break;
                    default:
                        throw new WrongUsageException("Invalid mode " + mode);
                }
                ChunkGraph.start(modeInt);
            } else if (args.length == 5) {
                int x = CommandBase.parseInt(args[2]);
                int y = CommandBase.parseInt(args[3]);
                int z = CommandBase.parseInt(args[4]);
                VerticalChunkPos pos = new VerticalChunkPos(x, y, z);
                ChunkCollisions collisions = physicsWorld.getTerrainManager().getChunkAt(pos);
                if (collisions == null) {
                    sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "[CHUNK-CONTROL] Force-load chunk " + pos));
                    collisions = physicsWorld.getTerrainManager().loadChunkCollisionsNow(physicsWorld.getTerrainManager().getTicket(pos), Profiler.get());
                }
                if (args[1].equalsIgnoreCase("getelements")) {
                    sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "All elements : " + collisions.getElements().getElements(TerrainElementType.ALL)));
                }
                if (args[1].equalsIgnoreCase("fullinfo")) {
                    System.out.println("PRINTING CHUNK DATA AT " + pos);
                    System.out.println("Ticket is " + physicsWorld.getTerrainManager().getTicket(pos));
                    List<ITerrainElement> elems = collisions.getElements().getElements(TerrainElementType.ALL);
                    if (elems.isEmpty()) {
                        System.out.println("Is empty");
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "No elements found !"));
                    } else {
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Printing all elements in the log..."));
                        for (ITerrainElement el : elems) {
                            String msg = el.toString();
                            System.out.println("Element : " + msg);
                            //sender.sendMessage(new TextComponentString(msg));
                        }
                    }
                    sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Now simulating collisions calculus..."));
                    List<ITerrainElement> elements = TerrainCollisionsCalculator.computeCollisionFaces(pos, sender.getEntityWorld(), Profiler.get(), true);
                    sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Finished. Got " + elements.size() + " elements. Check the log for details."));
                } else if (args[1].equalsIgnoreCase("getslopes")) {
                    sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Slopes : " + collisions.getElements().getElements(TerrainElementType.PERSISTENT_ELEMENTS)));
                } else if (args[1].equalsIgnoreCase("clear")) {
                    int size = collisions.getElements().getPersistentElements().size();
                    if (size > 0) {
                        collisions.removePersistentElements(physicsWorld.getTerrainManager(), new ArrayList<>(collisions.getElements().getPersistentElements()));
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Removed all slopes of chunk " + pos + " (" + size + " slopes)"));
                    } else {
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "No slopes were found at " + pos));
                    }
                } else if (args[1].equalsIgnoreCase("getgraph")) {
                    ChunkGraph graph = ChunkGraph.getAt(pos);
                    if (graph == null)
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.RED + "Graph not found !"));
                    else {
                        graph.prettyPrint();
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Printed the graph in console"));
                    }
                } else if (args[1].equalsIgnoreCase("resetstate")) {
                    ChunkLoadingTicket graph = physicsWorld.getTerrainManager().getTicket(pos);
                    if (graph == null)
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.RED + "Chunk ticket not found !"));
                    else {
                        graph.setLoaded(collisions);
                        physicsWorld.getTerrainManager().onChunkChanged(pos);
                        sender.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY + "Reloading this chunk..."));
                    }
                } else {
                    throw new WrongUsageException(getUsage());
                }
            } else
                throw new WrongUsageException(getUsage());
        } else
            throw new WrongUsageException(getUsage());
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        List<String> props = new ArrayList<>();
        if (args.length == 2) {
            props.add("graphmode");
            props.add("getelements");
            props.add("getslopes");
            props.add("clear");
            props.add("getgraph");
            props.add("resetstate");
            props.add("fullinfo");
        }
        if (args.length == 3 && args[1].equals("graphmode")) {
            props.add("print_then_disable");
            props.add("print_then_keep_recent");
            props.add("keep_tracked");
            props.add("keep_all");
            props.add("print_disable_and_get_physics_object_count");
        }
        r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, props));
    }
}
