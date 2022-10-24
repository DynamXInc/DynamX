package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemCar extends ItemModularEntity {
    public ItemCar(ModularVehicleInfo info) {
        super(info);
        if (info.getSubPropertyByType(EngineInfo.class) == null)
            DynamXErrorManager.addPackError(getInfo().getPackName(), "config_error", ErrorLevel.FATAL, getInfo().getName(), "Missing engine config !");
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new CarEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }

    public static ItemCar getItemForCar(ModularVehicleInfo info) {
        if (info.getPartsByType(PartWheel.class).isEmpty()) {
            DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no wheels !");
        }
        return new ItemCar(info);
    }
}
