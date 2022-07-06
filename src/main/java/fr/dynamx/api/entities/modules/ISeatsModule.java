package fr.dynamx.api.entities.modules;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Base of a seat module, all of these methods are called by the {@link BaseVehicleEntity}, if {@link IModuleContainer.ISeatsContainer} is implemented
 */
public interface ISeatsModule extends IPhysicsModule
{
    @Nullable
    /**
     * The entity controlling this
     */
    Entity getControllingPassenger();

    /**
     * Called to update a passenger's position
     */
    void updatePassenger(Entity passenger);

    /**
     * Called to update a passenger's orientation
     */
    void applyOrientationToEntity(Entity passenger);

    /**
     * Client only
     * @return True if Minecraft.getMinecraft().player is controlling this
     */
    @SideOnly(Side.CLIENT)
    boolean isLocalPlayerDriving();

    /**
     * @return The last seat dismounted by an entity, used to update it's dismount position
     */
    PartSeat getLastRiddenSeat();

    /**
     * Used for network sync
     */
    Map<PartSeat, EntityPlayer> getSeatToPassengerMap();

    /**
     * Called when a {@link MessagePhysicsEntitySync} is received, to update seats on client side
     */
    void updateSeats(MessageSeatsSync msg, PhysicsEntityNetHandler<?> netHandler);

    /**
     * @return True if the player is riding the entity
     */
    boolean isPlayerSitting(EntityPlayer player);

    PartSeat getRidingSeat(Entity entity);
}
