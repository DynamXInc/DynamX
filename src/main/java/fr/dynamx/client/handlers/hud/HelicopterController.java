package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class HelicopterController extends BaseController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    protected final HelicopterEngineModule engine;

    protected static boolean mouseLocked = true;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    @SideOnly(Side.CLIENT)
    public HelicopterController(BaseVehicleEntity<?> entity, HelicopterEngineModule engine) {
        super(entity, engine);
        this.engine = engine;
        while (KeyHandler.KEY_HELICOPTER_PITCH_FORWARD.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_PITCH_BACKWARD.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_LEFT.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_RIGHT.isPressed()) ;
        while (KeyHandler.KEY_LOCK_ROTATION.isPressed()) ;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void tickMouse(MouseEvent event) {
        if (mouseLocked && MC.player.getRidingEntity() instanceof HelicopterEntity && ((HelicopterEntity<?>) MC.player.getRidingEntity()).getSeats().isLocalPlayerDriving()) {
            HelicopterEngineModule engineModule = ((HelicopterEntity<?>) MC.player.getRidingEntity()).getModuleByType(HelicopterEngineModule.class);
            int invert = MC.gameSettings.invertMouse ? -1 : 1;
            engineModule.getRollControls().set(0, invert * event.getDx());
            engineModule.getRollControls().set(1, invert * event.getDy());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void updateControls() {
        HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
        if (engine.getEngineProperties() != null && engine != null) {
            if (KeyHandler.KEY_POWERUP.isPressed() && isEngineStarted) {
                engine.setPower(engine.getPower() + 0.05f);
            }
            if (KeyHandler.KEY_POWERDOWN.isPressed() && isEngineStarted) {
                engine.setPower(engine.getPower() - 0.05f);
            }
            if (KeyHandler.KEY_LOCK_ROTATION.isPressed()) {
                mouseLocked = !mouseLocked;
            }
            boolean rolling = false;
            if (KeyHandler.KEY_HELICOPTER_PITCH_FORWARD.isKeyDown()) {
                engine.getRollControls().set(1, -25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_PITCH_BACKWARD.isKeyDown()) {
                engine.getRollControls().set(1, 25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_YAW_LEFT.isKeyDown()) {
                engine.getRollControls().set(0, -25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_YAW_RIGHT.isKeyDown()) {
                engine.getRollControls().set(0, 25);
                rolling = true;
            }
            handbraking = !rolling && KeyHandler.KEY_HANDBRAKE.isKeyDown();

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
        GuiPanel speed = new GuiPanel();
        speed.setCssClass("speed_pane");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", s -> String.format(s, engine.isEngineStarted() ? (int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] : "--", "")).setCssId("engine_speed"));
        speed.add(new UpdatableGuiLabel("Power %f", s -> String.format(s, engine.getPower())).setCssId("engine_gear"));
        panel.add(speed);
        panel.add(new UpdatableGuiLabel("View locked %b", s -> String.format(s, mouseLocked)).setCssId("engine_gear"));
        //panel.add(new UpdatableGuiLabel("                             AngleFront %f", s -> String.format(s, HelicopterEnginePhysicsHandler.AngleFront)).setCssId("engine_gear"));
        panel.setCssId("engine_hud");
        return panel;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    public static boolean isMouseLocked() {
        return mouseLocked;
    }
}
