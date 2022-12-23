package fr.dynamx.client.handlers;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.gui.GuiLoadingErrors;
import fr.dynamx.client.gui.GuiTexturedButton;
import fr.dynamx.client.gui.VehicleHud;
import fr.dynamx.client.renders.RenderMovableLine;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.items.DynamXItemSpawner;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.packets.MessageEntityInteract;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.common.slopes.GuiSlopesConfig;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class ClientEventHandler {
    public static final Minecraft MC = Minecraft.getMinecraft();
    public static boolean rendering;
    public static RenderPlayer renderPlayer;

    /* Placing block */
    private ObjModelRenderer model;
    private boolean canPlace;
    private BlockPos blockPos;
    private int playerOrientation;
    private BlockObject<?> blockObjectInfo;

    /* World events */

    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load event) {
        if (DynamXContext.getPhysicsWorld() == null && event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            DynamXContext.setPhysicsWorld(DynamXMain.proxy.provideClientPhysicsWorld(event.getWorld()));
        }
    }

    @SubscribeEvent
    public void onWorldUnloaded(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            ClientProxy.SOUND_HANDLER.unload();
        }
        DynamXDebugOptions.PROFILING.disable();
    }

    /* Interaction events */

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            if (event.getTarget() instanceof PhysicsEntity && event.getHand().equals(EnumHand.MAIN_HAND) && !(event.getEntityPlayer().getHeldItem(event.getHand()).getItem() instanceof DynamXItemSpawner)) {
                DynamXContext.getNetwork().sendToServer(new MessageEntityInteract(event.getTarget().getEntityId()));
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickItem e) {
        if (e.getWorld().isRemote && (MC.objectMouseOver == null || MC.objectMouseOver.typeOfHit == RayTraceResult.Type.MISS) && e.getEntity() instanceof EntityPlayer && !e.getEntity().isSneaking() && e.getItemStack().getItem() instanceof ItemSlopes) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSlopesConfig(e.getItemStack()).getGuiScreen());
        }
    }

    @SubscribeEvent
    public void onMount(VehicleEntityEvent.PlayerMount event) {
        if (event.getPlayer().isUser()) {
            ACsGuiApi.asyncLoadThenShowHudGui("Vehicle HUD", () -> new VehicleHud((IModuleContainer.ISeatsContainer) event.getEntity()));
        }
    }

    @SubscribeEvent
    public void onDismount(VehicleEntityEvent.PlayerDismount event) {
        if (event.getPlayer().isUser()) {
            ACsGuiApi.closeHudGui();
        }
    }

    /* Gui events */

    //private static final ResourceLocation CRAFTING_TABLE_GUI_TEXTURES = new ResourceLocation("textures/gui/container/crafting_table.png");

    @SubscribeEvent
    public void initMainMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu && DynamXErrorManager.getErrorManager().hasErrors(ACsLib.getPlatform().getACsLibErrorCategory(), DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS__ERRORS, DynamXErrorManager.MODEL_ERRORS, ACsGuiApi.getCssErrorType()))
            event.getButtonList().add(new GuiTexturedButton(-54391, event.getGui().width - 25, 5, 20, 20, TextFormatting.GOLD + "DynamX loading errors" + TextFormatting.RESET, new ResourceLocation(DynamXConstants.ID, "textures/mark.png")));
        else if (event.getGui() instanceof GuiMainMenu && DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.UPDATES)) //TODO MAJ INFO BUTTON
            event.getButtonList().add(new GuiButton(-54391, event.getGui().width / 2 - 110, event.getGui().height - 30, 220, 20, TextFormatting.AQUA + "Mise à jour DynamX disponible !" + TextFormatting.RESET));
        //else if (event.getGui() instanceof GuiWorldSelection || event.getGui() instanceof GuiMultiplayer)
        //    event.getButtonList().add(new GuiButtonImage(-54392, event.getGui().width - 25, 5, 20, 18, 0, 168, 19, CRAFTING_TABLE_GUI_TEXTURES));
    }

    @SubscribeEvent
    public void performMainMenuAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu && event.getButton().id == -54391)
            ACsGuiApi.asyncLoadThenShowGui("LoadingErrors", GuiLoadingErrors::new);
        /* else if ((event.getGui() instanceof GuiWorldSelection || event.getGui() instanceof GuiMultiplayer) && event.getButton().id == -54392) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(event.getGui(), "Improving DynamX", new TextComponentString("DynamX is collecting data about your computer (GPU, memory, OS) and crash-reports to improve the mod. \n" +
                    "You can disable this in the configuration file of DynamX (under 'config' directory)")));
        }*/
    }

    @SubscribeEvent
    public void drawHudCursor(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            Vector3fPool.openPool();
            GlStateManager.enableBlend();
            GameSettings gamesettings = MC.gameSettings;

            if (gamesettings.thirdPersonView == 0 && MC.player.getRidingEntity() == null && MC.objectMouseOver != null) {
                ResourceLocation loc = null;
                if (MC.objectMouseOver.entityHit instanceof BaseVehicleEntity) {
                    InteractivePart<?, ?> part = ((BaseVehicleEntity<?>) MC.objectMouseOver.entityHit).getHitPart(MC.player);
                    if (part != null) {
                        loc = part.getHudCursorTexture();
                    }
                } else if (MC.objectMouseOver.entityHit instanceof PropsEntity) {
                    loc = new ResourceLocation(DynamXConstants.ID, "textures/focus.png");
                }

                if (loc != null) {
                    event.setCanceled(true);
                    int l = event.getResolution().getScaledWidth();
                    int i1 = event.getResolution().getScaledHeight();

                    MC.getTextureManager().bindTexture(loc);

                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.enableAlpha();
                    Gui.drawModalRectWithCustomSizedTexture(l / 2 - 7, i1 / 2 - 7, 0, 0, 16, 16, 16, 16);
                }
            }
            Vector3fPool.closePool();
        } else if(event.getType() == RenderGameOverlayEvent.ElementType.ALL && !Minecraft.getMinecraft().isSingleplayer() && DynamXConfig.useUdp && (!DynamXContext.getNetwork().isConnected() || !DynamXContext.getNetwork().getQuickNetwork().isAuthenticated())) {
            String text = "DynamX: connecting to the server " + (DynamXContext.getNetwork().isConnected() ? "2/2" : "1/2");
            switch ((int)(Minecraft.getSystemTime() / 600L % 3L))
            {
                case 0:
                default:
                    text += ".  ";
                    break;
                case 1:
                    text += " . ";
                    break;
                case 2:
                    text += "  .";
                    break;
            }
            MC.fontRenderer.drawString(text, event.getResolution().getScaledWidth() - MC.fontRenderer.getStringWidth(text) - 4, 4, 0xFFFFFFFF);
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    /* Network events */

    private static long connectionTime = -1;

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        connectionTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DynamXContext.getNetwork().stopNetwork();
        connectionTime = -1;
    }

    /* Sound events */

    @SubscribeEvent
    public void onSoundSystemSetup(SoundSetupEvent event) {
        ClientProxy.SOUND_HANDLER.setup(event);
    }

    @SubscribeEvent
    public void onSoundSystemLoad(SoundLoadEvent event) {
        ClientProxy.SOUND_HANDLER.load(event);
    }

    /* Tick/render events */

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        ClientProxy.SOUND_HANDLER.tick();

        if (connectionTime != -1 && !Minecraft.getMinecraft().isSingleplayer() && DynamXConfig.useUdp) {
            if ((System.currentTimeMillis() - connectionTime) > 30000) {
                if (!DynamXContext.getNetwork().isConnected()) {
                    DynamXMain.log.warn("Failed to establish an UDP connection : timed out (0x1)");
                    if (Minecraft.getMinecraft().getConnection() != null)
                        Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(new TextComponentString("DynamX UDP connection timed out (Auth not started)"));
                } else
                    connectionTime = -1;
            }
        }

        model = null;
        EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
        if (entityPlayer != null) {
            if(DynamXContext.getWalkingPlayers().containsKey(entityPlayer)){
                PhysicsEntity<?> physicsEntity = DynamXContext.getWalkingPlayers().get(entityPlayer);
                if(!physicsEntity.canPlayerStandOnTop()){
                    if(WalkingOnPlayerController.controller != null) {
                        WalkingOnPlayerController.controller.disable();
                        entityPlayer.motionY += 0.2D;
                    }
                }
            }

            ItemStack currentItem = entityPlayer.inventory.getCurrentItem();
            if (currentItem.getItem() instanceof ItemBlock && ((ItemBlock) currentItem.getItem()).getBlock() instanceof DynamXBlock) {
                ItemBlock itemBlock = (ItemBlock) currentItem.getItem();
                DynamXBlock<?> block = (DynamXBlock<?>) itemBlock.getBlock();
                RayTraceResult target = Minecraft.getMinecraft().objectMouseOver;
                if (target != null && target.typeOfHit == RayTraceResult.Type.BLOCK) {
                    EnumFacing side = target.sideHit;
                    playerOrientation = MathHelper.floor((entityPlayer.rotationYaw * 16.0F / 360.0F) + 0.5D) & 0xF;
                    blockPos = new BlockPos(target.getBlockPos().getX() + side.getXOffset(),
                            target.getBlockPos().getY() + side.getYOffset(),
                            target.getBlockPos().getZ() + side.getZOffset());

                    blockObjectInfo = block.blockObjectInfo;
                    this.canPlace = itemBlock.canPlaceBlockOnSide(entityPlayer.world, blockPos, side, entityPlayer, currentItem);
                    this.model = DynamXContext.getObjModelRegistry().getModel(block.blockObjectInfo.getModel());
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (this.model != null) {
            GlStateManager.enableAlpha();
            model.renderPreview(blockObjectInfo, event.getPlayer(), blockPos, canPlace, playerOrientation, event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void onEntityCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (event.getEntity().getRidingEntity() instanceof BaseVehicleEntity)
            CameraSystem.rotateVehicleCamera(event);
    }

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        double partialTicks = event.getPartialTicks();

        float x = (float) (MC.player.lastTickPosX + (MC.player.posX - MC.player.lastTickPosX) * partialTicks);
        float y = (float) (MC.player.lastTickPosY + (MC.player.posY - MC.player.lastTickPosY) * partialTicks);
        float z = (float) (MC.player.lastTickPosZ + (MC.player.posZ - MC.player.lastTickPosZ) * partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.translate(-x, -y, -z);

        Vector3fPool.openPool();
        GlQuaternionPool.openPool();
        QuaternionPool.openPool();
        {
            RenderMovableLine.renderLine(event.getPartialTicks());
        }
        Vector3fPool.closePool();
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        if (ClientDebugSystem.enableDebugDrawing) {
            if (DynamXDebugOptions.CAMERA_RAYCAST.isActive()) {
                CameraSystem.drawDebug();
            }
        }

        boolean setup = false;
        Entity entity = MC.getRenderViewEntity();
        ICamera icamera = null;
        double d0 = 0, d1 = 0, d2 = 0;

        for (PhysicsEntity<?> entityp : MC.world.getEntities(PhysicsEntity.class, e -> true)) {
            if (!entityp.wasRendered && entityp.isRidingOrBeingRiddenBy(entity)) {
                if (!setup) {
                    GlStateManager.pushMatrix();
                    RenderHelper.enableStandardItemLighting();
                    MC.entityRenderer.enableLightmap();
                    icamera = new Frustum();

                    d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
                    d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
                    d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
                    icamera.setPosition(d0, d1, d2);

                    d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
                    d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
                    d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
                    setup = true;
                }
                tryRenderEntity(entityp, icamera, d0, d1, d2, (float) partialTicks);
            }
            entityp.wasRendered = false;
        }
        if (setup) {
            MC.entityRenderer.disableLightmap();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    private static void tryRenderEntity(Entity entity2, ICamera camera, double d0, double d1, double d2, float partialTicks) {
        boolean flag = MC.getRenderManager().shouldRender(entity2, camera, d0, d1, d2) || entity2.isRidingOrBeingRiddenBy(MC.player);

        if (flag) {
            boolean flag1 = MC.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase) MC.getRenderViewEntity()).isPlayerSleeping();

            if ((entity2 != MC.getRenderViewEntity() || MC.gameSettings.thirdPersonView != 0 || flag1) && (entity2.posY < 0.0D || entity2.posY >= 256.0D || MC.world.isBlockLoaded(entity2.getPosition()))) {
                MC.getRenderManager().renderEntityStatic(entity2, partialTicks, false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerRender(RenderPlayerEvent.Pre event) {
        renderPlayer = event.getRenderer();
        if (event.getEntityPlayer().getRidingEntity() instanceof BaseVehicleEntity && !rendering && event.getRenderer().getRenderManager().isRenderShadow()) { //If shadows are disabled, were are in GuiInventory, CAN BREAK OTHER MODS
            //If the player is on a seat, and GlobalRender isn't rendering players riding the entity, just cancel the event, and cancel all modifications by other mods (priority = EventPriority.HIGHEST)
            event.setCanceled(true);
        }
    }
}
