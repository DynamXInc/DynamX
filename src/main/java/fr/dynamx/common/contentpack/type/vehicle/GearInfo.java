package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;

/**
 * Automatic gear of an {@link EngineInfo}
 */
public class GearInfo extends SubInfoType<EngineInfo>
{
    private byte id;
    private final String gearName;

    @PackFileProperty(configNames = "SpeedRange")
    private int[] speedRange = new int[2];
    @PackFileProperty(configNames = "RPMRange")
    private int[] rpmRange = new int[2];

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
    public void appendTo(EngineInfo engineInfo) {
        engineInfo.addGear(this);
    }

    @Override
    public String getName() {
        return "Gear_"+ getGearName() +" in "+getOwner().getName();
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
