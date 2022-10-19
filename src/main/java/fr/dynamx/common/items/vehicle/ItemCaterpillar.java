package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.CaterpillarEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemCaterpillar<T extends ModularVehicleInfo<T>> extends ItemCar<T> {
    public ItemCaterpillar(T modularVehicleInfo) {
        super(modularVehicleInfo);
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new CaterpillarEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }
}
