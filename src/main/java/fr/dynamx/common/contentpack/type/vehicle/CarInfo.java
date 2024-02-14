package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.registry.PackFileProperty;
import lombok.Getter;

@Getter
public class CarInfo extends ModularVehicleInfo
{
    @PackFileProperty(configNames = "ReversingSound", required = false)
    protected String reversingSound;

    @PackFileProperty(configNames = "HandbrakeSoundOn", required = false)
    protected String handbrakeSoundOn;

    @PackFileProperty(configNames = "HandbrakeSoundOff", required = false)
    protected String handbrakeSoundOff;

    public CarInfo(String packName, String fileName) {
        super(packName, fileName, VehicleValidator.CAR_VALIDATOR);
    }
}
