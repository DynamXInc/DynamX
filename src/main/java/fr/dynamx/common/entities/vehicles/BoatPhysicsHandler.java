package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BoatPhysicsHandler<T extends BoatEntity<?>> extends BaseVehiclePhysicsHandler<T> implements IPackInfoReloadListener {
    public List<PartFloat> floatList;
    public List<Vector3f> buoyForces = new ArrayList<>();
    public List<Vector3f> dragForces = new ArrayList<>();

    public BoatPhysicsHandler(T entity) {
        super(entity);
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        floatList = packInfo.getPartsByType(PartFloat.class);
        //Debug, to clean
        buoyForces.clear();
        dragForces.clear();
        floatList.forEach(pf -> pf.childFloatsPos.forEach(p -> {
            buoyForces.add(new Vector3f());
            dragForces.add(new Vector3f());
        }));
    }

    @Override
    public IPropulsionHandler getPropulsion() {
        return getHandledEntity().getPropulsion().getPhysicsHandler(); //BOAT ENGINE
    }

    @Override
    public void update() {
        collisionObject.setAngularDamping(0.6f);
        collisionObject.setLinearDamping(0.6f);
        collisionObject.setEnableSleep(false);

        boolean isInLiquid = false;
        int liquidOffset = 0;
        float waterLevel;

        for (int offset = -1; offset <= 2; offset++) {
            BlockPos blockPos = new BlockPos(physicsPosition.x, physicsPosition.y + offset, physicsPosition.z);
            if (handledEntity.getEntityWorld().getBlockState(blockPos).getMaterial().isLiquid()) {
                liquidOffset = offset;
                isInLiquid = true;
            }
        }

        if (isInLiquid) {
            BlockPos blockPos = new BlockPos(physicsPosition.x, physicsPosition.y + liquidOffset, physicsPosition.z);
            AxisAlignedBB boundingBox = handledEntity.getEntityWorld().getBlockState(blockPos).getBoundingBox(handledEntity.getEntityWorld(), blockPos);
            waterLevel = (float) boundingBox.offset(blockPos).maxY - 0.125F + 0.5f;
            int i = 0;
            for (PartFloat partFloat : floatList) {
                for (Vector3f floatCenter : partFloat.childFloatsPos) {
                    handleBuoyancy(floatCenter, partFloat.dragCoefficient, partFloat.size, partFloat.getScale().y, waterLevel, i++);
                }
            }
        }
        super.update();
    }

    private void handleBuoyancy(Vector3f partPos, float dragCoefficient, float size, float floatYSize, float waterLevel, int i) {
        Vector3f localPosRotated = DynamXGeometry.rotateVectorByQuaternion(partPos, physicsRotation);
        Vector3f inWorldPos = physicsPosition.add(localPosRotated);

        double dy = waterLevel - inWorldPos.y;

        if (dy > 0) {
            float area = size * size;
            dy = Math.min(dy, floatYSize);
            Vector3f buoyForce = Vector3fPool.get(0, dy * area * DynamXPhysicsHelper.WATER_DENSITY * 9.81, 0);

            Vector3f rotatedFloaterPos = DynamXGeometry.rotateVectorByQuaternion(partPos, physicsRotation);

            buoyForces.get(i).set(buoyForce.mult(0.001f));
            collisionObject.applyForce(buoyForce.multLocal(0.05f), rotatedFloaterPos);

            Vector3f velocityAtPoint = DynamXPhysicsHelper.getVelocityAtPoint(getLinearVelocity(), getAngularVelocity(), rotatedFloaterPos);
            float velocityLength = velocityAtPoint.length();
            Vector3f dragDir = velocityAtPoint.normalize();
            Vector3f dragForce = dragDir.multLocal(0.5f * DynamXPhysicsHelper.WATER_DENSITY * velocityLength * velocityLength * dragCoefficient * area);

            if(Vector3f.isValidVector(dragForce))
                collisionObject.applyForce(dragForce.multLocal(0.05f), rotatedFloaterPos);
            Vector3f nonRotatedDrag = DynamXGeometry.rotateVectorByQuaternion(dragForce, DynamXGeometry.inverseQuaternion(physicsRotation, QuaternionPool.get()));
            dragForces.get(i).set(nonRotatedDrag);
        }
    }
}
