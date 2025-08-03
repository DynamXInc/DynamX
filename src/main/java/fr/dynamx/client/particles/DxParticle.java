package fr.dynamx.client.particles;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DxParticle {

    @Getter
    private final Vector3f position = new Vector3f();

    @Getter
    private final Vector3f rotation = new Vector3f();

    @Getter
    private final Vector4f color = new Vector4f();

    @Getter
    @Setter
    private float scale = 1;

    @Getter
    private float initialScale = 1;

    @Getter
    @Setter
    private Vector3f speed = new Vector3f();

    @Getter
    @Setter
    private float life = -1;

    @Getter
    @Setter
    private float cameraDistance = -1;

    @Getter
    @Setter
    private DxParticleSystem parentSystem;

    public DxParticle(){}

    public DxParticle(Vector3f speed, float life) {
        this.speed = new Vector3f(speed);
        this.life = life;
    }

    public DxParticle(DxParticle baseParticle) {
        setPosition(baseParticle.getPosition());
        setRotation(baseParticle.getRotation());
        setScale(baseParticle.getScale());
        setInitialScale(baseParticle.getInitialScale());
        this.speed.set(baseParticle.speed);
        this.life = baseParticle.getLife();
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public void setRotation(Vector3f rotation) {
        this.rotation.set(rotation);
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
    }

    public void setColor(Vector4f color) {
        this.color.set(color);
    }

    public DxParticle setInitialScale(float scale) {
        this.scale = scale;
        this.initialScale = scale;
        return this;
    }

    public void setColor(int r, int g, int b, int a) {
        this.color.set(r, g, b, a);
    }

    public Matrix4f buildModelViewMatrix(){
        return new Matrix4f().translate(position).rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z).scale(scale);
    }

}
