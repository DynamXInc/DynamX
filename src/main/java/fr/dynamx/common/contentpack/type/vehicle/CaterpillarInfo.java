package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;

import javax.annotation.Nullable;

@RegisteredSubInfoType(name = "caterpillar", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class CaterpillarInfo implements ISubInfoType<ModularVehicleInfoBuilder> {
    private final ModularVehicleInfoBuilder owner;

    //caterpillar things
    public boolean caterpillar;

    @PackFileProperty(configNames = "LeftPoses")
    public Vector3f[] caterpillarLeftBuffer;
    @PackFileProperty(configNames = "RightPoses")
    public Vector3f[] caterpillarRightBuffer;
    @PackFileProperty(configNames = "Width")
    public float caterpillarWidth;

    public CaterpillarInfo(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner) {
        this.owner = (ModularVehicleInfoBuilder) owner;
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        owner.addSubProperty(this);
    }

    @Nullable
    @Override
    public ModularVehicleInfoBuilder getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return "CaterpillarInfo in " + owner.getName();
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }
}
