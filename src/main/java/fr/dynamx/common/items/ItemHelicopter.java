package fr.dynamx.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemHelicopter extends ItemModularEntity {
    public ItemHelicopter(ModularVehicleInfo info) {
        super(info);
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new HelicopterEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }

    public static ItemHelicopter getItemForCar(ModularVehicleInfo info) {
        return new ItemHelicopter(info);
    }
}
