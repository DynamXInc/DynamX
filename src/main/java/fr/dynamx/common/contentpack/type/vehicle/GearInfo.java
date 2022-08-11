package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

/**
 * Automatic gear of an {@link EngineInfo}
 */
@RegisteredSubInfoType(name = "gear", registries = SubInfoTypeRegistries.ENGINES, strictName = false)
public class GearInfo extends SubInfoType<EngineInfo> {
    private byte id;
    private final String gearName;

    @PackFileProperty(configNames = "SpeedRange")
    private final int[] speedRange = new int[2];
    @PackFileProperty(configNames = "RPMRange")
    private final int[] rpmRange = new int[2];

    public GearInfo(ISubInfoTypeOwner<EngineInfo> owner, String name) {
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
    public void appendTo(EngineInfo owner) {
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
