package fr.dynamx.common.physics.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.physics.entities.modules.FloatPhysicsHandler;
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
public abstract class PackEntityPhysicsHandler<A extends IPhysicsPackInfo & IPartContainer<?>, T extends PackPhysicsEntity<?, A>> extends EntityPhysicsHandler<T> implements IPackInfoReloadListener {

    @Getter
    protected A packInfo;

    @Getter
    private List<FloatPhysicsHandler> floatList;
    @Getter
    private List<Vector3f> debugBuoyForces;
    @Getter
    private List<Vector3f> debugDragForces;

    @Getter
    private boolean isInWater;

    public PackEntityPhysicsHandler(T entity) {
        super(entity);
        onPackInfosReloaded();
    }


    @Override
    public void update() {
        super.update();
        float waterLevel = getWaterLevel();
        if (waterLevel == Float.MIN_VALUE) {
            if (!isInWater)
                return;
            isInWater = false;
            collisionObject.setLinearDamping(packInfo.getLinearDamping());
            collisionObject.setAngularDamping(packInfo.getAngularDamping());
            return;
        }
        if(!isInWater) {
            isInWater = true;
            collisionObject.setLinearDamping(packInfo.getInWaterLinearDamping());
            collisionObject.setAngularDamping(packInfo.getInWaterAngularDamping());
        }
        for (FloatPhysicsHandler floatPhysicsHandler : floatList) {
            floatPhysicsHandler.handleBuoyancy(waterLevel);
        }
    }

    @Nullable
    @Override
    public Vector3f getCenterOfMass() {
        return getPackInfo().getCenterOfMass();
    }

    @Override
    public void onPackInfosReloaded() {
        packInfo = handledEntity.getPackInfo();
        if (getCollisionObject() != null) {
            getCollisionObject().setAngularDamping(packInfo.getAngularDamping());
            getCollisionObject().setLinearDamping(packInfo.getLinearDamping());
            isInWater = false;
        }

        //Debug, to clean
        if (debugBuoyForces == null) {
            debugBuoyForces = new ArrayList<>();
        } else {
            debugBuoyForces.clear();
        }
        if (debugDragForces == null) {
            debugDragForces = new ArrayList<>();
        } else {
            debugDragForces.clear();
        }

        floatList = new ArrayList<>();
        List<PartFloat> partsByType = packInfo.getPartsByType(PartFloat.class);
        if (partsByType.isEmpty()) {
            FloatPhysicsHandler floatPhysicsHandler = new FloatPhysicsHandler(handledEntity);
            floatPhysicsHandler.setSize(0.5f);
            floatPhysicsHandler.setScale(new Vector3f(0.5f, 0.5f, 0.5f));
            floatPhysicsHandler.setBuoyCoefficient(2f);
            floatPhysicsHandler.setDragCoefficient(0.1f);
            debugDragForces.add(floatPhysicsHandler.getDebugDragForce());
            debugBuoyForces.add(floatPhysicsHandler.getDebugBuoyForce());
            floatList.add(floatPhysicsHandler);
            return;
        }
        floatList = new ArrayList<>(partsByType.size());
        for (PartFloat<?> partFloat : partsByType) {
            for (Vector3f floatCenter : partFloat.getChildrenPositionList()) {
                FloatPhysicsHandler floatPhysicsHandler = new FloatPhysicsHandler(handledEntity, partFloat, floatCenter);
                debugDragForces.add(floatPhysicsHandler.getDebugDragForce());
                debugBuoyForces.add(floatPhysicsHandler.getDebugBuoyForce());
                floatList.add(floatPhysicsHandler);
            }
        }
    }
}
