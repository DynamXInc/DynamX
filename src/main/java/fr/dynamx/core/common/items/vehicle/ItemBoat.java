package fr.dynamx.core.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.core.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.vehicles.BoatEntity;
import fr.dynamx.core.common.items.ItemModularEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemBoat extends ItemModularEntity {
    public ItemBoat(ModularVehicleInfo modularVehicleInfo) {
        super(modularVehicleInfo);
    }

    @Override
    public BaseVehicleEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new BoatEntity<>(getInfo().getFullName(), worldIn, pos.subtractLocal(0,1,0), spawnRotation, metadata);
    }
}
