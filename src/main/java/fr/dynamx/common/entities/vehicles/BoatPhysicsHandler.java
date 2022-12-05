package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
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

public class BoatPhysicsHandler<T extends BoatEntity<?>> extends BaseVehiclePhysicsHandler<T> {
    public List<PartFloat> floatList;
    public List<Vector3f> buoyForces = new ArrayList<>();
    public List<Vector3f> dragForces = new ArrayList<>();


    public BoatPhysicsHandler(T entity) {
        super(entity);
        floatList = packInfo.getPartsByType(PartFloat.class);
        for (PartFloat partFloat : floatList) {
            partFloat.childFloatsPos.clear();
            AxisAlignedBB floaterBoundingBox = partFloat.box;
            Vector3f pos;

            switch (partFloat.axis) {
                case 0:
                    for (int i = 0; i < partFloat.lineSize.x; i++) {
                        pos = new Vector3f().set(partFloat.getPosition());
                        float xPos = (float) (floaterBoundingBox.minX + i * (partFloat.size + partFloat.spacing.x) + partFloat.offset.x);
                        pos.addLocal(xPos + partFloat.size / 2, 0, 0);
                        partFloat.childFloatsPos.add(pos);
                        buoyForces.add(new Vector3f());
                        dragForces.add(new Vector3f());
                    }
                    break;
                case 2:
                    for (int j = 0; j < partFloat.lineSize.z; j++) {
                        pos = new Vector3f().set(partFloat.getPosition());
                        float zPos = (float) (floaterBoundingBox.minZ + j * (partFloat.size + partFloat.spacing.z) + partFloat.offset.z);
                        pos.addLocal(0, 0, -zPos - partFloat.size / 2);
                        partFloat.childFloatsPos.add(pos);
                        buoyForces.add(new Vector3f());
                        dragForces.add(new Vector3f());
                    }
                    break;
                case 3:
                    for (int i = 0; i < partFloat.lineSize.x; i++) {
                        for (int j = 0; j < partFloat.lineSize.z; j++) {
                            pos = new Vector3f().set(partFloat.getPosition());
                            float xPos = (float) (floaterBoundingBox.minX + i * (partFloat.size + partFloat.spacing.x) + partFloat.offset.x);
                            float zPos = (float) (floaterBoundingBox.minZ + j * (partFloat.size + partFloat.spacing.z) + partFloat.offset.z);
                            pos.addLocal(xPos + partFloat.size / 2, 0, zPos + partFloat.size / 2);
                            partFloat.childFloatsPos.add(pos);
                            buoyForces.add(new Vector3f());
                            dragForces.add(new Vector3f());
                        }
                    }
                    break;
                default:
                    pos = partFloat.getPosition();
                    partFloat.childFloatsPos.add(pos);
                    buoyForces.add(new Vector3f());
                    dragForces.add(new Vector3f());
                    break;
            }

        }
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
            waterLevel = (float) handledEntity.getEntityWorld().getBlockState(blockPos).getSelectedBoundingBox(handledEntity.getEntityWorld(), blockPos).maxY - 0.125F + 0.5f;
            int i = 0;
            for (PartFloat partFloat : floatList) {
                for (Vector3f floatCenter : partFloat.childFloatsPos) {
                    handleBuoyancy(floatCenter, partFloat.dragCoefficient, partFloat.size, waterLevel, i++);
                }
            }
        }
            /*Vector3f floatPos = physicsPosition.add(DynamXGeometry.rotateVectorByQuaternion(floater, physicsRotation));
            BlockPos blockPos = new BlockPos(floatPos.x, floatPos.y + liquidOffset - 1, floatPos.z);
            //waterLevel = (float) handledEntity.getEntityWorld().getBlockState(blockPos).getSelectedBoundingBox(handledEntity.getEntityWorld(), blockPos).maxY - 0.125F + 0.5f;
            if (handledEntity.getEntityWorld().getBlockState(blockPos).getMaterial().isLiquid()) {
                waterLevel = blockPos.getY() + 1 + BlockLiquid.getBlockLiquidHeight(handledEntity.getEntityWorld().getBlockState(blockPos), handledEntity.world, blockPos);
                floatPart(f.size, floater, waterLevel, i);
            }
            i++;*/
        super.update();
    }

    private void handleBuoyancy(Vector3f partPos, float dragCoefficient, float size, float waterLevel, int i) {
        Vector3f localPosRotated = DynamXGeometry.rotateVectorByQuaternion(partPos, physicsRotation);
        Vector3f inWorldPos = physicsPosition.add(localPosRotated);

        double dy = waterLevel - inWorldPos.y;

        if (dy > 0) {
            float area = size * size;
            dy = Math.min(dy, size);
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
