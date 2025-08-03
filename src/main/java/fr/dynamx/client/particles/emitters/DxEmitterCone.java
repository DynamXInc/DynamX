package fr.dynamx.client.particles.emitters;

import fr.dynamx.client.particles.DxParticle;
import fr.dynamx.client.particles.DxParticleSystem;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;

import java.util.Random;

@Getter
@Setter
public class DxEmitterCone implements DxEmitterShape {
    private float height;
    private float angle; // In degrees
    private float radius;
    private float radiusThickness;
    private Random random;
    private DxParticleSystem system;

    private float arc = 360; // Full circular arc by default
    private float arcStart = 0; // Starting angle for the arc
    private float currentAngle = 0; // Current angle for non-random modes
    private boolean direction = true; // Used in Ping-Pong mode

    public enum EmitFrom {
        BASE,
        VOLUME
    }

    public enum ArcEmitMode {
        RANDOM,
        LOOP,
        PING_PONG,
        BURST_SPREAD
    }

    private EmitFrom emitFrom = EmitFrom.BASE;
    private ArcEmitMode arcMode = ArcEmitMode.RANDOM;
    private float spread = 0.1f; // Discrete intervals for particle generation

    public DxEmitterCone() {
        this.angle = 45;
        this.radius = 5;
        this.radiusThickness = 1f;
    }

    public DxEmitterCone(float height, float angle, float radius) {
        this.height = height;
        this.angle = angle;
        this.radius = radius;
    }

    @Override
    public void init(DxParticleSystem system) {
        this.system = system;
        this.random = system.getRandom();
        if (this.height == 0) {
            this.height = system.getTransform().getScale(new Vector3f()).y;
        }
    }

    @Override
    public void transformParticle(DxParticle particle) {
        Vector3f position = system.getTransform().getTranslation(new Vector3f());
        float theta = getNextAngle(); // Get the angle based on the current mode
        float h = emitFrom == EmitFrom.VOLUME ? random.nextFloat() * height : height;
        float radialFactor = (1 - radiusThickness) + (random.nextFloat() * radiusThickness);
        float r = radialFactor * (h / height) * radius;

        float x = position.x + r * (float) Math.cos(Math.toRadians(theta));
        float y = position.y + h;
        float z = position.z + r * (float) Math.sin(Math.toRadians(theta));

        particle.setPosition(x, y, z);
    }

    private float getNextAngle() {
        float angle;
        switch (arcMode) {
            case RANDOM:
                angle = arcStart + random.nextFloat() * arc;
                break;
            case LOOP:
            case PING_PONG:
            case BURST_SPREAD:
                angle = currentAngle;
                updateAngle();
                break;
            default:
                angle = arcStart;
        }
        return angle;
    }

    private void updateAngle() {
        switch (arcMode) {
            case LOOP:
                currentAngle += spread * 360;
                if (currentAngle > arcStart + arc) {
                    currentAngle = arcStart;
                }
                break;
            case PING_PONG:
                if (direction) {
                    currentAngle += spread * 360;
                    if (currentAngle > arcStart + arc) {
                        currentAngle = arcStart + arc;
                        direction = !direction;
                    }
                } else {
                    currentAngle -= spread * 360;
                    if (currentAngle < arcStart) {
                        currentAngle = arcStart;
                        direction = !direction;
                    }
                }
                break;
            case BURST_SPREAD:
                if (currentAngle < arcStart + arc) {
                    currentAngle += spread * 360;
                }
                break;
        }
    }
}