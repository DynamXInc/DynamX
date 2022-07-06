package fr.dynamx.common.slopes;

import com.jme3.math.Vector3f;
import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.physics.terrain.DynamXTerrainApi;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.physics.terrain.element.CustomSlopeTerrainElement;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import fr.dynamx.utils.debug.TerrainDebugRenderer;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.dynamx.utils.debug.ClientDebugSystem.*;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class SlopePreviewer
{
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(new DynamXThreadedModLoader.DefaultThreadFactory("SlopesPreviewer"));

    private static Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> slopeCache;
    private static Vector3f cacheP1, cacheP2;
    private static List<Vector3f> cachePoints;
    private static int cacheVersion = -1;

    private static final SlopesPreviewTerrainLoader slopesPreviewer = new SlopesPreviewTerrainLoader();
    static
    {
        DynamXTerrainApi.addCustomTerrainLoader(slopesPreviewer);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void worldRender(RenderWorldLastEvent event)
    {
        //Drawing preview of slopes with the slope item
        if(Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemSlopes){
            Vector3fPool.openPool();
            GlStateManager.pushMatrix();
            EntityPlayer rootPlayer = Minecraft.getMinecraft().player;
            float partialTicks = event.getPartialTicks();
            double x = rootPlayer.lastTickPosX + (rootPlayer.posX - rootPlayer.lastTickPosX) * partialTicks;
            double y = rootPlayer.lastTickPosY + (rootPlayer.posY - rootPlayer.lastTickPosY) * partialTicks;
            double z = rootPlayer.lastTickPosZ + (rootPlayer.posZ - rootPlayer.lastTickPosZ) * partialTicks;
            GlStateManager.translate(-x, -y, -z);
            GlStateManager.disableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();

            renderSlopesDebug();

            ItemStack s = Minecraft.getMinecraft().player.getHeldItemMainhand();
            float margin = 0.2f;
            Vector3f hoveredPos = null;
            if(Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK)
            {
                Vec3d post = Minecraft.getMinecraft().objectMouseOver.hitVec;
                hoveredPos = ItemSlopes.fixPos(Minecraft.getMinecraft().world, post);
            }
            boolean hoveredPosDrawn = hoveredPos == null;
            if(s.hasTagCompound()){
                int mode = s.getTagCompound().getInteger("mode");
                switch (mode) {
                    case 0: //Delete mode
                        hoveredPosDrawn = drawDeleteModePreview(margin, s, hoveredPos);
                        break;
                    case 1: //Create mode
                        hoveredPosDrawn = drawCreateModePreview(margin, s, hoveredPos);
                        break;
                    case 2: //Auto mode
                        hoveredPosDrawn = drawAutoModePreview(rootPlayer, margin, s, hoveredPos);
                        break;
                }
            }
            if(!hoveredPosDrawn) {
                drawAABBDebug(new float[] {hoveredPos.x-margin, hoveredPos.y, hoveredPos.z-margin, hoveredPos.x+margin, hoveredPos.y, hoveredPos.z+margin, 0, 0.5f, 1});
            }

            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
            Vector3fPool.closePool();
        }
    }

    private static void renderSlopesDebug() {
        //Draw slopes debug
        if(!enableDebugDrawing || (!DynamXDebugOptions.SLOPE_BOXES.isActive() && !DynamXDebugOptions.CLIENT_SLOPE_BOXES.isActive())) {
            try {
                DynamXDebugOption.TerrainDebugOption option = DynamXDebugOptions.SLOPE_BOXES.getDataIn().isEmpty() ? DynamXDebugOptions.CLIENT_SLOPE_BOXES : DynamXDebugOptions.SLOPE_BOXES;
                for (Map.Entry<Integer, TerrainDebugData> pos : option.getDataIn().entrySet()) {
                    drawSlopeDebug(pos.getValue().getData(), 1, 0.5f, 0.5f, 0.5f);
                    if(pos.getValue().getRenderer() == TerrainDebugRenderer.CUSTOM_SLOPE) {
                        float margin = 0.02f;
                        float[] p = pos.getValue().getData();
                        drawAABBDebug(new float[] {p[p.length-3]-margin, p[p.length-2]-margin, p[p.length-1]-margin, p[p.length-3]+margin, p[p.length-2]+margin, p[p.length-1]+margin,
                                pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB()});
                    }
                }
            } catch (ConcurrentModificationException ignored) {
            } //Server is modifying something
        }
    }

    private static boolean drawDeleteModePreview(float margin, ItemStack s, Vector3f hoveredPos) {
        if(s.getTagCompound().hasKey("p1") && s.getTagCompound().hasKey("p2")){
            Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("p1"));
            drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
            if(p1.equals(hoveredPos))
            {
                hoveredPos = null;
                drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 0, 0});
            }
            else
                drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
            Vector3f p2 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("p2"));
            drawAABBDebug(new float[] {p2.x-margin, p2.y, p2.z-margin, p2.x+margin, p2.y, p2.z+margin, 1, 1, 0});
            drawAABBDebug(new float[]{p1.x,p1.y,p1.z,p2.x,p2.y,p2.z,1,1,1});
        }
        else if(s.getTagCompound().hasKey("p1")) {
            Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("p1"));
            drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
        }
        return hoveredPos == null;
    }

    private static boolean drawAutoModePreview(EntityPlayer rootPlayer, float margin, ItemStack s, Vector3f hoveredPos) {
        if(s.getTagCompound().hasKey("pt1") && s.getTagCompound().hasKey("pt2")){
            Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("pt1"));
            drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
            if(p1.equals(hoveredPos))
            {
                hoveredPos = null;
                drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 0, 0});
            }
            else
                drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
            Vector3f p2 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("pt2"));
            drawAABBDebug(new float[] {p2.x-margin, p2.y, p2.z-margin, p2.x+margin, p2.y, p2.z+margin, 1, 1, 0});

            SlopeBuildingConfig config = new SlopeBuildingConfig(s.getTagCompound().getCompoundTag("ptconfig"));
            if(!p1.equals(cacheP1) || !p2.equals(cacheP2) || cacheVersion != config.getConfigVersion())
            {
                cacheP1 = Vector3fPool.getPermanentVector(p1);
                cacheP2 = Vector3fPool.getPermanentVector(p2);
                cacheVersion = config.getConfigVersion();
                if(config.getFacing().getAxis() != EnumFacing.Axis.Y) {
                    Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.GOLD + "Generating preview...", false);
                    POOL.submit(() -> {
                        Vector3fPool.openPool();
                        slopeCache = SlopeGenerator.generateSlopesInBox(rootPlayer.world, config, new BlockPos(p1.x, p1.y, p1.z), new BlockPos(p2.x, p2.y, p2.z));
                        if(slopeCache.isEmpty())
                            Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.RED + "No slope found", false);
                        else
                            Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.GREEN + "Preview generated", false);
                        Vector3fPool.closePool();
                    });
                }
                else
                {
                    slopeCache = null;
                    rootPlayer.sendMessage(new TextComponentString(TextFormatting.GOLD+"[AUTO] Veuillez configurer une direction : /dynamx slopes automatic facing <facing>"));
                }
            }
            drawSlopesFromCache();
        }
        else if(s.getTagCompound().hasKey("pt1")) {
            Vector3f p1 = ItemSlopes.getPosFromTag((NBTTagCompound) s.getTagCompound().getTag("pt1"));
            drawAABBDebug(new float[] {p1.x-margin, p1.y, p1.z-margin, p1.x+margin, p1.y, p1.z+margin, 1, 1, 0});
        }
        return hoveredPos == null;
    }

    private static boolean drawCreateModePreview(float margin, ItemStack s, Vector3f hoveredPos) {
        if (s.getTagCompound().hasKey("plist")) {
            NBTTagList points = s.getTagCompound().getTagList("plist", 10);
            if (points.tagCount() >= 4) {
                GlStateManager.color(1,0,0, 0.4f);
                GlStateManager.disableCull();

                List<Vector3f> pos = new ArrayList<>();
                for (NBTBase c : points) {
                    pos.add(ItemSlopes.getPosFromTag((NBTTagCompound) c));
                }
                if(!pos.equals(cachePoints)) {
                    cachePoints = pos;
                    cacheVersion = -1;
                    Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.GOLD + "Generating preview...", false);
                    POOL.submit(() -> {
                        Vector3fPool.openPool();
                        slopeCache = SlopeGenerator.generateSlopesFromControlPoints(cachePoints);
                        if(slopeCache.isEmpty())
                            Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.RED + "No slope found", false);
                        else
                            Minecraft.getMinecraft().ingameGUI.setOverlayMessage(TextFormatting.GREEN + "Preview generated", false);
                        Vector3fPool.closePool();
                    });
                }
                drawSlopesFromCache();
            }
            for (int i = 0; i < points.tagCount(); i++) {
                Vector3f pos = ItemSlopes.getPosFromTag(points.getCompoundTagAt(i));
                if(pos.equals(hoveredPos))
                {
                    hoveredPos = null;
                    drawAABBDebug(new float[] {pos.x-margin, pos.y, pos.z-margin, pos.x+margin, pos.y, pos.z+margin, 1, 0, 0});
                }
                else
                    drawAABBDebug(new float[] {pos.x-margin, pos.y, pos.z-margin, pos.x+margin, pos.y, pos.z+margin, 0, 1, 0});
            }
        }
        return hoveredPos == null;
    }

    private static void drawSlopesFromCache() {
        if(slopeCache != null)
        {
            for(Map.Entry<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> e : slopeCache.entrySet())
            {
                Vector3f pos = Vector3fPool.get(e.getKey().x * 16 + 8, e.getKey().y * 16, e.getKey().z * 16 + 8);
                for(ITerrainElement el : e.getValue()) {
                    drawSlopeDebug(((CustomSlopeTerrainElement) el).getDebugDataPreview(pos), 0, 0.7f, 0.7f, 0.5f);
                }
            }
        }
    }
}
