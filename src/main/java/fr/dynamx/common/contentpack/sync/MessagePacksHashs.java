package fr.dynamx.common.contentpack.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagePacksHashs implements IDnxPacket {
    private Map<String, Map<String, byte[]>> objects;

    public MessagePacksHashs() {
    }

    public MessagePacksHashs(Map<String, Map<String, byte[]>> objects) {
        this.objects = objects;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        objects = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String name = ByteBufUtils.readUTF8String(buf);
            Map<String, byte[]> map = new HashMap<>();
            int s = buf.readInt();
            for (int j = 0; j < s; j++) {
                String object = ByteBufUtils.readUTF8String(buf);
                byte[] arr = new byte[buf.readInt()];
                buf.readBytes(arr);
                map.put(object, arr);
            }
            objects.put(name, map);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(objects.size());
        objects.forEach((s, m) -> {
            ByteBufUtils.writeUTF8String(buf, s);
            buf.writeInt(m.size());
            m.forEach((o, b) -> {
                ByteBufUtils.writeUTF8String(buf, o);
                buf.writeInt(b.length);
                buf.writeBytes(b);
            });
        });
    }

    public static class HandlerServer implements IMessageHandler<MessagePacksHashs, IMessage> {
        @Override
        public IMessage onMessage(MessagePacksHashs message, MessageContext ctx) {
            if (DynamXConfig.syncPacks) {
                try {
                    Map<String, List<String>> delta = PackSyncHandler.getFullDelta(message.objects);
                    //System.out.println("Full delta with "+ctx.getServerHandler().player+" is "+delta);
                    if (delta.values().stream().anyMatch(l -> !l.isEmpty())) {
                        Map<String, Map<String, byte[]>> fullData = new HashMap<>();
                        int size = 0;
                        for (Map.Entry<String, List<String>> entry : delta.entrySet()) {
                            String s = entry.getKey();
                            List<String> l = entry.getValue();
                            fullData.put(s, new HashMap<>());
                            size += s.getBytes(StandardCharsets.UTF_8).length;
                            InfoLoader<?, ?> loader = DynamXObjectLoaders.getLoaders().stream().filter(i -> i.getPrefix().equals(s)).findFirst().get();
                            loader.encodeObjects(l, fullData.get(s));
                            for (Map.Entry<String, byte[]> e : fullData.get(s).entrySet()) {
                                String a = e.getKey();
                                byte[] b = e.getValue();
                                size += a.getBytes(StandardCharsets.UTF_8).length + b.length;
                            }
                        }
                        //System.out.println("Send data "+fullData+" with byte size "+size);
                        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessagePacksHashs(fullData), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
                    }
                    //else
                    //  System.out.println("There is no delta");
                } catch (Exception e) {
                    ctx.getServerHandler().getNetworkManager().closeChannel(new TextComponentString("Invalid DynamX pack " + e.getMessage()));
                }
            }
            return null;
        }
    }

    public static class HandlerClient implements IMessageHandler<MessagePacksHashs, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessagePacksHashs message, MessageContext ctx) {
            Minecraft.getMinecraft().ingameGUI.setOverlayMessage("Synchronizing DynamX packs...", false);
            Minecraft.getMinecraft().addScheduledTask(() -> {
                //System.out.println("Receive override data "+message.objects);
                try {
                    for (Map.Entry<String, Map<String, byte[]>> entry : message.objects.entrySet()) {
                        String s = entry.getKey();
                        Map<String, byte[]> l = entry.getValue();
                        InfoLoader<?, ?> loader = DynamXObjectLoaders.getLoaders().stream().filter(i -> i.getPrefix().equals(s)).findFirst().get();
                        loader.receiveObjects(l);
                    }
                } catch (Exception e) {
                    DynamXMain.log.fatal("Cannot sync DynamX packs. Connection to the server will be closed.", e);
                    TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 10) {
                        @Override
                        public void run() {
                            ctx.getClientHandler().getNetworkManager().closeChannel(new TextComponentString("Failed to sync DynamX packs. Update your client packs."));
                        }
                    });
                }
                PackSyncHandler.computeAll();
                Minecraft.getMinecraft().ingameGUI.setOverlayMessage("", false);
            });
            return null;
        }
    }
}
