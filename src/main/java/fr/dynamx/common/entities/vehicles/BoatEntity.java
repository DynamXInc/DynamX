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
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
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
        engine = getModuleByType(IEngineModule.class);
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

        @Override
        public void update() {
            Vector3f dragForce = null;//SHOULD NOT BE COMMENTED  DynamXPhysicsHelper.getWaterDrag(getLinearVelocity(), getPackInfo().getDragFactor());

            getCollisionObject().setAngularDamping(0.5f);
            //if(getPhysicsPosition().y <= 40)
            {
                //forces.add(new Force(dragForce, Vector3fPool.get()));
                int i = 0;
                System.out.println("=== === Linear vel " + getLinearVelocity() + " === === Angular vel " + getAngularVelocity() + " === ===");
                for (PartFloat f : packInfo.getPartsByType(PartFloat.class)) {
                    MutableBoundingBox bb = new MutableBoundingBox(f.box);
                    bb = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), bb, getRotation());
                    double dy = (float) (40 - (getPosition().y + bb.minY));
                    Vector3f p = f.getPosition();
                    Vector3f forcer = new Vector3f();

                    Vector3f drag = DynamXPhysicsHelper.getVelocityAtPoint(getLinearVelocity(), getAngularVelocity(), p);
                    ntm[i] = drag;
                    System.out.println("[" + i + "] Speed is " + drag);
                    if (dy > 0) {
                        dy = Math.min(dy, Math.sqrt((bb.maxY - bb.minY) * (bb.maxY - bb.minY)));
                        p.y = (float) bb.minY;
                        double vol = dy * Math.sqrt((bb.maxX - bb.minX) * (bb.maxX - bb.minX)) * Math.sqrt((bb.maxZ - bb.minZ) * (bb.maxZ - bb.minZ));
                        //System.out.println(bb+" "+vol+" "+DynamXMain.physicsWorld.getDynamicsWorld().getGravity(new Vector3f()).multLocal((float) (-1000*vol)));
                  /*      Force fr = new Force(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(new Vector3f()).multLocal((float) (-vol*1000)), p);
                        //fr = new Force(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(new Vector3f()).multLocal(-(packInfo.getEmptyMass()+10)), new Vector3f());
                        //forces.add(fr);
                        getRigidBody().applyForce(fr.getForce().multLocal(0.05f), fr.getPosition());
                        forcer.set(fr.getForce());*/
                        //SHOULD NOT BE COMMENTED drag = DynamXPhysicsHelper.getWaterDrag(drag, getPackInfo().getDragFactor());
                    }
                    //else
                    //SHOULD NOT BE COMMENTED drag = DynamXPhysicsHelper.getAirDrag(drag, getPackInfo().getDragFactor());
                    drag.multLocal(0.025f);
                    getCollisionObject().applyImpulse(drag, p);
                    ntmdrag[i] = drag;
                    ntmdrag[i + 5] = new Vector3f();
                    ntmdrag[i + 5].set(forcer);
                    ntmdrag[i + 5].multLocal(0.01f);

                    //Vector3f dragVect = new Vector3f();
                    //Vector3f surfVelVec = new Vector3f();
                    //for (int i = 0; i < this.sections; i++)
                    {
                        /*Vector3f sectionVect = p;

                        surfVelVec.set(drag.x, 0.0F, drag.z);
                        float surfaceVel = surfVelVec.length();

                        float volume = 0.0F;
                        float area = 0.0F;
                        float lift = 0.0F;
                        float density = 997.0F;
                        if (getPhysicsPosition().y+bb.maxY <= 40)
                        {
                            volume = (float) (Math.sqrt((bb.maxY-bb.minY)*(bb.maxY-bb.minY)) * Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * Math.sqrt((bb.maxZ-bb.minZ)*(bb.maxZ-bb.minZ)));
                            area = (float) (Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * Math.sqrt((bb.maxY-bb.minY)*(bb.maxY-bb.minY)));
                        }
                        else if (getPhysicsPosition().y+p.y <= 40)
                        {
                            float capHeight = 40 - (getPhysicsPosition().y+p.y);
                            volume = (float) (capHeight * Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * Math.sqrt((bb.maxZ-bb.minZ)*(bb.maxZ-bb.minZ)));

                            area = (float) (Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * capHeight);
                        }
                        else if (getPhysicsPosition().y+bb.minY <= 40)
                        {
                            float capHeight = 40 - (getPhysicsPosition().y+p.y);
                            volume = (float) (capHeight * Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * Math.sqrt((bb.maxZ-bb.minZ)*(bb.maxZ-bb.minZ)));

                            area = (float) (Math.sqrt((bb.maxX-bb.minX)*(bb.maxX-bb.minX)) * capHeight);

                            //lift = 0.25F * density * surfaceVel * surfaceVel * packInfo.getDragFactor() * area;
                        }
                        float bouyancy = density * 9.81F * volume + lift;
                        float dragc = 0.5F * density * drag.length() * packInfo.getDragFactor() * area;

                        dragVect.set(drag);
                        if (dragVect.length() > 0.0F) {
                            dragVect.normalize();
                        }
                        System.out.println(area+" "+dragc);
                        dragVect.multLocal(dragc*0.05f*0);
                        forcer.set(0, bouyancy*0.05f, 0);
                        getRigidBody().applyForce(forcer, p);
                        getRigidBody().applyForce(dragVect, p);

                        ntmdrag[i] = dragVect;
                        ntmdrag[i+5] = new Vector3f();
                        ntmdrag[i+5].set(forcer);*/
                    }
                    System.out.println("[" + i + "] Apply force = " + forcer + " // drag = " + drag + " // at = " + p);
                    i++;
                }
                //System.out.println(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(Vector3fPool.get())+" "+packInfo.getEmptyMass());
            }
            if (!getHandledEntity().isInWater()) {
                //SHOULD NOT BE COMMENTED dragForce = DynamXPhysicsHelper.getAirDrag(getLinearVelocity(), getPackInfo().getDragFactor());
                //dragForce.y *= 5000;
                //System.err.println("Apply drag force "+dragForce+" "+getLinearVelocity());
                //forces.add(new Force(dragForce, Vector3fPool.get()));
            }
            super.update();
        }
    }
}
