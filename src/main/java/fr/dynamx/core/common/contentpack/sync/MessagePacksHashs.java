package fr.dynamx.core.common.contentpack.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.core.common.contentpack.loader.InfoLoader;
import fr.dynamx.core.common.handlers.TaskScheduler;
import fr.dynamx.forge.DynamXConfig;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            if (!DynamXConfig.syncPacks) {
                DynamXMain.log.warn("[PackSync] Sync requested by {}, but disabled on server", ctx.getServerHandler().player);
                return null;
            }
            try {
                Map<String, List<String>> delta = PackSyncHandler.getFullDelta(message.objects);
                if (delta.values().stream().allMatch(List::isEmpty)) {
                    DynamXMain.log.debug("[PackSync] No delta for {}", ctx.getServerHandler().player);
                    return null;
                }

                Map<String, Map<String, byte[]>> fullData = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : delta.entrySet()) {
                    String s = entry.getKey();
                    List<String> l = entry.getValue();
                    fullData.put(s, new HashMap<>());
                    InfoLoader<?> loader = DynamXObjectLoaders.getInfoLoaders().stream().filter(i -> i.getPrefix().equals(s)).findFirst().get();
                    loader.encodeObjects(l, fullData.get(s));
                }

                DynamXMain.log.info("[PackSync] Sending {} changed pack files to {}", fullData.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue().size()).collect(Collectors.toList()), ctx.getServerHandler().player);
                DynamXContext.getNetwork().sendToClientFromOtherThread(new MessagePacksHashs(fullData), EnumPacketTarget.PLAYER, ctx.getServerHandler().player);
            } catch (Exception e) {
                DynamXMain.log.error("[PackSync] Failed to sync changed pack files for " + ctx.getServerHandler().player, e);
                ctx.getServerHandler().getNetworkManager().closeChannel(new TextComponentString("Invalid DynamX pack " + e.getMessage()));
            }
            return null;
        }
    }

    public static class HandlerClient implements IMessageHandler<MessagePacksHashs, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessagePacksHashs message, MessageContext ctx) {
            DynamXMain.log.info("[PackSync] Received server packs, applying diff of {} elements...",
                    message.objects.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue().size()).collect(Collectors.toList()));

            Minecraft.getMinecraft().ingameGUI.setOverlayMessage("Synchronizing DynamX packs...", false);
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Vector3fPool.openPool();

                try {
                    for (Map.Entry<String, Map<String, byte[]>> entry : message.objects.entrySet()) {
                        String s = entry.getKey();
                        Map<String, byte[]> l = entry.getValue();
                        InfoLoader<?> loader = DynamXObjectLoaders.getInfoLoaders().stream().
                                filter(i -> i.getPrefix().equals(s)).findFirst().get();
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

                DynamXContext.getDxModelRegistry().getItemRenderer().refreshItemInfos();
                DynamXUtils.hotswapWorldPackInfos(DynamXMain.getProxy().getClientWorld());
                Minecraft.getMinecraft().ingameGUI.setOverlayMessage("", false);

                Vector3fPool.closePool();
            });
            return null;
        }
    }
}
