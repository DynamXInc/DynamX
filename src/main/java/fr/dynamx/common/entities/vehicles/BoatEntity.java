package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.BoatEngineModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BoatEntity<T extends BoatEntity.BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.IEngineContainer, IModuleContainer.IPropulsionContainer, IModuleContainer.ISeatsContainer {
    private IEngineModule engine;
    private ISeatsModule seats;
    private IPropulsionModule propulsion;

    public BoatEntity(World world) {
        super(world);
    }

    public BoatEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new BoatPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        //Take care to add seats BEFORE engine (the engine needs to detect dismounts)
        modules.add(seats = new SeatsModule(this));
        //Take care to add propulsion BEFORE engine (the engine needs a propulsion)
        modules.add(propulsion = new BoatEngineModule(this));

        super.createModules(modules);
        engine = modules.getByClass(EngineModule.class);
    }

    @Nonnull
    @Override
    public IEngineModule getEngine() {
        return engine;
    }

    @Nonnull
    @Override
    public IPropulsionModule getPropulsion() {
        return propulsion;
    }

    @Nonnull
    @Override
    public ISeatsModule getSeats() {
        if (seats == null) //We may need seats before modules are created, because of seats sync
            seats = new SeatsModule(this);
        return seats;
    }

    @Override
    public BaseVehicleEntity<?> cast() {
        return this;
    }

    @Override
    protected ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.BOATS.findInfo(infoName);
    }

    public static Vector3f[] ntm = new Vector3f[10];
    public static Vector3f[] ntmdrag = new Vector3f[10];

    public static class BoatPhysicsHandler<A extends BoatEntity<?>> extends BaseVehiclePhysicsHandler<A> {
        public BoatPhysicsHandler(A entity) {
            super(entity);
            //getPhysicsVehicle().setAngularFactor(0);
            //System.out.println("Gravity is " + getPhysicsVehicle().getGravity(Vector3fPool.get()));
        }

        @Override
        public IPropulsionHandler getPropulsion() {
            return getHandledEntity().getPropulsion().getPhysicsHandler(); //BOAT ENGINE
        }

        private void floatPart(/*MutableBoundingBox bb, */float size, Vector3f partPos, int i) {
            //MutableBoundingBox bb = new MutableBoundingBox(f.box);
            //bb = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), bb, getRotation());

            Vector3f localPosRotated = DynamXGeometry.rotateVectorByQuaternion(partPos, getCollisionObject().getPhysicsRotation(QuaternionPool.get()));
            Vector3f inWorldPos = getCollisionObject().getPhysicsLocation(Vector3fPool.get()).add(localPosRotated);

            double dy = (float) (40 - (inWorldPos.y/* + bb.minY*/));
            Vector3f p = partPos;
            Vector3f forcer = new Vector3f();

            Vector3f drag = DynamXPhysicsHelper.getVelocityAtPoint(getLinearVelocity(), getAngularVelocity(), p);
            if(i < 5) {
                ntm[i] = drag;
            }
            //System.out.println("[" + i + "] Speed is " + drag);
            if (dy > 0) {
                dy = Math.min(dy,size);
                p.y -= size;
                double vol = dy * size * size;
                //System.out.println(bb+" "+vol+" "+DynamXMain.physicsWorld.getDynamicsWorld().getGravity(new Vector3f()).multLocal((float) (-1000*vol)));
                vol = vol * 1000;
                //vol *= 0.01f;
                Vector3f fr = getCollisionObject().getGravity(Vector3fPool.get()).multLocal((float) (-vol));
                //fr = new Force(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(new Vector3f()).multLocal(-(packInfo.getEmptyMass()+10)), new Vector3f());
                //forces.add(fr);*/

                    /*Vector3f surfPos = Vector3fPool.get(getPosition().x, 40, getPosition().z);
                        p = DynamXPhysicsHelper.calculateBuoyantCenter(getCollisionObject(), surfPos, Vector3fPool.get(0, 1, 0));

                        Vector3f center = getCollisionObject().getTransform(TransformPool.get()).transformVector(p, Vector3fPool.get());
                        Vector3f offset = center.subtractLocal(surfPos);
                        float submerged = Vector3fPool.get(0, 1, 0).dot(offset);
                    if (submerged < 0) {
                        // Calc the volume coefficient
                        float dist = -submerged;
                        float p1 = MathHelper.clamp(dist / 1.2f, 0.f, 1.f);
                        double vol = p1 * Math.sqrt((bb.maxX - bb.minX) * (bb.maxX - bb.minX)) * Math.sqrt((bb.maxZ - bb.minZ) * (bb.maxZ - bb.minZ)) * Math.min(dy, Math.sqrt((bb.maxY - bb.minY) * (bb.maxY - bb.minY)));

                        // TODO: Need a way to calculate this properly
                        // Force should be exactly equal to -gravity when submerged distance is 0
                        // density units kg/m^3
                        Vector3f force = (getCollisionObject().getGravity(Vector3fPool.get()).multLocal((float) (-10f * vol)));

                        Vector3f relPos = center.subtract(getCollisionObject().getTransform(TransformPool.get()).getTranslation());
                        //getCollisionObject().applyForce(force, relPos);
                        forcer.set(force);
                    }*/

                getCollisionObject().applyForce(fr.multLocal(0.05f), p);
                forcer.set(fr);
                //SHOULD NOT BE COMMENTED drag = DynamXPhysicsHelper.getWaterDrag(drag, getPackInfo().getDragFactor());
            }
            //else
            //SHOULD NOT BE COMMENTED drag = DynamXPhysicsHelper.getAirDrag(drag, getPackInfo().getDragFactor());
            drag.multLocal(1f);
            drag.multLocal(drag.mult(-1));
            //getCollisionObject().applyForce(drag, p);
            if(i < 5) {
                ntmdrag[i] = drag;
                ntmdrag[i + 5] = new Vector3f();
                ntmdrag[i + 5].set(forcer);
                ntmdrag[i + 5].multLocal(0.01f);
            }

            //System.out.println("[" + i + "] Apply force = " + forcer + " // drag = " + drag + " // at = " + p);
        }

        @Override
        public void update() {
            Vector3f dragForce = null;//SHOULD NOT BE COMMENTED  DynamXPhysicsHelper.getWaterDrag(getLinearVelocity(), getPackInfo().getDragFactor());

            getCollisionObject().setAngularDamping(0.7f);
            getCollisionObject().setLinearDamping(0.7f);
            getCollisionObject().setEnableSleep(false);
            //if(getPhysicsPosition().y <= 40)
            {
                //forces.add(new Force(dragForce, Vector3fPool.get()));
                int i = 0;
               // System.out.println("=== === Linear vel " + getLinearVelocity() + " === === Angular vel " + getAngularVelocity() + " === ===");
                for (PartFloat f : packInfo.getPartsByType(PartFloat.class)) {
                    AxisAlignedBB bb = f.box;
                    float size = 0.1f;
                    float sizeX = (float) (bb.maxX - bb.minX);
                    float sizeY = (float) (bb.maxY - bb.minY);
                    float sizeZ = (float) (bb.maxZ - bb.minZ);
                    for (int j = 0; j < sizeX/size; j++) {
                        for (int k = 0; k < sizeZ/size; k++) {
                            for (int l = 0; l < sizeY/size; l++) {
                                Vector3f pos = Vector3fPool.get(bb.minX + j*size, bb.minY + l*size, bb.minZ + k*size);
                                floatPart(size, pos, i);
                                i++;
                            }
                        }
                    }
                    i++;
                    break;
                }
                //System.out.println(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(Vector3fPool.get())+" "+packInfo.getEmptyMass());
            }
            super.update();
        }
    }
}
