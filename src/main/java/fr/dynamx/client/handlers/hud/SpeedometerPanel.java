package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

public class SpeedometerPanel extends CircleCounterPanel {
    private final CarController carController;

    public SpeedometerPanel(CarController carController, float scale, float maxRpm) {
        super(new ResourceLocation(DynamXConstants.ID, "textures/waw.png"), false, 300, 300, scale, maxRpm);
        this.carController = carController;
    }

    @Override
    public void tick() {
        super.tick();
        prevValue = value;
        //Don't use modified maxRpm here
        value = carController.engine.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * carController.entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class).getMaxRevs();
    }
}
