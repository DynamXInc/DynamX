package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import fr.dynamx.common.entities.modules.HelicopterPartModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class HelicopterController implements IVehicleController {
    //TODO CLEAN

    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");
    private static final Minecraft MC = Minecraft.getMinecraft();

    protected final BaseVehicleEntity<?> entity;
    protected final HelicopterEngineModule engine;

    public boolean accelerating, handbraking, reversing;
    public boolean turningLeft, turningRight, isEngineStarted;
    public byte onCooldown;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public HelicopterController(BaseVehicleEntity<?> entity, HelicopterEngineModule engine) {
        this.entity = entity;
        this.engine = engine;

        isEngineStarted = engine.isEngineStarted();
        handbraking = engine.isHandBraking();

        CameraSystem.setCameraZoom(entity.getPackInfo().getDefaultZoomLevel());

        while (CarController.car_brake.isPressed()) ;
        while (CarController.speedLimiter.isPressed()) ;
        while (CarController.car_engineOn.isPressed()) ;
        while (CarController.toggleLockDoor.isPressed()) ;
    }

    @SubscribeEvent
    public static void tickMouse(MouseEvent event) {
        if (MC.player.getRidingEntity() instanceof HelicopterEntity && ((HelicopterEntity<?>) MC.player.getRidingEntity()).getSeats().isLocalPlayerDriving()) {
            HelicopterEngineModule engineModule = ((HelicopterEntity<?>) MC.player.getRidingEntity()).getModuleByType(HelicopterEngineModule.class);
            engineModule.getRollControls().set(0, event.getDx());
            engineModule.getRollControls().set(1, event.getDy());
        }
    }

    @Override
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isLocalPlayerDriving() && engine.getEngineProperties() != null) {
            if (KeyHandler.KEY_POWERUP.isPressed()) {
                if (onCooldown == 0) {
                    HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
                    if (engine != null) {
                        engine.setPower(engine.getPower() + 0.05f);
                    }
                    onCooldown = 5;
                }
            }
            if (KeyHandler.KEY_POWERDOWN.isPressed()) {
                if (onCooldown == 0) {
                    HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
                    if (engine != null) {
                        engine.setPower(engine.getPower() - 0.05f);
                    }
                    onCooldown = 5;
                }
            }
            if (accelerating != MC.gameSettings.keyBindForward.isKeyDown()) {
                accelerating = MC.gameSettings.keyBindForward.isKeyDown();
            }
            if (reversing != MC.gameSettings.keyBindBack.isKeyDown()) {
                reversing = MC.gameSettings.keyBindBack.isKeyDown();
            }
            if (engine.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] == 0) //point mort
            {
                if (CarController.car_brake.isPressed()) {
                    handbraking = !handbraking;
                }
            } else if (handbraking != CarController.car_brake.isKeyDown()) {
                handbraking = CarController.car_brake.isKeyDown();
            }
            if (turningLeft != MC.gameSettings.keyBindLeft.isKeyDown() || turningRight != MC.gameSettings.keyBindRight.isKeyDown()) {
                turningLeft = MC.gameSettings.keyBindLeft.isKeyDown();
                turningRight = MC.gameSettings.keyBindRight.isKeyDown();
            }
            if (onCooldown > 0)
                onCooldown--;
            if (CarController.car_engineOn.isPressed()) {
                if (onCooldown == 0) {
                    isEngineStarted = !isEngineStarted;
                    onCooldown = 40;
                }
            }
            /* todo update this if (CarController.toggleLockDoor.isPressed()) {
                if (onCooldown == 0) {
                    if (entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null) {
                        PartSeat seat = ((IModuleContainer.ISeatsContainer) entity).getSeats().getRidingSeat(MC.player);
                        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
                        DynamXContext.getNetwork().sendToServer(new MessageOpenDoor(entity, !doors.isDoorOpened(seat.getLinkedPartDoor(entity))));
                    }
                    onCooldown = 30;
                }
            }*/

            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.ControllerUpdate<>(entity, this));
            int controls = 0;
            if (accelerating)
                controls = controls | 2;
            if (handbraking)
                controls = controls | 32;
            if (reversing)
                controls = controls | 4;
            if (turningLeft)
                controls = controls | 8;
            if (turningRight)
                controls = controls | 16;
            if (isEngineStarted)
                controls = controls | 1;
            engine.setControls(controls);
        }
    }

    //HUD

    @Override
    @SideOnly(Side.CLIENT)
    public GuiComponent<?> createHud() {
        GuiPanel panel = new GuiPanel();
        float scale = 90f / 300;
        GuiPanel speed = new GuiPanel();
        speed.setCssId("speed_pane");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", s -> String.format(s, engine.isEngineStarted() ? (int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] : "--", "")).setCssId("engine_speed"));
        // speed.add(new UpdatableGuiLabel("%d", s -> String.format(s, (int) (engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs()), "")).setCssId("engine_rpm"));
        panel.add(speed);

        speed.add(new UpdatableGuiLabel("power %f", s -> String.format(s, engine.getPower())).setCssId("engine_gear"));

        //panel.add(new UpdatableGuiLabel("                             AngleFront %f", s -> String.format(s, HelicopterEnginePhysicsHandler.AngleFront)).setCssId("engine_gear"));
        panel.setCssId("engine_hud");
        return panel;
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    protected String getGearString(int gear) {
        return gear == -1 ? "R" : gear == 0 ? (engine.isHandBraking() ? TextFormatting.RED + "P" : "N") : "" + gear;
    }
}
