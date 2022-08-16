package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemBoat<T extends ModularVehicleInfo<?>> extends ItemModularEntity<T> {
    public ItemBoat(T modularVehicleInfo) {
        super(modularVehicleInfo);
        if (getInfo().getSubPropertyByType(BoatEngineInfo.class) == null) {
            //DynamXMain.log.error("Cannot determine type of " + getInfo().getFullName() + " ! It's a boat with no boat_engine...");
            DynamXErrorManager.addError(getInfo().getPackName(), "config_error", ErrorLevel.FATAL, getInfo().getName(), "Missing boat_engine config !");
        }
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new BoatEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }
}
