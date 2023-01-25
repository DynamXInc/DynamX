package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

/**
 * Automatic gear of an {@link CarEngineInfo}
 */
@RegisteredSubInfoType(name = "gear", registries = SubInfoTypeRegistries.ENGINES, strictName = false)
public class GearInfo extends SubInfoType<CarEngineInfo> {
    private byte id;
    private final String gearName;

    @PackFileProperty(configNames = "SpeedRange")
    private int[] speedRange = new int[2];
    @PackFileProperty(configNames = "RPMRange")
    private int[] rpmRange = new int[2];

    public GearInfo(ISubInfoTypeOwner<CarEngineInfo> owner, String name) {
        super(owner);
        this.gearName = name;
    }

    public int[] getSpeedRange() {
        return speedRange;
    }

    public int[] getRpmRange() {
        return rpmRange;
    }

    @Override
    public void appendTo(CarEngineInfo owner) {
        owner.addGear(this);
    }

    @Override
    public String getName() {
        return "Gear_" + getGearName();
    }

    public byte getId() {
        return id;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public String getGearName() {
        return gearName;
    }
}
