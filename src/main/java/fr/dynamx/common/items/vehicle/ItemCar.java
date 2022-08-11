package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.ErrorManagerService;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.CaterpillarInfo;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemCar<T extends ModularVehicleInfo<?>> extends ItemModularEntity<T> {
    public ItemCar(T info) {
        super(info);
        if (info.getSubPropertyByType(EngineInfo.class) == null) {
            DynamXMain.log.error("Cannot determine type of " + info.getFullName() + " ! It's a car with no engine...");
            DynamXErrorManager.addError(getInfo().getPackName(), "config_error", ErrorManagerService.ErrorLevel.FATAL, getInfo().getName(),  "Missing engine config !");
        }
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new CarEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }

    public static <A extends ModularVehicleInfo<?>> ItemCar<A> getItemForCar(A info) {
        if (info.getPartsByType(PartWheel.class).isEmpty()) {
            DynamXMain.log.error("Cannot determine type of " + info.getFullName() + " ! It's a car with no wheels...");
            DynamXErrorManager.addError(info.getPackName(), "config_error", ErrorManagerService.ErrorLevel.FATAL, info.getName(),  "This car has no wheels !");
        }
        if (info.getSubPropertyByType(CaterpillarInfo.class) == null)
            return new ItemCar<>(info);
        else
            return new ItemCaterpillar<>(info);
    }
}
