package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;
import lombok.Setter;

/**
 * Automatic gear of an {@link CarEngineInfo}
 */
@Getter
@RegisteredSubInfoType(name = "gear", registries = SubInfoTypeRegistries.CAR_ENGINES, strictName = false)
public class GearInfo extends SubInfoType<BaseEngineInfo> {
    @Setter
    private byte id;
    private final String gearName;

    @PackFileProperty(configNames = "SpeedRange")
    private int[] speedRange = new int[2];
    @PackFileProperty(configNames = "RPMRange")
    private int[] rpmRange = new int[2];
    @PackFileProperty(configNames = {"GearRatio", "Ratio" }, defaultValue = "1")
    private float gearRatio = 1;

    public GearInfo(ISubInfoTypeOwner<BaseEngineInfo> owner, String name) {
        super(owner);
        this.gearName = name;
    }

    @Override
    public void appendTo(BaseEngineInfo owner) {
        owner.addGear(this);
    }

    @Override
    public String getName() {
        return "Gear_" + getGearName();
    }
}
