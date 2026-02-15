package fr.dynamx.core.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.core.client.handlers.KeyHandler;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.core.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.utils.HmResourceLocation;
import fr.hermes.api.mod.McObjectBinder;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collections;
import java.util.List;

// TODO this annotation is miam
@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class HelicopterController extends BaseController {
    public static final HmResourceLocation STYLE = McObjectBinder.instance.newResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    protected final HelicopterEngineModule engine;


    public HelicopterController(BaseVehicleEntity<?> entity, HelicopterEngineModule engine) {
        super(entity, engine);
        this.engine = engine;
        while (KeyHandler.KEY_HELICOPTER_PITCH_FORWARD.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_PITCH_BACKWARD.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_LEFT.isPressed()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_RIGHT.isPressed()) ;
        while (KeyHandler.KEY_LOCK_ROTATION.isPressed()) ;
    }

    // TODO CONVERT
    @SubscribeEvent
    public static void tickMouse(MouseEvent event) {
        HelicopterEntity<?> helicopter = HmEntityLogicMatcher.cast(MC.hm$getPlayer().hm$getRidingEntity(), HelicopterEntity.class);
        if (HelicopterEntity.isMouseLocked() && helicopter != null && helicopter.getSeats().isEntityDriving(MC.hm$getPlayer())) {
            HelicopterEngineModule engineModule = helicopter.getModuleByType(HelicopterEngineModule.class);
            int invert = MC.hm$getGameSettings().hm$isInvertMouse() ? -1 : 1;
            engineModule.getRollControls().set(0, invert * event.getDx());
            engineModule.getRollControls().set(1, invert * event.getDy());
        }
    }

    @Override
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
                HelicopterEntity.setMouseLocked(!HelicopterEntity.isMouseLocked());
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

            // TODO EVENT MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.ControllerUpdate<>(entity, this));
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
    public GuiComponent createHud() {
        GuiPanel panel = new GuiPanel();
        GuiPanel speed = new GuiPanel();
        speed.setCssClass("speed_pane");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(engine.isEngineStarted() ? (int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] : "--", "")).setCssId("engine_speed"));
        speed.add(new UpdatableGuiLabel("Power %.2f", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(Math.abs(engine.getPower()))).setCssId("engine_gear"));
        panel.add(new UpdatableGuiLabel("View locked %b", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(HelicopterEntity.isMouseLocked())).setCssId("engine_gear"));
        panel.setCssId("engine_hud");
        panel.add(speed);
        return panel;
    }

    @Override
    public List<HmResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
