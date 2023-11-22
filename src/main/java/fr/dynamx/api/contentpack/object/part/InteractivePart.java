package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Part of a that allows interaction (with a bounding box)
 */
public abstract class InteractivePart<A extends Entity, T extends ISubInfoTypeOwner<T>> extends BasePart<T> {
    private AxisAlignedBB box;

    public InteractivePart(T owner, String partName) {
        super(owner, partName);
    }

    public InteractivePart(T owner, String partName, float halfWidth, float halfHeight) {
        super(owner, partName, new Vector3f(halfWidth, halfHeight, halfWidth));
    }

    public InteractivePart(T owner, String partName, Vector3f halfBoxSize) {
        super(owner, partName, halfBoxSize);
    }

    public void setBox(AxisAlignedBB box) {
        this.box = box;
    }

    public void getBox(MutableBoundingBox out) {
        out.setTo(box);
    }

    @Override
    public void appendTo(T owner) {
        super.appendTo(owner);
        box = new AxisAlignedBB(-getScale().x, 0, -getScale().z, getScale().x, getScale().y, getScale().z);
    }

    /**
     * @return The texture to display on the cursor when the player is looking at this part
     */
    public ResourceLocation getHudCursorTexture() {
        return null;
    }

    /**
     * Handles interaction with this part
     *
     * @return True if interacted with success
     */
    public abstract boolean interact(A with, EntityPlayer player);

    /**
     * Checks if the player can interact with this part
     *
     * @param with The target entity. Null if the part is on a block and the method is fired to get the cursor to display on client.
     * @param player The interacting player
     * @return True if the player can interact with this part
     */
    public boolean canInteract(A with, EntityPlayer player) {
        return true;
    }
}
