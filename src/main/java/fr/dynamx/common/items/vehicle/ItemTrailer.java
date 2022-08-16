package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemTrailer<T extends ModularVehicleInfo<?>> extends ItemModularEntity<T> {
    public ItemTrailer(T modularVehicleInfo) {
        super(modularVehicleInfo);
        if (getInfo().getPartsByType(PartWheel.class).isEmpty()) {
            //DynamXMain.log.error("Cannot determine type of " + getInfo().getFullName() + " ! It's a trailer with no wheels...");
            DynamXErrorManager.addError(getInfo().getPackName(), "config_error", ErrorLevel.FATAL, getInfo().getName(),  "This trailer has no wheels !");
        }
        if (getInfo().getSubPropertyByType(TrailerAttachInfo.class) == null) {
            //DynamXMain.log.error("Cannot determine type of " + getInfo().getFullName() + " ! It's a trailer with no trailer attach...");
            DynamXErrorManager.addError(getInfo().getPackName(), "config_error", ErrorLevel.FATAL, getInfo().getName(),  "Missing trailer config !");
        }
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new TrailerEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }
}
