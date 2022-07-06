package fr.dynamx.common.physics.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;

import javax.annotation.Nullable;

/**
 * Physics handler of {@link PackPhysicsEntity} <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 * @param <A> The pack info type
 */
public abstract class PackEntityPhysicsHandler<A extends IPhysicsPackInfo, T extends PackPhysicsEntity<?, A>> extends EntityPhysicsHandler<T> {
    protected A packInfo;

    public PackEntityPhysicsHandler(T entity) {
        super(entity);
        this.packInfo = entity.getPackInfo();
    }

    public A getPackInfo() {
        return packInfo;
    }

    @Nullable
    @Override
    public Vector3f getCenterOfMass() {
        return getPackInfo().getCenterOfMass();
    }
}
