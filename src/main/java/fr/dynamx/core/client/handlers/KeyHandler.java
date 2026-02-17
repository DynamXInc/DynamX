package fr.dynamx.core.client.handlers;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.core.client.camera.CameraSystem;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.contentpack.parts.BasePartSeat;
import fr.dynamx.core.common.contentpack.parts.PartDoor;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.DoorsModule;
import fr.dynamx.core.common.entities.modules.MovableModule;
import fr.dynamx.core.common.entities.modules.movables.PickingObjectHelper;
import fr.dynamx.core.common.items.tools.ItemSlopes;
import fr.dynamx.core.common.items.tools.ItemWrench;
import fr.dynamx.core.common.network.packets.MessageChangeDoorState;
import fr.dynamx.core.common.network.packets.MessageDebugRequest;
import fr.dynamx.core.common.network.packets.MessagePickObject;
import fr.dynamx.core.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmModEntity;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import static fr.dynamx.core.client.handlers.ClientEventHandler.MC;

public class KeyHandler {
    public static final KeyBinding KEY_ENGINE_ON = new KeyBinding("key.startEngine", Keyboard.KEY_O, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_HANDBRAKE = new KeyBinding("key.brake", Keyboard.KEY_SPACE, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_SPEED_LIMITIER = new KeyBinding("key.speedlimit", Keyboard.KEY_J, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_LOCK_DOOR = new KeyBinding("key.toggleLockDoor", Keyboard.KEY_Y, "key.categories." + DynamXConstants.ID);

    public static final KeyBinding KEY_CAMERA_MODE = new KeyBinding("key.cammode", Keyboard.KEY_MULTIPLY, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_ZOOM_IN = new KeyBinding("key.camin", Keyboard.KEY_ADD, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_ZOOM_OUT = new KeyBinding("key.camout", Keyboard.KEY_SUBTRACT, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_WATCH_BEHIND = new KeyBinding("key.watchbehind", Keyboard.KEY_C, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_PICK_OBJECT = new KeyBinding("key.pickobject", Keyboard.KEY_V, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_TAKE_OBJECT = new KeyBinding("key.takeobject", Keyboard.KEY_X, "key.categories." + DynamXConstants.ID);

    public static final KeyBinding KEY_DEBUG = new KeyBinding("key.debug", -1, "key.categories." + DynamXConstants.ID);


    public static final KeyBinding KEY_POWERUP = new KeyBinding("key.powerup", Keyboard.KEY_CAPITAL, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_POWERDOWN = new KeyBinding("key.powerdown", Keyboard.KEY_LSHIFT, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_HELICOPTER_PITCH_FORWARD = new KeyBinding("key.helicopter_pitch_forward", Keyboard.KEY_NUMPAD8, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_HELICOPTER_PITCH_BACKWARD = new KeyBinding("key.helicopter_pitch_backward", Keyboard.KEY_NUMPAD5, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_HELICOPTER_YAW_LEFT = new KeyBinding("key.helicopter_yaw_left", Keyboard.KEY_NUMPAD4, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_HELICOPTER_YAW_RIGHT = new KeyBinding("key.helicopter_yaw_right", Keyboard.KEY_NUMPAD6, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding KEY_LOCK_ROTATION = new KeyBinding("key.lock_rotation", Keyboard.KEY_NUMPAD1, "key.categories." + DynamXConstants.ID);

    public static final KeyBinding KEY_ATTACH_TRAILER = new KeyBinding("key.attachTrailer", Keyboard.KEY_H, "key.categories." + DynamXConstants.ID);

    private int holdingDown;
    private boolean justPressed;

    public KeyHandler() {
        ClientRegistry.registerKeyBinding(KEY_HANDBRAKE);
        ClientRegistry.registerKeyBinding(KEY_ENGINE_ON);
        ClientRegistry.registerKeyBinding(KEY_SPEED_LIMITIER);
        ClientRegistry.registerKeyBinding(KEY_LOCK_DOOR);
        ClientRegistry.registerKeyBinding(KEY_ATTACH_TRAILER);

        ClientRegistry.registerKeyBinding(KEY_CAMERA_MODE);
        ClientRegistry.registerKeyBinding(KEY_ZOOM_IN);
        ClientRegistry.registerKeyBinding(KEY_ZOOM_OUT);
        ClientRegistry.registerKeyBinding(KEY_DEBUG);
        ClientRegistry.registerKeyBinding(KEY_WATCH_BEHIND);
        ClientRegistry.registerKeyBinding(KEY_PICK_OBJECT);
        ClientRegistry.registerKeyBinding(KEY_TAKE_OBJECT);
        ClientRegistry.registerKeyBinding(KEY_POWERUP);
        ClientRegistry.registerKeyBinding(KEY_POWERDOWN);

        ClientRegistry.registerKeyBinding(KEY_HELICOPTER_PITCH_FORWARD);
        ClientRegistry.registerKeyBinding(KEY_HELICOPTER_PITCH_BACKWARD);
        ClientRegistry.registerKeyBinding(KEY_HELICOPTER_YAW_LEFT);
        ClientRegistry.registerKeyBinding(KEY_HELICOPTER_YAW_RIGHT);
        ClientRegistry.registerKeyBinding(KEY_LOCK_ROTATION);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tick(TickEvent.ClientTickEvent event) {
        if ((MC.hm$getPlayer() != null) && (event.phase == TickEvent.Phase.START)) {
            if (WalkingOnPlayerController.controller != null && (MC.hm$getPlayer().hm$isRiding()
                    || MC.hm$getGameSettings().hm$isForwardKeyDown() || MC.hm$getGameSettings().hm$isBackKeyDown()
                    || MC.hm$getGameSettings().hm$isLeftKeyDown() || MC.hm$getGameSettings().hm$isRightKeyDown()
                    || MC.hm$getGameSettings().hm$isJumpKeyDown())) {
                WalkingOnPlayerController.controller.disable();
            }
            controlCamera();

            if (KEY_DEBUG.isPressed()) {
                Minecraft.getMinecraft().player.sendChatMessage("/dynamx debug_gui");
            }

            if (KEY_PICK_OBJECT.isKeyDown() && MC.hm$getPlayer().hm$getRidingEntity() == null && MC.hm$getPlayer().hm$getHeldItemMainHand().hm$isEmpty()) {
                if (!DynamXContext.getPlayerPickingObjects().containsKey(MC.hm$getPlayer().hm$getEntityId())) {
                    if (MC.hm$isSingleplayer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.PICK, 3), MC.hm$getPlayer());
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.PICK, 3)));
                    }
                }
            } else {
                //FIXME THIS MAY FIRED WHILE TAKING OBJECT
                if (DynamXContext.getPlayerPickingObjects().containsKey(MC.hm$getPlayer().hm$getEntityId())) {
                    if (MC.hm$isSingleplayer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNPICK), MC.hm$getPlayer());
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.UNPICK)));
                    }
                }
            }

            if (KEY_LOCK_DOOR.isPressed()) {
                HmEntity entity = MC.hm$getPlayer().hm$getRidingEntity();
                BaseVehicleEntity<?> vehicleEntity = HmEntityLogicMatcher.cast(entity, BaseVehicleEntity.class);
                if (entity instanceof BaseVehicleEntity && entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null) {
                    BasePartSeat<?, ?> seat = ((IModuleContainer.ISeatsContainer) entity).getSeats().getRidingSeat(MC.hm$getPlayer());
                    if (seat == null)
                        return;
                    PartDoor door = seat.getLinkedPartDoor();
                    if (door == null)
                        return;
                    DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState((BaseVehicleEntity<?>) entity, doors.getInverseCurrentState(door.getId()), door.getId()));
                }
            }

            if (MC.hm$getObjectMouseOver() != null) {
                HmEntity entityHit = MC.hm$getObjectMouseOver().hm$getEntity();
                if (KEY_TAKE_OBJECT.isKeyDown()) {
                    if (holdingDown == 0) {
                        if (MC.hm$getPlayer().hm$getRidingEntity() == null && MC.hm$getPlayer().hm$getHeldItemMainHand().hm$isEmpty() && !DynamXContext.getPlayerPickingObjects().containsKey(MC.hm$getPlayer().hm$getEntityId())) {
                            if (entityHit != null) {
                                justPressed = true;
                                if (MC.hm$isSingleplayer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.hm$getEntityId()), MC.hm$getPlayer());
                                } else {
                                    DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.hm$getEntityId())));
                                }
                            }
                        }
                    }
                    holdingDown++;
                } else {
                    if (holdingDown > 10) {
                        if (DynamXContext.getPlayerPickingObjects().containsKey(MC.hm$getPlayer().hm$getEntityId())) {
                            if (MC.hm$isSingleplayer()) {
                                PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown), MC.hm$getPlayer());
                            } else {
                                DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown)));
                            }
                        }
                        holdingDown = 0;
                    } else if (holdingDown > 0) {
                        if (!justPressed) {
                            if (DynamXContext.getPlayerPickingObjects().containsKey(MC.hm$getPlayer().hm$getEntityId())) {
                                if (MC.hm$isSingleplayer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNTAKE), MC.hm$getPlayer());
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
        HmEntity entity = MC.hm$getPlayer().hm$getRidingEntity();
        if (!(entity instanceof HmModEntity) || !(((HmModEntity) entity).getLogic() instanceof IModuleContainer.ISeatsContainer)) {
            return;
        }
        if (KEY_ZOOM_IN.isPressed()) {
            CameraSystem.changeCameraZoom(false);
        }
        if (KEY_ZOOM_OUT.isPressed()) {
            CameraSystem.changeCameraZoom(true);
        }
        if (KEY_CAMERA_MODE.isPressed()) {
            MC.getHmRenderApi().setOverlayMessage("Vehicle camera mode : " + CameraSystem.cycleCameraMode((IModuleContainer.ISeatsContainer) entity), true);
        }
        CameraSystem.setWatchingBehind(KEY_WATCH_BEHIND.isKeyDown());
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (MC.hm$getPlayer() == null || !MC.hm$getPlayer().hm$isSneaking()) {
            return;
        }
        if (MC.hm$getPlayer().hm$getHeldItemMainHand().hm$getItem() instanceof ItemWrench) {
            if (Mouse.getEventDWheel() != 0) {
                DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15816));
                event.setCanceled(true);
            }
        } else if (MC.hm$getPlayer().hm$getHeldItemMainHand().hm$getItem() instanceof ItemSlopes) {
            if (Mouse.getEventDWheel() != 0) {
                DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15815));
                event.setCanceled(true);
            }
        }
    }
}