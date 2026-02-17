package fr.dynamx.core.client.handlers;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.core.client.ClientProxy;
import fr.dynamx.core.client.camera.CameraSystem;
import fr.dynamx.core.client.gui.*;
import fr.dynamx.core.client.renders.RenderMovableLine;
import fr.dynamx.core.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.contentpack.type.objects.BlockObject;
import fr.dynamx.core.common.core.mixin.MixinRenderGlobal;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.items.DynamXItemSpawner;
import fr.dynamx.core.common.items.tools.ItemSlopes;
import fr.dynamx.core.common.network.packets.MessageEntityInteract;
import fr.dynamx.core.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.core.common.slopes.GuiSlopesConfig;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.core.utils.optimization.GlQuaternionPool;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.forge.DynamXConfig;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import fr.hermes.api.events.HmEventResult;
import fr.hermes.api.mc.client.HmScreen;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.events.HmMcClientEvents;
import fr.hermes.api.mc.events.HmPlayerEvents;
import fr.hermes.api.mc.events.HmWorldEvents;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import fr.hermes.api.platform.HermesPlatform;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

public class ClientEventHandler {
    public static final HmMinecraftClient MC = HermesPlatform.getInstance().getClient();
    public static UUID renderingEntity;
    /**
     * There are two choices: <br/>
     * - When Optifine is enabled with shaders, the {@link MixinRenderGlobal} renders the big entities behind the player's chunk <br>
     * - When there is no Optifine/shaders are disabled, the big entities are rendered in the RenderWorldLast event, here
     */
    public static boolean isRenderingEntitiesWithOptifineShaders;

    /* Placing block */
    private static DxModelRenderer model;
    private static boolean canPlace;
    private static BlockPos blockPos;
    private static int playerOrientation;
    private static BlockObject<?> blockObjectInfo;
    private static int textureNum;

    public static void register() {
        HmWorldEvents.UNLOAD.register(world -> {
            if (world.hm$isClient()) {
                ClientProxy.SOUND_HANDLER.unload();
            }
            DynamXDebugOptions.PROFILING.disable();
        });

        HmPlayerEvents.RIGHT_CLICK_ENTITY.register((player, hand, target) -> {
            if (!player.hm$getWorld().hm$isClient()) {
                return HmEventResult.pass();
            }
            if (!(HmEntityLogicMatcher.is(target, PhysicsEntity.class) || !hand.equals(EnumHand.MAIN_HAND) || player.hm$getHeldItem(hand).hm$getItem() instanceof DynamXItemSpawner)) {
                return HmEventResult.pass();
            }
            DynamXContext.getNetwork().sendToServer(new MessageEntityInteract(target.hm$getEntityId()));
            return HmEventResult.success();
        });

        HmPlayerEvents.RIGHT_CLICK_ITEM.register((player, hand, itemStack) -> {
            if (player.hm$getWorld().hm$isClient() && (MC.hm$getObjectMouseOver() == null || MC.hm$getObjectMouseOver().hm$getType() == HmRayTraceResult.Type.MISS) && player.hm$isSneaking() && itemStack.hm$getItem() instanceof ItemSlopes) {
                MC.hm$displayGuiScreen((HmScreen) new GuiSlopesConfig(itemStack).getGuiScreen());
            }
        });

        HmMcClientEvents.TICK.register((phase) -> {
            ClientProxy.SOUND_HANDLER.tick();

            if (connectionTime != -1 && !MC.hm$isSingleplayer()) {
                if ((System.currentTimeMillis() - connectionTime) > 30000) {
                    connectionTime = -1;
                    if (!DynamXContext.getNetwork().isConnected()) {
                        DynamXMain.log.fatal("Failed to establish an TCP/UDP connection : timed out (0x1)");
                        if (DynamXConfig.doUdpTimeOut) {
                            MC.hm$disconnectPlayer("DynamX UDP connection timed out (Auth not started)");
                        }
                    }
                }
            }
    
            model = null;
            HmClientPlayerEntity entityPlayer = MC.hm$getPlayer();
            if (entityPlayer == null) {
                return;
            }
            if (DynamXContext.getWalkingPlayers().containsKey(entityPlayer)) {
                PhysicsEntity<?> physicsEntity = DynamXContext.getWalkingPlayers().get(entityPlayer);
                if (!physicsEntity.canPlayerStandOnTop() && WalkingOnPlayerController.controller != null) {
                    WalkingOnPlayerController.controller.disable();
                    entityPlayer.hm$setMotionY(entityPlayer.hm$getMotionY() + 0.2f);
                }
            }
    
            HmItemStack currentItem = entityPlayer.hm$getHeldItemMainHand();
            // TODO Convert ItemBlock logic
            if (currentItem.hm$getItem() instanceof ItemBlock && ((ItemBlock) currentItem.hm$getItem()).getBlock() instanceof DynamXBlock) {
                ItemBlock itemBlock = (ItemBlock) currentItem.hm$getItem();
                DynamXBlock<?> block = (DynamXBlock<?>) itemBlock.getBlock();
                HmRayTraceResult target = MC.hm$getObjectMouseOver();
                if (target != null && target.hm$getType() == HmRayTraceResult.Type.BLOCK && block.isDxModel()) {
                    EnumFacing side = target.hm$getSide();
                    playerOrientation = MathHelper.floor((entityPlayer.hm$getRotationYaw() * 16.0F / 360.0F) + 0.5D) & 0xF;
                    blockPos = new BlockPos(target.hm$getBlockPos().getX() + side.getXOffset(),
                            target.hm$getBlockPos().getY() + side.getYOffset(),
                            target.hm$getBlockPos().getZ() + side.getZOffset());
    
                    textureNum = currentItem.hm$getMetadata();
                    blockObjectInfo = block.blockObjectInfo;
                    canPlace = itemBlock.canPlaceBlockOnSide(entityPlayer.hm$getWorld(), blockPos, side, entityPlayer, currentItem);
                    model = DynamXContext.getDxModelRegistry().getModel(block.blockObjectInfo.getModel());
                }
            }
        });

        HmPlayerEvents.CLIENT_CONNECTED_TO_SERVER.register(() -> {
            connectionTime = System.currentTimeMillis();
        });

        HmPlayerEvents.CLIENT_DISCONNECTED_FROM_SERVER.register(() -> {
            DynamXContext.getNetwork().stopNetwork();
            connectionTime = -1;
        });

        HmMcClientEvents.SOUND_SETUP.register((soundManager) -> {
            ClientProxy.SOUND_HANDLER.setup(soundManager);
        });

        HmMcClientEvents.SOUND_SYSTEM_LOAD.register(() -> {
            ClientProxy.SOUND_HANDLER.load();
        });

        HmMcClientEvents.DRAW_BLOCK_HIGHLIGHT.register((player, target, partialTicks) -> {
            if (model == null) {
                return;
            }
            GlStateManager.enableAlpha();
            GlQuaternionPool.openPool();
            QuaternionPool.openPool();
            model.renderPreview(blockObjectInfo, player, blockPos, canPlace, playerOrientation, partialTicks, textureNum);
            QuaternionPool.closePool();
            GlQuaternionPool.closePool();
        });

        HmMcClientEvents.CAMERA_SETUP.register((entity, yaw, pitch, roll, partialTicks) -> {
            if (HmEntityLogicMatcher.is(entity, PhysicsEntity.class)) {
                return CameraSystem.rotateVehicleCamera(entity, yaw, pitch, roll, partialTicks);
            }
            return HmEventResult.pass(null);
        });

        HmMcClientEvents.RENDER_WORLD_LAST.register((partialTicks) -> {
            if (RenderMovableLine.hasMovableLines()) {
                HmPlayerEntity player = MC.hm$getPlayer();
                float x = (float) (player.hm$getLastTickPosX() + (player.hm$getPosX() - player.hm$getLastTickPosX()) * partialTicks);
                float y = (float) (player.hm$getLastTickPosY() + (player.hm$getPosY() - player.hm$getLastTickPosY()) * partialTicks);
                float z = (float) (player.hm$getLastTickPosZ() + (player.hm$getPosZ() - player.hm$getLastTickPosZ()) * partialTicks);
                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.translate(-x, -y, -z);
    
                JmeVector3fPool.openPool();
                QuaternionPool.openPool();
                RenderMovableLine.renderLine(partialTicks);
                JmeVector3fPool.closePool();
                QuaternionPool.closePool();
    
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.enableLighting();
                GlStateManager.popMatrix();
            }
    
            if (ClientDebugSystem.enableDebugDrawing) {
                if (DynamXDebugOptions.CAMERA_RAYCAST.isActive()) {
                    CameraSystem.drawDebug();
                }
            }
            if (!isRenderingEntitiesWithOptifineShaders) {
                renderBigEntities(partialTicks);
            } else {
                isRenderingEntitiesWithOptifineShaders = false;
            }
        });

        HmMcClientEvents.PLAYER_RENDER_PRE.register((player, partialTicks, renderShadow) -> {
            if (HmEntityLogicMatcher.is( player.hm$getRidingEntity(), PhysicsEntity.class) && player.hm$getUniqueID() != renderingEntity && renderShadow) { //If shadows are disabled, were are in GuiInventory, CAN BREAK OTHER MODS
                //If the player is on a seat, and GlobalRender isn't rendering players riding the entity, just cancel the event, and cancel all modifications by other mods (priority = EventPriority.HIGHEST)
                return HmEventResult.cancel();
            }
            return HmEventResult.pass();
        });

        HmMcClientEvents.LIVING_ENTITY_RENDER_PRE.register((entity, partialTicks, renderShadow) -> {
            if (HmEntityLogicMatcher.is(entity.hm$getRidingEntity(), PhysicsEntity.class) && entity.hm$getUniqueID() != renderingEntity && renderShadow) { //If shadows are disabled, were are in GuiInventory, CAN BREAK OTHER MODS
                //If the entity is on a seat, and GlobalRender isn't rendering entity riding the entity, just cancel the event, and cancel all modifications by other mods (priority = EventPriority.HIGHEST)
                return HmEventResult.cancel();
            }
    
            // If the entity has a ragdoll, don't render it
            if (entity.hm$isInvisible() && DynamXContext.getPlayerToCollision().containsKey(entity) && DynamXContext.getPlayerToCollision().get(entity).ragdollEntity != null) {
                return HmEventResult.cancel();
            }
            return HmEventResult.pass();
        });
    }

    // === TODO DynamX events ===

    @SubscribeEvent
    public void onMount(VehicleEntityEvent.EntityMount event) {
        if (!(event.getEntityMounted() instanceof EntityPlayer) || !((EntityPlayer) event.getEntityMounted()).isUser()) {
            return;
        }
        ACsGuiApi.asyncLoadThenShowHudGui("Vehicle HUD", () -> new VehicleHud((IModuleContainer.ISeatsContainer) event.getEntity()));
    }

    @SubscribeEvent
    public void onDismount(VehicleEntityEvent.EntityDismount event) {
        if (!(event.getEntityDismounted() instanceof EntityPlayer) || !((EntityPlayer) event.getEntityDismounted()).isUser()) {
            return;
        }
        ACsGuiApi.closeHudGui(VehicleHud.class);
    }

    // === end ===

    /* Gui events */

    /* TODO error guis migration 
    
    @SubscribeEvent
    public void guiOpenEvent(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiMainMenu && DynamXMain.getInstance().getMemoizedLoadingError() != null) {
            DynamXMain.log.warn("Some errors occurred while loading DynamX content. Showing user a custom error screen.");
            CustomModLoadingErrorDisplayException custom = DynamXMain.getInstance().getMemoizedLoadingError().toCustomModLoadingErrorDisplayException("You can ignore this error but you may miss some 3D models.");
            event.setGui(new GuiMpsLoadingError(custom, (GuiMainMenu) event.getGui()));
        }
    }

    @SubscribeEvent
    public void initMainMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu && DynamXErrorManager.getErrorManager().hasErrors(ACsLib.getPlatform().getACsLibErrorCategory(), DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS, DynamXErrorManager.MODEL_ERRORS, ACsGuiApi.getCssErrorType(), ModProtectionSystem.getMpsErrorCategory()))
            event.getButtonList().add(new GuiTexturedButton(-54391, event.getGui().width - 25, 5, 20, 20, TextFormatting.GOLD + "DynamX loading errors" + TextFormatting.RESET, new ResourceLocation(DynamXConstants.ID, "textures/mark.png")));
        else if (event.getGui() instanceof GuiMainMenu && DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.UPDATES)) //TODO MAJ INFO BUTTON
            event.getButtonList().add(new GuiButton(-54391, event.getGui().width / 2 - 110, event.getGui().height - 30, 220, 20, TextFormatting.AQUA + "Mise à jour DynamX disponible !" + TextFormatting.RESET));
            //else if (event.getGui() instanceof GuiWorldSelection || event.getGui() instanceof GuiMultiplayer)
            //    event.getButtonList().add(new GuiButtonImage(-54392, event.getGui().width - 25, 5, 20, 18, 0, 168, 19, CRAFTING_TABLE_GUI_TEXTURES));
        else if (event.getGui() instanceof GuiScreenOptionsSounds) {
            int i = 1 + SoundCategory.values().length;
            event.getButtonList().add(new ButtonSlider(-54393, event.getGui().width / 2 - 155 + i % 2 * 160, event.getGui().height / 6 - 12 + 24 * (i >> 1), false, "DynamX Sounds" + TextFormatting.RESET));
        }
    }

    @SubscribeEvent
    public void performMainMenuAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu && event.getButton().id == -54391)
            ACsGuiApi.asyncLoadThenShowGui("LoadingErrors", GuiLoadingErrors::new);
        /* else if ((event.getGui() instanceof GuiWorldSelection || event.getGui() instanceof GuiMultiplayer) && event.getButton().id == -54392) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(event.getGui(), "Improving DynamX", new TextComponentString("DynamX is collecting data about your computer (GPU, memory, OS) and crash-reports to improve the mod. \n" +
                    "You can disable this in the configuration file of DynamX (under 'config' directory)")));
        }*/ /*
    }
    */

    /* TODO Hud cursor migration 
    @SubscribeEvent
    public void drawHudCursor(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            JmeVector3fPool.openPool(SubClassPool.CURSOR_HIT);
            QuaternionPool.openPool(SubClassPool.CURSOR_HIT);
            GlStateManager.enableBlend();
            GameSettings gamesettings = MC.gameSettings;

            if (gamesettings.thirdPersonView == 0 && MC.player.getRidingEntity() == null && MC.objectMouseOver != null) {
                ResourceLocation loc = null;
                if (MC.objectMouseOver.entityHit instanceof PackPhysicsEntity) {
                    InteractivePart part = ((PackPhysicsEntity<?, ?>) MC.objectMouseOver.entityHit).getHitPart(MC.player);
                    if (part != null && part.canInteract((IDynamXObject) MC.objectMouseOver.entityHit, MC.player)) {
                        loc = part.getHudCursorTexture();
                    } else if (MC.objectMouseOver.entityHit instanceof PropsEntity) {
                        loc = new ResourceLocation(DynamXConstants.ID, "textures/focus.png");
                    }
                } else if (MC.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                    TileEntity te = MC.world.getTileEntity(MC.objectMouseOver.getBlockPos());
                    if (te instanceof TEDynamXBlock) {
                        InteractivePart part = ((TEDynamXBlock) te).getHitPart(MC.player);
                        if (part != null && part.canInteract((IDynamXObject) te, MC.player)) {
                            loc = part.getHudCursorTexture();
                        }
                    }
                }

                if (loc != null) {
                    event.setCanceled(true);
                    int l = event.getResolution().getScaledWidth();
                    int i1 = event.getResolution().getScaledHeight();

                    MC.hm$textureManagerIsLoaded().bindTexture(loc);

                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.enableAlpha();
                    Gui.drawModalRectWithCustomSizedTexture(l / 2 - 7, i1 / 2 - 7, 0, 0, 16, 16, 16, 16);
                }
            }
            QuaternionPool.closePool();
            JmeVector3fPool.closePool();
        } else if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && !Minecraft.getMinecraft().isSingleplayer() && DynamXConfig.useUdp && (!DynamXContext.getNetwork().isConnected() || !DynamXContext.getNetwork().getQuickNetwork().isAuthenticated())) {
            String text = "DynamX: connecting to the server " + (DynamXContext.getNetwork().isConnected() ? "2/2" : "1/2");
            switch ((int) (Minecraft.getSystemTime() / 600L % 3L)) {
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

    /* Tick/render events */

    /**
     * This method is part of the big entities rendering fix <br>
     * It resets the wasRendered field of all DynamX entities to false
     */
    public static void resetBigEntities() {
        MC.hm$getWorld().hm$getEntityList().forEach(HmEntityLogicMatcher.consumer(PhysicsEntity.class, e -> e.wasRendered = false));
    }

    /**
     * This method is part of the big entities rendering fix <br>
     * It renders all the DynamX entities that weren't rendered by the normal renderer, but are in the camera frustum and SHOULD be rendered
     *
     * @param partialTicks The partial render ticks
     */
    public static void renderBigEntities(float partialTicks) {
        boolean setup = false;
        HmEntity renderViewEntity = MC.hm$getRenderViewEntity();
        if (renderViewEntity == null) {
            return;
        }
        ICamera icamera = null;
        double d0 = 0, d1 = 0, d2 = 0;

        for (HmEntity e : MC.hm$getWorld().hm$getEntityList()) {
            PhysicsEntity<?> physicsEntity = HmEntityLogicMatcher.cast(e, PhysicsEntity.class);
            if (physicsEntity == null || physicsEntity.wasRendered) {
                continue;
            }

            if (!setup) {
                GlStateManager.pushMatrix();
                RenderHelper.enableStandardItemLighting();
                MC.getHmRenderApi().enableLightmap();
                icamera = new Frustum();

                d0 = renderViewEntity.hm$getLastTickPosX() + (renderViewEntity.hm$getPosX() - renderViewEntity.hm$getLastTickPosX()) * partialTicks;
                d1 = renderViewEntity.hm$getLastTickPosY() + (renderViewEntity.hm$getPosY() - renderViewEntity.hm$getLastTickPosY()) * partialTicks;
                d2 = renderViewEntity.hm$getLastTickPosZ() + (renderViewEntity.hm$getPosZ() - renderViewEntity.hm$getLastTickPosZ()) * partialTicks;
                icamera.setPosition(d0, d1, d2);

                d0 = renderViewEntity.hm$getPrevPosX() + (renderViewEntity.hm$getPosX() - renderViewEntity.hm$getPrevPosX()) * partialTicks;
                d1 = renderViewEntity.hm$getPrevPosY() + (renderViewEntity.hm$getPosY() - renderViewEntity.hm$getPrevPosY()) * partialTicks;
                d2 = renderViewEntity.hm$getPrevPosZ() + (renderViewEntity.hm$getPosZ() - renderViewEntity.hm$getPrevPosZ()) * partialTicks;
                setup = true;
            }
            if (!MC.getHmRenderApi().shouldRender(e, icamera, d0, d1, d2) && !e.hm$isRidingOrBeingRiddenBy(MC.hm$getPlayer())) {
                continue;
            }
            boolean flag1 = renderViewEntity instanceof EntityLivingBase && ((EntityLivingBase) renderViewEntity).isPlayerSleeping();
            if ((e != renderViewEntity || !MC.hm$getGameSettings().hm$isFirstPersonView() || flag1) && (e.hm$getPosY() < 0.0D || e.hm$getPosY() >= 256.0D || MC.hm$getWorld().hm$isBlockLoaded(e.hm$getPosition()))) {
                MC.getHmRenderApi().renderEntity(e, partialTicks);
            }
        }
        if (setup) {
            MC.getHmRenderApi().disableLightmap();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }
}
