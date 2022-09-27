package fr.dynamx.common.entities.modules;

import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.ResourceLocation;

public class TrailerAttachModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, AttachModule.AttachToOtherModule<TrailerEntity<?>> {
    public static final ResourceLocation JOINT_NAME = new ResourceLocation(DynamXConstants.ID, "trailer_module");
    public static final JointHandler<CarEntity<?>, TrailerEntity<?>, TrailerAttachModule> HANDLER;

    static {
        JointHandlerRegistry.register(HANDLER = new JointHandler(JOINT_NAME, CarEntity.class, TrailerEntity.class, TrailerAttachModule.class));
    }

    private final BaseVehicleEntity<?> entity;
    private final TrailerAttachInfo info;
    private int connectedEntity = -1;

    public TrailerAttachModule(BaseVehicleEntity<?> entity, TrailerAttachInfo info) {
        this.entity = entity;
        this.info = info;
    }

    public Vector3f getAttachPoint() {
        return info.getAttachPoint();
    }

    public int getConnectedEntity() {
        return connectedEntity;
    }

    @Override
    public Constraint createJoint(TrailerEntity<?> trailer, byte jointId) {
        Vector3f p1 = Vector3fPool.get(info.getAttachPoint());
        Vector3f p2 = Vector3fPool.get(trailer.getModuleByType(TrailerAttachModule.class).getAttachPoint());
        p1.addLocal(entity.getPackInfo().getCenterOfMass());
        p2.addLocal(trailer.getPackInfo().getCenterOfMass());
        Point2PointJoint joint = new Point2PointJoint(entity.physicsHandler.getCollisionObject(), trailer.physicsHandler.getCollisionObject(), p1, p2);
        joint.setBreakingImpulseThreshold(trailer.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class).getAttachStrength());

        connectedEntity = trailer.getEntityId();
        trailer.getModuleByType(TrailerAttachModule.class).connectedEntity = entity.getEntityId();

        return joint;
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return connectedEntity == -1;
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        connectedEntity = -1;
    }
}
