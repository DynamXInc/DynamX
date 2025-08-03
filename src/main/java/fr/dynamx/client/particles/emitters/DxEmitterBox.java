package fr.dynamx.client.particles.emitters;

import fr.dynamx.client.particles.DxParticle;
import fr.dynamx.client.particles.DxParticleSystem;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;

import java.util.Random;

public class DxEmitterBox implements DxEmitterShape {
    private Vector3f size;
    private DxParticleSystem system;
    private static final Random random = new Random();

    @Getter
    @Setter
    private EmitFrom emitFrom;

    public enum EmitFrom {
        EDGE,
        SHELL,
        VOLUME
    }

    public DxEmitterBox() {
        this.emitFrom = EmitFrom.VOLUME;
    }

    @Override
    public void init(DxParticleSystem system) {
        this.system = system;
        this.size = system.getTransform().getScale(new Vector3f());
    }

    @Override
    public void transformParticle(DxParticle particle) {
        Vector3f center = system.getTransform().getTranslation(new Vector3f());
        float x = center.x + (random.nextFloat() - 0.5f) * size.x;
        float y = center.y + (random.nextFloat() - 0.5f) * size.y;
        float z = center.z + (random.nextFloat() - 0.5f) * size.z;

        switch (emitFrom) {
            case VOLUME:
                particle.setPosition(x, y, z);
                break;
            case SHELL:
                int axis = random.nextInt(3); // Choose an axis to fix to the shell
                if (axis == 0) x = random.nextBoolean() ? center.x + size.x / 2 : center.x - size.x / 2;
                else if (axis == 1) y = random.nextBoolean() ? center.y + size.y / 2 : center.y - size.y / 2;
                else z = random.nextBoolean() ? center.z + size.z / 2 : center.z - size.z / 2;
                particle.setPosition(x, y, z);
                break;
            case EDGE:
                boolean xEdge = random.nextBoolean();
                boolean yEdge = random.nextBoolean();
                boolean zEdge = random.nextBoolean();
                if (xEdge) x = random.nextBoolean() ? center.x + size.x / 2 : center.x - size.x / 2;
                if (yEdge) y = random.nextBoolean() ? center.y + size.y / 2 : center.y - size.y / 2;
                if (zEdge) z = random.nextBoolean() ? center.z + size.z / 2 : center.z - size.z / 2;
                particle.setPosition(x, y, z);
                break;
            default:
                particle.setPosition(x, y, z);
                break;
        }
    }
}