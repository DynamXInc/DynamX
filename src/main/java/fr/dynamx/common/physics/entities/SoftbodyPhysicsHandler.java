package fr.dynamx.common.physics.entities;

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.SoftbodyEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import jme3utilities.lbj.IndexBuffer;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SoftbodyPhysicsHandler<T extends SoftbodyEntity> extends AbstractEntityPhysicsHandler<T, PhysicsSoftBody> {

    private final Vector3f linearVel = new Vector3f();
    private final Vector3f rotationalVel = new Vector3f();

    public SoftbodyPhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    protected PhysicsSoftBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        PhysicsSoftBody softBody = new PhysicsSoftBody();
        softBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, handledEntity));
        NativeSoftBodyUtil.appendFromTriMesh(DynamXRenderUtils.icosphereMesh, softBody);

        softBody.setPose(false, true);
        SoftBodyConfig config = softBody.getSoftConfig();
        config.set(Sbcp.PoseMatching, 0.02f);
        softBody.generateClusters();
        softBody.randomizeConstraints();

         softBody.setPhysicsLocation(position);
        FacesMesh facesMesh = new FacesMesh(softBody, null, null);
        DynamXContext.getSoftbodyEntityMesh().put(handledEntity, facesMesh);
        //softBody.setUserObject(facesMesh);
        facesMesh.setUvs(DynamXRenderUtils.icosphereMesh.getTextureCoordinates());
        //softBody.setCcdSweptSphereRadius(0.7f);
        //softBody.setCcdMotionThreshold(0.7f);
        softBody.setMargin(0.2f);
        for (int i = 0; i < softBody.countNodes(); i++) {
            //softBody.setNodeMass(i, 0);
        }
        /*softBody.setCcdSweptSphereRadius(0.7f);
        softBody.setCcdMotionThreshold(0.7f);
        softBody.setMargin(0.1f);*/

        //softBody.setPose(false, true);
        return softBody;
    }

    @Override
    public void update() {
        super.update();
        if (handledEntity.softBody != null && getCollisionObject() != null &&
                handledEntity.changed) {
            /*FloatBuffer nodes = BufferUtils.createFloatBuffer(handledEntity.softBody.countNodes() * 3);
            handledEntity.softBody.copyLocations(nodes);
            //getCollisionObject().appendNodes(nodes);

            IntBuffer faces = BufferUtils.createIntBuffer(handledEntity.softBody.countFaces() * 3);
            handledEntity.softBody.copyFaces(faces);
            //getCollisionObject().appendFaces(IndexBuffer.wrapIndexBuffer(faces));

            IntBuffer links = BufferUtils.createIntBuffer(handledEntity.softBody.countLinks() * 2);
            handledEntity.softBody.copyLinks(links);
            //getCollisionObject().appendLinks(IndexBuffer.wrapIndexBuffer(links));

            FloatBuffer masses = BufferUtils.createFloatBuffer(handledEntity.softBody.countNodes());
            handledEntity.softBody.copyMasses(masses);
            getCollisionObject().setMasses(masses);*/

            //getCollisionObject().getSoftConfig().copyAll(handledEntity.softBody.getSoftConfig());
            handledEntity.changed = false;

        }
    }

    @Override
    public void setPhysicsPosition(Vector3f position) {
        handledEntity.physicsPosition.set(position);
        collisionObject.setPhysicsLocation(position);
    }

    @Override
    public void setPhysicsRotation(Quaternion rotation) {
        handledEntity.physicsRotation.set(rotation);
        collisionObject.applyRotation(rotation);
    }

    @Override
    public Vector3f getLinearVelocity() {
        return linearVel;
    }

    @Override
    public Vector3f getAngularVelocity() {
        return rotationalVel;
    }

    @Override
    public void setLinearVelocity(Vector3f velocity) {
        this.linearVel.set(velocity);
        //collisionObject.vel(velocity);
    }

    @Override
    public void setAngularVelocity(Vector3f velocity) {
        this.rotationalVel.set(velocity);
        //collisionObject.setAngularVelocity(velocity);
    }

    @Override
    public void applyForce(Vector3f at, Vector3f force) {
        //getCollisionObject().applyForce(force, at);

    }

    @Override
    public void applyTorque(Vector3f force) {

    }

    @Override
    public void applyImpulse(Vector3f at, Vector3f force) {

    }

    @Override
    public void applyTorqueImpulse(Vector3f force) {

    }

    @Override
    public void setFreezePhysics(boolean freeze) {

    }
}
