package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartPropsContainer;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;

import java.util.ArrayList;
import java.util.List;

public class PropsContainerModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener {
    //TODO NEW SYNC IMPROVE THIS
    private final BaseVehicleEntity<?> entity;
    private final List<PartPropsContainer> containers;
    private final List<PhysicsEntity<?>> modifiedEntitiesCache = new ArrayList<>();

    public PropsContainerModule(BaseVehicleEntity<?> entity) {
        this.entity = entity;
        this.containers = entity.getPackInfo().getPartsByType(PartPropsContainer.class);
    }

    @Override
    public void updateEntity() {
        if (entity.ticksExisted % 20 == 0 && !modifiedEntitiesCache.isEmpty()) {
            modifiedEntitiesCache.removeIf(e -> {
                if (e.getDistance(entity) > 10) {
                    System.out.println("[DEV] Remove " + e + " : far from " + entity);
                    e.getSynchronizer().setSimulationHolder(SimulationHolder.SERVER, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onSetSimulationHolder(SimulationHolder simulationHolder, SimulationHolder.UpdateContext changeContext) {
        modifiedEntitiesCache.clear();
        for (PartPropsContainer container : containers) {
            Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(container.getPosition(), entity.physicsRotation);
            MutableBoundingBox rotatedSize = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0, 0, 0), container.getBox(), entity.physicsRotation);
            rotatedSize = rotatedSize.offset(pos);
            rotatedSize = rotatedSize.offset(entity.physicsPosition);
            List<PhysicsEntity> entityList = entity.world.getEntitiesWithinAABB(PhysicsEntity.class, rotatedSize.toBB(), ent -> ent != entity);
            if (!entityList.isEmpty()) {
                System.out.println("[DEV] Found " + entityList.size() + " to set sim holder " + simulationHolder + " from " + entity);
                for (PhysicsEntity<?> ent : entityList) {
                    System.out.println("[DEV] Set on " + ent);
                    ent.getSynchronizer().setSimulationHolder(simulationHolder, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE);
                    modifiedEntitiesCache.add(ent);
                }
            } else {
                System.out.println("[DEV] Found no entity to set sim holder " + simulationHolder + " from " + entity + " box is " + rotatedSize);
            }
        }
    }
}
