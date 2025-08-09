package fr.dynamx.core.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.core.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.vehicles.TrailerEntity;
import fr.dynamx.core.common.items.ItemModularEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemTrailer extends ItemModularEntity {
    public ItemTrailer(ModularVehicleInfo modularVehicleInfo) {
        super(modularVehicleInfo);
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new TrailerEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }
}
