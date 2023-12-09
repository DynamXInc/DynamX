package fr.dynamx.utils.debug.renderer;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import java.awt.*;


public class PhysicsDebugRenderer {
    public static void debugSoftBody(PhysicsSoftBody physicsSoftBody) {
        GlStateManager.pushMatrix();
        Vector3f physicsLocation = Vector3fPool.get();
        Quaternion physicsRotation = QuaternionPool.get();
        physicsSoftBody.getPhysicsLocation(physicsLocation);
        physicsSoftBody.getPhysicsRotation(physicsRotation);
        /*BoundingBox bb = new BoundingBox();
        physicsSoftBody.boundingBox(bb);
        Vector3f min = new Vector3f();
        Vector3f max = new Vector3f();
        bb.getMin(min);
        bb.getMax(max);
        DynamXRenderUtils.drawBoundingBox(min, max, 1,0,0,1);*/
        //GlStateManager.translate(physicsLocation.x, physicsLocation.y, physicsLocation.z);
        //GlStateManager.rotate(GlQuaternionPool.get(physicsRotation));

        //DynamXRenderUtils.drawBoundingBox(new Vector3f(1,1,1), 1,0,0,1);
        GlStateManager.glBegin(GL11.GL_TRIANGLES);
        /*System.out.println(physicsSoftBody.countFaces());
        System.out.println("n "+physicsSoftBody.countNodes());*/
        int numFaces = physicsSoftBody.countFaces();
        for (int i = 0; i < numFaces; i++) {
            Vector3f nodePos1 = Vector3fPool.get();
            Vector3f nodePos2 = Vector3fPool.get();
            Vector3f nodePos3 = Vector3fPool.get();

            Vector3f nodeNormal1 = Vector3fPool.get();
            Vector3f nodeNormal2 = Vector3fPool.get();
            Vector3f nodeNormal3 = Vector3fPool.get();

            physicsSoftBody.nodeLocation(i, nodePos1);
            physicsSoftBody.nodeLocation(i + 1, nodePos2);
            physicsSoftBody.nodeLocation(i + 2, nodePos3);

            physicsSoftBody.nodeNormal(i, nodeNormal1);
            physicsSoftBody.nodeNormal(i + 1, nodeNormal2);
            physicsSoftBody.nodeNormal(i + 2, nodeNormal3);
            Vector3f tpt1 = Vector3fPool.get(), tpt2 = Vector3fPool.get();
            tpt1 = nodePos2.subtract(nodePos1);
            tpt2 = nodePos3.subtract(nodePos1);

            Vector3f normal = tpt1.cross(tpt2);


            /*GlStateManager.glNormal3f(nodeNormal1.x, nodeNormal1.y, nodeNormal1.z);
            GlStateManager.glNormal3f(nodeNormal2.x, nodeNormal2.y, nodeNormal2.z);
            GlStateManager.glNormal3f(nodeNormal3.x, nodeNormal3.y, nodeNormal3.z);*/

            GlStateManager.glVertex3f(nodePos1.x, nodePos1.y, nodePos1.z);
            GlStateManager.glVertex3f(nodePos2.x, nodePos2.y, nodePos2.z);
            GlStateManager.glVertex3f(nodePos3.x, nodePos3.y, nodePos3.z);
            GlStateManager.glNormal3f(normal.x, normal.y, normal.z);

        }
        GlStateManager.glEnd();
        GlStateManager.popMatrix();
    }

    public static void debugRigidBody(PhysicsRigidBody physicsRigidBody, RigidBodyTransform prevTransform, RigidBodyTransform curTransform, float partialTicks) {
        Object userObject = physicsRigidBody.getUserObject();
        Vector3f physicsLocation = Vector3fPool.get();
        Quaternion physicsRotation = QuaternionPool.get();
        GlStateManager.pushMatrix();
        int greenColor = physicsRigidBody.getActivationState() == 2 ? 1 : 0;
        float blueColor = physicsRigidBody.getActivationState() == 2 ? 0 : 0.8f;
        if (userObject instanceof BulletShapeType) {
            if (curTransform != null) {
                BulletShapeType<?> shapeType = (BulletShapeType<?>) userObject;
                if (!(shapeType).getType().isTerrain()) {
                    physicsLocation = curTransform.getPosition();
                    physicsRotation = curTransform.getRotation();
                    if (prevTransform != null) {
                        GlStateManager.translate(
                                prevTransform.getPosition().x + (physicsLocation.x - prevTransform.getPosition().x) * partialTicks,
                                prevTransform.getPosition().y + (physicsLocation.y - prevTransform.getPosition().y) * partialTicks,
                                prevTransform.getPosition().z + (physicsLocation.z - prevTransform.getPosition().z) * partialTicks);
                        GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prevTransform.getRotation(), physicsRotation, partialTicks));
                    } else {
                        GlStateManager.translate(physicsLocation.x, physicsLocation.y, physicsLocation.z);
                        GlStateManager.rotate(GlQuaternionPool.get(physicsRotation));
                    }
                    GlStateManager.color(1, greenColor, blueColor, 1);
                    DynamXRenderUtils.drawConvexHull(shapeType.getDebugTriangles(physicsRigidBody.getCollisionShape()), DynamXDebugOptions.RENDER_WIREFRAME.isActive());
                    GlStateManager.color(1, 1, 1, 1);
                }
            }
        } else {
            physicsRigidBody.getPhysicsLocation(physicsLocation);
            physicsRigidBody.getPhysicsRotation(physicsRotation);
            GlStateManager.translate(physicsLocation.x, physicsLocation.y, physicsLocation.z);
            GlStateManager.rotate(GlQuaternionPool.get(physicsRotation));

            CollisionShape collisionShape = physicsRigidBody.getCollisionShape();

            if (collisionShape instanceof BoxCollisionShape) {
                debugBoxCollisionShape((BoxCollisionShape) collisionShape, 1, greenColor, blueColor, 1);
            } else if (collisionShape instanceof SphereCollisionShape) {
                debugSphereCollisionShape((SphereCollisionShape) collisionShape, 10, 1, greenColor, blueColor, 1);
            } else if (collisionShape instanceof CompoundCollisionShape) {
                for (ChildCollisionShape childCollisionShape : ((CompoundCollisionShape) collisionShape).listChildren()) {
                    if (childCollisionShape.getShape() instanceof BoxCollisionShape) {
                        DynamXRenderUtils.glTranslate(childCollisionShape.copyOffset(Vector3fPool.get()));
                        debugBoxCollisionShape((BoxCollisionShape) childCollisionShape.getShape(), 1, greenColor, blueColor, 1);
                        DynamXRenderUtils.glTranslate(childCollisionShape.copyOffset(Vector3fPool.get()).multLocal(-1));
                    }
                }
            }
        }



        GlStateManager.popMatrix();
    }


    public static void debugSphereCollisionShape(SphereCollisionShape sphereCollisionShape, int resolution, float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
        float radius = sphereCollisionShape.getRadius();
        Sphere sphere = new Sphere();
        sphere.setDrawStyle(GLU.GLU_LINE);
        sphere.draw(radius, resolution, resolution);
    }

    public static void debugBoxCollisionShape(BoxCollisionShape boxCollisionShape, float red, float green, float blue, float alpha) {
        Vector3f halfExtent = Vector3fPool.get();
        boxCollisionShape.getHalfExtents(halfExtent);
        DynamXRenderUtils.drawBoundingBox(halfExtent, red, green, blue, alpha);
    }

    public static void debugConstraint(PhysicsJoint joint, float partialTicks) {
        if (joint instanceof Constraint) {
            Constraint constraint = (Constraint) joint;
            Vector3f pivotA = Vector3fPool.get();
            Vector3f pivotB = Vector3fPool.get();
            if (constraint.getBodyA() != null) {
                constraint.getPivotA(pivotA);
                drawSingleEndedConstraint(constraint.getBodyA(), pivotA, new Color(1, 0, 0, 1), new Color(1, 0, 0, 0.5f), new Color(1, 0, 0, 1), partialTicks);
            }
            if (constraint.getBodyB() != null) {
                constraint.getPivotB(pivotB);
                drawSingleEndedConstraint(constraint.getBodyB(), pivotB, new Color(0, 1, 0, 1), new Color(1, 1, 0, 0.5f), new Color(0, 1, 0, 1), partialTicks);
            }
            if (constraint.getBodyA() != null && constraint.getBodyB() != null) {
                constraint.getPivotA(pivotA);
                constraint.getPivotB(pivotB);
                drawDoubleEndedConstraint(constraint.getBodyA(), constraint.getBodyB(), pivotA, pivotB, new Color(0.5f, 0, 0.5f, 1), partialTicks);
            }
        }
    }

    public static void drawSingleEndedConstraint(PhysicsRigidBody rigidBody, Vector3f pivot, Color lineColor, Color endA, Color endB, float partialTicks) {
        Vector3f bodyPos = ClientDebugSystem.getInterpolatedTranslation(rigidBody, partialTicks);
        Quaternion bodyRot = ClientDebugSystem.getInterpolatedRotation(rigidBody, partialTicks);

        Vector3f rotatedPosA = DynamXGeometry.rotateVectorByQuaternion(pivot, bodyRot);
        Vector3f translatedPosA = rotatedPosA.add(bodyPos);

        drawJointLine(bodyPos, translatedPosA, lineColor);

        DynamXRenderUtils.drawSphere(bodyPos, 0.05f, endA);
        DynamXRenderUtils.drawSphere(translatedPosA, 0.05f, endB);

    }

    public static void drawDoubleEndedConstraint(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB, Vector3f pivotA, Vector3f pivotB, Color lineColor, float partialTicks) {
        Vector3f posA = ClientDebugSystem.getInterpolatedTranslation(bodyA, partialTicks);
        Quaternion rotA = ClientDebugSystem.getInterpolatedRotation(bodyA, partialTicks);

        Vector3f posB = ClientDebugSystem.getInterpolatedTranslation(bodyB, partialTicks);
        Quaternion rotB = ClientDebugSystem.getInterpolatedRotation(bodyB, partialTicks);

        Vector3f rotatedPosA = DynamXGeometry.rotateVectorByQuaternion(pivotA, rotA);
        Vector3f rotatedPosB = DynamXGeometry.rotateVectorByQuaternion(pivotB, rotB);
        Vector3f translatedPosA = rotatedPosA.add(posA);
        Vector3f translatedPosB = rotatedPosB.add(posB);

        drawJointLine(translatedPosA, translatedPosB, lineColor);

        DynamXRenderUtils.drawSphere(translatedPosA, 0.05f, lineColor);
        DynamXRenderUtils.drawSphere(translatedPosB, 0.05f, lineColor);
    }

    public static void drawJointLine(Vector3f pivotA, Vector3f pivotB, Color lineColor) {
        GlStateManager.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha());
        GlStateManager.glBegin(GL11.GL_LINE_STRIP);
        GlStateManager.glVertex3f(pivotA.x, pivotA.y, pivotA.z);
        GlStateManager.glVertex3f(pivotB.x, pivotB.y, pivotB.z);
        GlStateManager.glEnd();
        GlStateManager.color(1, 1, 1, 1);
    }
}
