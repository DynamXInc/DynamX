package fr.dynamx.api.contentpack.object.part;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public interface IDrawablePart<T extends ModularPhysicsEntity<?>> {
    /**
     * Called to update textures of this part (egg for wheels) according to the new entity's metadata
     *
     * @param entity The entity
     */
    @SideOnly(Side.CLIENT)
    default void onTexturesChange(T entity) {
    }

    /**
     * note : should render ALL parts of this type (called once per type of part)
     *
     * @param entity
     * @param render
     * @param packInfo
     * @param textureId
     * @param partialTicks
     * @param forceVanillaRender
     */
    @SideOnly(Side.CLIENT)
    void drawParts(@Nullable T entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks, boolean forceVanillaRender);

    /**
     * Prevents the added parts from being rendered with the main obj model of the vehicle <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     *
     * @return The parts to hide when rendering the main obj model
     */
    String[] getRenderedParts();
}
