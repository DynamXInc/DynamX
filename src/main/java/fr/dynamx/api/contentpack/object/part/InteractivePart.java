package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Part of a that allows interaction (with a bounding box)
 */
public abstract class InteractivePart<A extends PhysicsEntity<?>, T extends ISubInfoTypeOwner<T>> extends BasePart<T> {
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

    public ResourceLocation getHudCursorTexture() {
        return null;
    }

    /**
     * Handles interaction with this part
     *
     * @return True if interacted with success
     */
    public abstract boolean interact(A entity, EntityPlayer with);
}
