package fr.dynamx.client.handlers;

import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.movables.PickingObjectHelper;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.network.packets.MessagePickObject;
import fr.dynamx.common.network.packets.MessagePlayerMountVehicle;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.function.Predicate;

import static fr.dynamx.client.handlers.ClientEventHandler.MC;

public class KeyHandler {
    public static final KeyBinding KEY_CAMERA_MODE = new KeyBinding("key.cammode", Keyboard.KEY_MULTIPLY, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_ZOOM_IN = new KeyBinding("key.camin", Keyboard.KEY_ADD, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_ZOOM_OUT = new KeyBinding("key.camout", Keyboard.KEY_SUBTRACT, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_WATCH_BEHIND = new KeyBinding("key.watchbehind", Keyboard.KEY_C, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_PICK_OBJECT = new KeyBinding("key.pickobject", Keyboard.KEY_V, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_TAKE_OBJECT = new KeyBinding("key.takeobject", Keyboard.KEY_X, "key.categories." + DynamXConstants.ID);

    public static final KeyBinding KEY_DEBUG = new KeyBinding("key.debug", -1, "key.categories." + DynamXConstants.ID);

    private final Minecraft mc;
    private int holdingDown;
    private boolean justPressed;

    public KeyHandler(Minecraft minecraft) {
        this.mc = minecraft;

        ClientRegistry.registerKeyBinding(KEY_CAMERA_MODE);
        ClientRegistry.registerKeyBinding(KEY_ZOOM_IN);
        ClientRegistry.registerKeyBinding(KEY_ZOOM_OUT);
        ClientRegistry.registerKeyBinding(KEY_DEBUG);
        ClientRegistry.registerKeyBinding(KEY_WATCH_BEHIND);
        ClientRegistry.registerKeyBinding(KEY_PICK_OBJECT);
        ClientRegistry.registerKeyBinding(KEY_TAKE_OBJECT);

        CarController.registerControls();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tick(TickEvent.ClientTickEvent event) {
        if ((mc.player != null) && (event.phase == TickEvent.Phase.START)) {
            //FIXME SOLO : SIMPLIFY CONDITION
            if (mc.player.getRidingEntity() instanceof BaseVehicleEntity && mc.isSingleplayer() && !DynamXConfig.clientOwnsPhysicsInSolo) {
                //INEFFICIENT BUT NOT USED ANYMORE
                ((BaseVehicleEntity<?>) mc.player.getRidingEntity()).getNetwork().onPrePhysicsTick(Profiler.get());
                ((BaseVehicleEntity<?>) mc.player.getRidingEntity()).getNetwork().onPostPhysicsTick(Profiler.get());
            }
            if (WalkingOnPlayerController.controller != null && (MC.player.isRiding() || MC.gameSettings.keyBindForward.isKeyDown() || MC.gameSettings.keyBindBack.isKeyDown() || MC.gameSettings.keyBindLeft.isKeyDown() || MC.gameSettings.keyBindRight.isKeyDown() || MC.gameSettings.keyBindJump.isKeyDown())) {
                WalkingOnPlayerController.controller.disable();
            }
            controlCamera();

            if (KEY_DEBUG.isPressed()) {
                Minecraft.getMinecraft().player.sendChatMessage("/dynamx debug_gui");
            }

            if (KEY_PICK_OBJECT.isKeyDown() && MC.player.getRidingEntity() == null && MC.player.getHeldItemMainhand().isEmpty()) {
                if (!DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getEntityId())) {
                    if (DynamXConfig.clientOwnsPhysicsInSolo && MC.isSingleplayer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.PICK, 3), MC.player);
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.PICK, 3)));
                    }
                }
            } else {
                if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getEntityId())) {
                    if (DynamXConfig.clientOwnsPhysicsInSolo && MC.isSingleplayer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNPICK), MC.player);
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.UNPICK)));
                    }
                }
            }

            if (MC.objectMouseOver != null) {
                Entity entityHit = MC.objectMouseOver.entityHit;
                if (KEY_TAKE_OBJECT.isKeyDown()) {
                    if (holdingDown == 0) {
                        if (MC.player.getRidingEntity() == null && MC.player.getHeldItemMainhand().isEmpty() && !DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getEntityId())) {
                            if (entityHit != null) {
                                justPressed = true;
                                if (DynamXConfig.clientOwnsPhysicsInSolo && MC.isSingleplayer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.getEntityId()), MC.player);
                                } else {
                                    DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.getEntityId())));
                                }
                            }
                        }
                    }
                    holdingDown++;
                } else {
                    if (holdingDown > 10) {
                        if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getEntityId())) {
                            if (DynamXConfig.clientOwnsPhysicsInSolo && MC.isSingleplayer()) {
                                PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown), MC.player);
                            } else {
                                DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown)));
                            }
                        }
                        holdingDown = 0;
                    } else if (holdingDown > 0) {
                        if (!justPressed) {
                            if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getEntityId())) {
                                if (DynamXConfig.clientOwnsPhysicsInSolo && MC.isSingleplayer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNTAKE), MC.player);
                                } else {
                                    DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.UNTAKE)));
                                }
                            }
                        } else {
                            justPressed = false;
                        }
                        holdingDown = 0;
                    }
                }
            }
        }
    }

    private void controlCamera() {
        if (KEY_ZOOM_IN.isPressed()) {
            CameraSystem.changeCameraZoom(false);
        }
        if (KEY_ZOOM_OUT.isPressed()) {
            CameraSystem.changeCameraZoom(true);
        }
        if (KEY_CAMERA_MODE.isPressed()) {
            mc.ingameGUI.setOverlayMessage("Vehicle camera mode : " + CameraSystem.cycleCameraMode(), true);
        }
        CameraSystem.setWatchingBehind(KEY_WATCH_BEHIND.isKeyDown());
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (MC.player != null) {
            if (MC.player.getHeldItemMainhand().getItem() instanceof ItemWrench) {
                if (MC.player.isSneaking()) {
                    if (Mouse.getEventDWheel() != 0) {
                        DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15816));
                        event.setCanceled(true);
                    }
                }
            } else if (MC.player.isSneaking() && MC.player.getHeldItemMainhand().getItem() instanceof ItemSlopes) {
                if (Mouse.getEventDWheel() != 0) {
                    DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15815));
                    event.setCanceled(true);
                }
            }
            //Door interact
            if (Mouse.isButtonDown(1)) {
                if (!justPressed) {
                    justPressed = true;
                    Predicate<EnumBulletShapeType> predicateShape = p -> !p.isTerrain() && !p.isPlayer() && p != EnumBulletShapeType.VEHICLE;
                    PhysicsRaycastResult raycastResult = DynamXUtils.castRayFromEntity(mc.player, 2, predicateShape);
                    if (raycastResult == null)
                        return;
                    if (raycastResult.hitBody == null)
                        return;
                    Object userObject = raycastResult.hitBody.getUserObject();
                    if (!(userObject instanceof BulletShapeType)) {
                        return;
                    }
                    if (!(((BulletShapeType<?>) userObject).getObjectIn() instanceof DoorsModule.DoorVarContainer)) {
                        return;
                    }
                    DoorsModule.DoorVarContainer doorContainer = (DoorsModule.DoorVarContainer) ((BulletShapeType<?>) userObject).getObjectIn();
                    byte doorID = doorContainer.getDoorID();
                    DoorsModule doorsModule = doorContainer.getModule();
                    DynamXContext.getNetwork().sendToServer(new MessagePlayerMountVehicle(doorsModule.vehicleEntity.getEntityId(), doorID));
                }
            } else {
                justPressed = false;
            }
        }
    }
}