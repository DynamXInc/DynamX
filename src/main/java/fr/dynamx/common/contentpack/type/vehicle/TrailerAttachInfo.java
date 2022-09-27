package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;

/**
 * Info of the trailer attach point of a vehicle
 */
public class TrailerAttachInfo extends SubInfoType<ModularVehicleInfoBuilder> {
    @PackFileProperty(configNames = "AttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f trailerAttachPoint;
    @PackFileProperty(configNames = "AttachStrength", required = false)
    private final int trailerAttachStrength = 1000;

    public TrailerAttachInfo(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder partInfo) {
        if (trailerAttachPoint == null)
            throw new IllegalArgumentException("AttachPoint not configured ! In trailer of " + partInfo.toString());
        partInfo.addSubProperty(this);
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        modules.add(new TrailerAttachModule(entity, this));
    }

    public Vector3f getAttachPoint() {
        return trailerAttachPoint;
    }

    public int getAttachStrength() {
        return trailerAttachStrength;
    }

    @Override
    public String getName() {
        return "TrailerAttachInfo in " + getOwner().getName();
    }
}