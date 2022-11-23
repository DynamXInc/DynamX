package fr.dynamx.common.network.sync.vars;

import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

public class AttachedDoorsSynchronizedVariable extends AttachedBodySynchronizedVariable<BaseVehicleEntity<?>> {
    //public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "attach/doors");

    @Override
    public AttachedBodySynchronizer getSynchronizer(BaseVehicleEntity<?> on) {
        return on.getModuleByType(DoorsModule.class);
    }
}
