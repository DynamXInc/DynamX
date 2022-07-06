package fr.dynamx.server.command;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.packets.MessageSwitchAutoSlopesMode;
import fr.dynamx.common.network.packets.MessageUpdateChunk;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.slopes.SlopeBuildingConfig;
import fr.dynamx.common.slopes.SlopeGenerator;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.*;

public class CmdSlopes implements ISubCommand
{
    @Override
    public String getName() {
        return "slopes";
    }

    @Override
    public String getUsage() {
        return getName()+" <create|delete|automatic|enableAutoSlopes>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        ItemStack stack = ((EntityPlayer) (sender)).getHeldItem(EnumHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ItemSlopes)) {
            sender.sendMessage(new TextComponentTranslation("cmd.slopes.noitem"));
            return;
        }
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        if (args.length >= 2) {
            exec(server, (EntityPlayer) sender, stack, args);
        } else {
            throw new WrongUsageException("/dynamx "+getUsage());
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos, List<String> r) {
        if(args.length>=2){
            if(args.length == 2) {
                r.add("create");
                r.add("delete");
                r.add("automatic");
                //r.add("switch");
                //r.add("clear");
                r.add("enableAutoSlopes");
            }
            else if(args[1].equalsIgnoreCase("automatic")) {
                if(args.length == 3) {
                    r.add("generate");
                    r.add("facing");
                    r.add("round");
                    r.add("diagDir");
                    r.add("refresh");
                }
                else if(args.length == 4) {
                    if(args[2].equalsIgnoreCase("facing")) {
                        for(EnumFacing f : EnumFacing.HORIZONTALS)
                            r.add(f.name());
                    }
                    else if(args[2].equalsIgnoreCase("diagDir")) {
                        r.add("+");
                        r.add("-");
                    }
                    else if(args[2].equalsIgnoreCase("round")) {
                        r.add("false");
                        r.add("true");
                    }
                }
            }
        }
    }

    private void exec(MinecraftServer server, EntityPlayer sender, ItemStack stack, String[] args) throws CommandException {
        if (args[1].equalsIgnoreCase("create")) {
            if (stack.getTagCompound().getInteger("mode") != 1) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.needcreate"));
                return;
            }
            List<Vector3f> pos = new ArrayList<>();
            for (NBTBase c : stack.getTagCompound().getTagList("plist", 10)) {
                pos.add(ItemSlopes.getPosFromTag((NBTTagCompound) c));
            }
            if(pos.size()<4){
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.points"));
                return;
            }

            long start = System.currentTimeMillis();
            Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> l3 = SlopeGenerator.generateSlopesFromControlPoints(pos);
            if(l3.isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.error"));
                return;
            }
            //Saving the custom slope so it can be replaced when world loads
            ChunkCollisions c;
            //VerticalChunkPos cp = new VerticalChunkPos((int) p1.x / 16, (int) p1.y / 16, (int) p1.z / 16);
            boolean error = false;
            for(Map.Entry<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> cst : l3.entrySet())
            {
                c = DynamXContext.getPhysicsWorld().getTerrainManager().getChunkAt(cst.getKey());
                if(c == null) {
                    sender.sendMessage(new TextComponentString(TextFormatting.GRAY+"[SLOPES] Force-load chunk "+cst.getKey()));
                    c = DynamXContext.getPhysicsWorld().getTerrainManager().loadChunkCollisionsNow(DynamXContext.getPhysicsWorld().getTerrainManager().getTicket(cst.getKey()), Profiler.get());
                }
                if(c == null) {
                    error = true;
                    break;
                }
            }
            if(error) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.terrainerror"));
                return;
            }
            //Very important : will set newest computed chunks
            DynamXContext.getPhysicsWorld().getTerrainManager().notifyWillChange();
            for(Map.Entry<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> cst : l3.entrySet())
            {
                if((c = DynamXContext.getPhysicsWorld().getTerrainManager().getChunkAt(cst.getKey())) != null) {
                    c.addPersistentElements(DynamXContext.getPhysicsWorld().getTerrainManager(), cst.getValue());
                    if(server.isDedicatedServer()) {
                        //Send updates to client
                        //Set<VerticalChunkPos> set = new HashSet<>();
                        //set.add(cst.getKey());
                        //MessageHandler.NETWORK.getVanillaNetwork().getChannel().sendToAll(new MessageSwitchAutoSlopesMode(1, stack.getTagCompound()));
                        DynamXContext.getNetwork().sendToClient(new MessageUpdateChunk(new VerticalChunkPos[]{cst.getKey()}), EnumPacketTarget.ALL);
                    }
                }
                else {
                    error = true;
                }
            }
            sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.result", TextFormatting.GOLD, ""+(System.currentTimeMillis()-start)));

            if(error)
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.resulterror"));
            else
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.success"));
            //slopes.clearMemory(sender.world, sender, stack);
        }
        else if (args[1].equalsIgnoreCase("delete")) {
            if (stack.getTagCompound().getInteger("mode") != 0) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.needdelete"));
                return;
            }
            if (!stack.getTagCompound().hasKey("p1") || !stack.getTagCompound().hasKey("p2")) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.delete.points"));
                return;
            }
            Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) stack.getTagCompound().getTag("p1"));
            Vector3f p2 = ItemSlopes.getPosFromTag((NBTTagCompound) stack.getTagCompound().getTag("p2"));

            float minX = Math.min(p1.x, p2.x);
            float maxX = Math.max(p1.x, p2.x);

            float minY = Math.min(p1.y, p2.y);
            float maxY = Math.max(p1.y, p2.y);

            float minZ = Math.min(p1.z, p2.z);
            float maxZ = Math.max(p1.z, p2.z);

            boolean out = false;
            Set<VerticalChunkPos> set = new HashSet<>();
            int count = 0;
            //Very important : will set newest computed chunks
            DynamXContext.getPhysicsWorld().getTerrainManager().notifyWillChange();
            for (float i = minX; i <= maxX+16; i += 16) {
                for (float j = minZ; j <= maxZ+16; j += 16) {
                    for (float k = minY; k <= maxY+16; k += 16) {
                        float ir = i;
                        float jr = j;
                        if(ir< 0)
                            ir -= 16;
                        if(jr < 0)
                            jr -= 16;
                        VerticalChunkPos chunkPos = new VerticalChunkPos((int) ir / 16, (int) k / 16, (int) jr / 16);
                        ChunkCollisions chunkData = DynamXContext.getPhysicsWorld().getTerrainManager().getChunkAt(chunkPos);
                        if(chunkData == null) {
                            sender.sendMessage(new TextComponentString(TextFormatting.GRAY+"[SLOPES] Force-load chunk "+chunkPos));
                            chunkData = DynamXContext.getPhysicsWorld().getTerrainManager().loadChunkCollisionsNow(DynamXContext.getPhysicsWorld().getTerrainManager().getTicket(chunkPos), Profiler.get());
                        }
                        if (chunkData != null) {
                            List<ITerrainElement.IPersistentTerrainElement> toRemove = new ArrayList<>();
                            for (ITerrainElement.IPersistentTerrainElement element : chunkData.getElements().getPersistentElements()) {
                                BoundingBox box = BoundingBoxPool.get();
                                element.getBody().boundingBox(box);
                                Vector3f min = Vector3fPool.get();
                                Vector3f max = Vector3fPool.get();
                                box.getMin(min);
                                box.getMax(max);
                                if (min.x >= minX-1 && max.x <= maxX+1 && min.y >= minY-1 && max.y <= maxY+1 && max.z <= maxZ+1 && min.z >= minZ-1) {
                                    set.add(chunkPos);
                                    toRemove.add(element);
                                    count++;
                                }
                                else
                                    out = true;
                            }
                            chunkData.removePersistentElements(DynamXContext.getPhysicsWorld().getTerrainManager(), toRemove);
                        }
                        else {
                            sender.sendMessage(new TextComponentTranslation("cmd.slopes.delete.terrainerror", chunkPos.toString()));
                        }
                    }
                }
            }
            if(server.isDedicatedServer()) {
                //MessageHandler.NETWORK.getVanillaNetwork().getChannel().sendToAll(new MessageSwitchAutoSlopesMode(0, stack.getTagCompound()));
                DynamXContext.getNetwork().sendToClient(new MessageUpdateChunk(set.toArray(new VerticalChunkPos[0])), EnumPacketTarget.ALL);
            }
            if(set.isEmpty())
            {
                if(out)
                    sender.sendMessage(new TextComponentTranslation("cmd.slopes.delete.selerror"));
                else
                    sender.sendMessage(new TextComponentTranslation("cmd.slopes.delete.selempty"));
            }
            else
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.delete.result", ""+TextFormatting.LIGHT_PURPLE.toString()+count));

            //slopes.clearMemory(sender.world, sender, stack);
        }
        else if (args[1].equalsIgnoreCase("automatic")) {
            if(args.length < 3)
                throw new WrongUsageException("/dynamx slopes automatic <generate|facing|diagDir|round>");
            if (stack.getTagCompound().getInteger("mode") != 2) {
                sender.sendMessage(new TextComponentTranslation("cmd.slopes.needauto"));
                return;
            }

            switch (args[2])
            {
                case "facing":
                {
                    if(args.length != 4)
                        throw new WrongUsageException("/dynamx slopes automatic facing <NORTH|EAST|SOUTH|WEST>");
                    EnumFacing f = null;
                    for(EnumFacing facing : EnumFacing.values()) {
                        if(facing.name().equalsIgnoreCase(args[3]))
                        {
                            f = facing;
                            break;
                        }
                    }
                    if(f == null)
                        throw new CommandException("Facing "+args[2]+" not found");
                    SlopeBuildingConfig config = new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig"));
                    config.setFacing(f);
                    stack.getTagCompound().setTag("ptconfig", config.serialize());
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD+"[AUTO] Facing set to "+f.name()));
                    break;
                }
                case "diagDir":
                {
                    if(args.length != 4)
                        throw new WrongUsageException("/dynamx slopes automatic diagDir <+|->");
                    if(!args[3].equals("+") && !args[3].equals("-"))
                        throw new WrongUsageException("'+' or '-' expected !");
                    SlopeBuildingConfig config = new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig"));
                    config.setDiagDir(args[3].equals("-") ? -1 : 1);
                    stack.getTagCompound().setTag("ptconfig", config.serialize());
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD+"[AUTO] Diagonal direction set to "+args[3]));
                    break;
                }
                case "round":
                {
                    if(args.length != 4)
                        throw new WrongUsageException("/dynamx slopes automatic round <false|true>");
                    boolean round = CommandBase.parseBoolean(args[3]);
                    SlopeBuildingConfig config = new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig"));
                    config.setEnableSlabs(!round);
                    stack.getTagCompound().setTag("ptconfig", config.serialize());
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD+"[AUTO] Round set to "+round));
                    break;
                }
                case "refresh":
                {
                    SlopeBuildingConfig config = new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig"));
                    config.refresh();
                    stack.getTagCompound().setTag("ptconfig", config.serialize());
                    break;
                }
                case "generate":
                {
                    if (!stack.getTagCompound().hasKey("pt1") || !stack.getTagCompound().hasKey("pt2")) {
                        sender.sendMessage(new TextComponentTranslation("cmd.slopes.auto.points"));
                        return;
                    }
                    sender.sendMessage(new TextComponentTranslation("cmd.slopes.auto.working"));
                    Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) stack.getTagCompound().getTag("pt1"));
                    Vector3f p2 = ItemSlopes.getPosFromTag((NBTTagCompound) stack.getTagCompound().getTag("pt2"));

                    float minX = p1.x;//Math.min(p1.x, p2.x);
                    float maxX = p2.x;//Math.max(p1.x, p2.x);

                    float minY = p1.y;//Math.min(p1.y, p2.y);
                    float maxY = p2.y;//Math.max(p1.y, p2.y);

                    float minZ = p1.z;//Math.min(p1.z, p2.z);
                    float maxZ = p2.z;//qMath.max(p1.z, p2.z);

                    SlopeBuildingConfig config = new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig"));
                    /*EnumFacing f = EnumFacing.values()[stack.getTagCompound().getInteger("ptface")];
                    if(f.getAxis() == EnumFacing.Axis.Y)
                        throw new IllegalArgumentException("Facing is not set !");
                    boolean round = stack.getTagCompound().getBoolean("ptround");
                    int diagDir = stack.getTagCompound().getInteger("ptdiagdir");
                    if(diagDir != 1 && diagDir != -1)
                        diagDir = 1;*/
                    long start = System.currentTimeMillis();
                    Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> l3 = SlopeGenerator.generateSlopesInBox(sender.getEntityWorld(), config, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
                    if(l3.isEmpty()) {
                        sender.sendMessage(new TextComponentTranslation("cmd.slopes.auto.error"));
                        return;
                    }
                    //Saving the custom slope so it can be replaced when world loads
                    ChunkCollisions c;
                    //VerticalChunkPos cp = new VerticalChunkPos((int) p1.x / 16, (int) p1.y / 16, (int) p1.z / 16);
                    boolean error = false;
                    for(Map.Entry<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> cst : l3.entrySet())
                    {
                        c= DynamXContext.getPhysicsWorld().getTerrainManager().getChunkAt(cst.getKey());
                        if(c == null) {
                            sender.sendMessage(new TextComponentTranslation(TextFormatting.GRAY+"[SLOPES] Force-load chunk "+cst.getKey()));
                            c = DynamXContext.getPhysicsWorld().getTerrainManager().loadChunkCollisionsNow(DynamXContext.getPhysicsWorld().getTerrainManager().getTicket(cst.getKey()), Profiler.get());
                        }
                        if(c == null) {
                            error = true;
                            break;
                        }
                    }
                    if(error) {
                        sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.terrainerror"));
                        return;
                    }
                    //Very important : will set newest computed chunks
                    DynamXContext.getPhysicsWorld().getTerrainManager().notifyWillChange();
                    for(Map.Entry<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> cst : l3.entrySet())
                    {
                        if((c= DynamXContext.getPhysicsWorld().getTerrainManager().getChunkAt(cst.getKey())) != null) {
                            c.addPersistentElements(DynamXContext.getPhysicsWorld().getTerrainManager(), cst.getValue());
                            if(server.isDedicatedServer()) {
                                //Send updates to client
                                //Set<VerticalChunkPos> set = new HashSet<>();
                                //set.add(cst.getKey());
                                //MessageHandler.NETWORK.getVanillaNetwork().getChannel().sendToAll(new MessageSwitchAutoSlopesMode(1, stack.getTagCompound()));
                                DynamXContext.getNetwork().sendToClient(new MessageUpdateChunk(new VerticalChunkPos[]{cst.getKey()}), EnumPacketTarget.ALL);
                            }
                        }
                        else
                            error = true;
                    }
                    sender.sendMessage(new TextComponentTranslation("cmd.slopes.auto.result", ""+(System.currentTimeMillis()-start)));

                    if(error)
                        sender.sendMessage(new TextComponentTranslation("cmd.slopes.create.resulterror"));
                    else
                        sender.sendMessage(new TextComponentTranslation("cmd.slopes.auto.success"));
                    break;
                }
            }
        }
        /*else if (args[1].equalsIgnoreCase("switch")) {
            if (stack.getTagCompound().getInteger("mode") == 0) {
                stack.getTagCompound().setInteger("mode", 1);
            } else {
                stack.getTagCompound().setInteger("mode", 0);
            }
            sender.sendMessage(new TextComponentTranslation("Mode mis à " + (stack.getTagCompound().getInteger("mode") == 1 ? "[CREATE]" : "[DELETE]")));
        }
        else if (args[1].equalsIgnoreCase("clear")) {
            int mode = stack.getTagCompound().getInteger("mode");
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setInteger("mode", mode);
            sender.sendMessage(new TextComponentTranslation("Points supprimés de l'item !"));
        }*/
        else if (args[1].equalsIgnoreCase("enableAutoSlopes")) {
            ContentPackLoader.PLACE_SLOPES = !ContentPackLoader.PLACE_SLOPES;
            //NBTTagCompound tag = new NBTTagCompound();
            //tag.setBoolean("enable", ContentPackLoader.PLACE_SLOPES);
            DynamXContext.getNetwork().getVanillaNetwork().getChannel().sendToAll(new MessageSwitchAutoSlopesMode(ContentPackLoader.PLACE_SLOPES ? 1 : 0));
            sender.sendMessage(new TextComponentString("[EXPERIMENTAL] Placement des pentes automatiques " + (ContentPackLoader.PLACE_SLOPES ? "activé" : "désactivé")));
            sender.sendMessage(new TextComponentString("Cette valeur n'est pas sauvegardée par défaut, ajoutez \"auto slopes:"+ContentPackLoader.PLACE_SLOPES+"\" dans le fichier slopes.dynx !"));
        }
        else {
            throw new WrongUsageException("/dynamx "+getUsage());
        }
    }
}
