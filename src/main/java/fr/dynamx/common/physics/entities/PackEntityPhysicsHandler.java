package fr.dynamx.common.physics.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Physics handler of {@link PackPhysicsEntity} <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 * @param <A> The pack info type
 */
public abstract class PackEntityPhysicsHandler<A extends IPhysicsPackInfo, T extends PackPhysicsEntity<?, A>> extends EntityPhysicsHandler<T> implements IPackInfoReloadListener {

    @Getter
    protected A packInfo;

    public PackEntityPhysicsHandler(T entity) {
        super(entity);
        onPackInfosReloaded();
    }

    @Nullable
    @Override
    public Vector3f getCenterOfMass() {
        return getPackInfo().getCenterOfMass();
    }

    @Override
    public void onPackInfosReloaded() {
        packInfo = handledEntity.getPackInfo();
    }
}
