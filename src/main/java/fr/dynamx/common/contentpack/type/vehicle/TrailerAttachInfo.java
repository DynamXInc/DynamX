package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import lombok.Getter;

/**
 * Info of the trailer attach point of a vehicle
 */
@Getter
@RegisteredSubInfoType(name = "trailer", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class TrailerAttachInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "AttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    protected Vector3f attachPoint;
    @PackFileProperty(configNames = "AttachStrength", required = false)
    protected int attachStrength = 1000;
    @PackFileProperty(configNames = "AttachSound", required = false)
    protected String attachSound;

    public TrailerAttachInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        if (attachPoint == null)
            throw new IllegalArgumentException("AttachPoint not configured ! In trailer of " + owner.toString());
        owner.addSubProperty(this);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        modules.add(new TrailerAttachModule((BaseVehicleEntity<?>) entity, this));
    }

    @Override
    public String getName() {
        return "TrailerAttachInfo";
    }
}