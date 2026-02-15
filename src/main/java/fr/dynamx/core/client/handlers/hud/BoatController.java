package fr.dynamx.core.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.utils.HmResourceLocation;
import fr.hermes.api.mod.McObjectBinder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

public class BoatController extends BaseController {
    public static final HmResourceLocation STYLE = McObjectBinder.instance.newResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    @Getter
    @Setter
    private static HudIcons hudIcons;

    protected final BoatPropellerModule engine;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public BoatController(BaseVehicleEntity<?> entity, BoatPropellerModule engine) {
        super(entity, engine);
        this.engine = engine;
    }

    @Override
    protected void updateControls() {
        if (engine.getEngineProperties() != null) {
            //TODO EVENT MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.ControllerUpdate<>(entity, this));
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
        panel.add(speed);
        panel.setCssId("engine_hud");
        return panel;
    }

    @Override
    public List<HmResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
