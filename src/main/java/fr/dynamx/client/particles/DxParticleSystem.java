package fr.dynamx.client.particles;

import fr.dynamx.client.particles.emitters.DxEmitterCone;
import fr.dynamx.client.particles.emitters.DxEmitterShape;
import fr.dynamx.client.particles.modules.DxParticleModule;
import fr.dynamx.client.renders.particles.DxParticleMesh;
import fr.dynamx.client.renders.particles.DxParticleRenderer;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Getter
public class DxParticleSystem {

    @Getter
    private final List<DxParticle> particles = new ArrayList<>();
    private final Vector3f gravity;
    @Setter
    private float duration;
    @Setter
    private float elapsedTime = 0;
    @Setter
    private boolean looping;

    private final Vector3f startSpeed;
    private final Vector4f startColor;
    private final float startDelay;
    @Getter
    private final float startScale;
    @Getter
    private final float startLife;
    private final boolean playOnAwake;

    @SideOnly(Side.CLIENT)
    @Getter
    private final DxParticleRenderer particleRenderer;

    @Getter
    private final Matrix4f transform;

    @Getter
    private boolean autoRandomSeed;
    private long randomSeed;
    @Getter
    private Random random;

    @Getter
    @Setter
    private DxEmitterShape emitterShape;

    private final List<DxParticleModule> modules;
    @Getter
    private final BlendMode blendMode;

    private final Vector3f tempVec1 = new Vector3f();
    private final Vector3f tempVec2 = new Vector3f();
    private final Vector3f translation = new Vector3f();

    private DxParticleSystem(Builder builder) {
        this.startDelay = builder.startDelay;
        this.startSpeed = builder.startSpeed;
        this.startColor = builder.startColor;
        this.startScale = builder.startScale;
        this.startLife = builder.startLife;
        this.duration = builder.duration;
        this.looping = builder.looping;
        this.playOnAwake = builder.playOnAwake;
        this.gravity = builder.gravity;
        this.modules = builder.modules;
        this.transform = builder.transform;
        this.blendMode = builder.blendMode;
        this.particleRenderer = new DxParticleRenderer(builder.textureName);

        initializeRandom();

        this.emitterShape = builder.emitterShape;
        if (this.emitterShape == null) {
            this.emitterShape = new DxEmitterCone();
        }
        this.emitterShape.init(this);

        for (DxParticleModule module : this.modules) {
            module.init(this);
        }
    }

    private void initializeRandom() {
        random = autoRandomSeed ? new Random() : new Random(randomSeed);
    }

    public void update(float deltaTime) {
        transform.getTranslation(translation);

        elapsedTime += deltaTime;
        if (elapsedTime < startDelay) {
            return;
        }

        if (playOnAwake && (looping || elapsedTime - startDelay < duration)) {
            if (particles.size() < DxParticleManager.MAX_PARTICLES) {
                spawnParticle(translation, startSpeed, startLife, startColor, startScale);
            }
        }

        if (!looping && elapsedTime - startDelay >= duration) {
            if (particles.isEmpty()) {
                // Mark for removal if not looping and all particles are dead
            }
        } else if (looping && elapsedTime - startDelay >= duration) {
            elapsedTime = startDelay;
            initializeRandom();
        }

        DxParticleMesh mesh = particleRenderer.getParticleMesh();
        mesh.getCenters().getBuffer().position(0);
        mesh.getColors().getBuffer().position(0);

        int particleIndex = 0;
        Iterator<DxParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            DxParticle particle = iterator.next();
            particle.setLife(particle.getLife() - deltaTime);
            if (particle.getLife() <= 0) {
                iterator.remove();
                continue;
            }

            //Update simulation
            tempVec1.set(gravity).mul(deltaTime);
            particle.getSpeed().add(tempVec1);
            tempVec2.set(particle.getSpeed()).mul(deltaTime);
            particle.getPosition().add(tempVec2);

            for (DxParticleModule module : modules) {
                module.transformParticle(particle);
            }

            Vector4f color = particle.getColor();
            color.x = Math.max(0, Math.min(1, color.x));
            color.y = Math.max(0, Math.min(1, color.y));
            color.z = Math.max(0, Math.min(1, color.z));
            color.w = Math.max(0, Math.min(1, color.w));

            //Update VBO
            updateParticleData(mesh, particle, particleIndex++);
        }
    }

    private void updateParticleData(DxParticleMesh mesh, DxParticle particle, int index) {
        int particleVboIndex = 4 * index;

        mesh.getCenters().put(particleVboIndex, particle.getPosition().x);
        mesh.getCenters().put(particleVboIndex + 1, particle.getPosition().y);
        mesh.getCenters().put(particleVboIndex + 2, particle.getPosition().z);
        mesh.getCenters().put(particleVboIndex + 3, particle.getScale());

        mesh.getColors().put(particleVboIndex, particle.getColor().x);
        mesh.getColors().put(particleVboIndex + 1, particle.getColor().y);
        mesh.getColors().put(particleVboIndex + 2, particle.getColor().z);
        mesh.getColors().put(particleVboIndex + 3, particle.getColor().w);
    }


    public void spawnParticle(Vector3f position, Vector3f speed, float life, Vector4f color, float scale) {
        if (particles.size() >= DxParticleManager.MAX_PARTICLES) return;

        DxParticle newParticle = new DxParticle(new Vector3f(speed), life);
        newParticle.setPosition(position);
        newParticle.setColor(color);
        newParticle.setInitialScale(scale);

        emitterShape.transformParticle(newParticle);
        particles.add(newParticle);
    }

    public void clear() {
        particles.clear();
        particleRenderer.clean();
    }


    public Vector3f randomVector(Vector3f min, Vector3f max) {
        float x = min.x + random.nextFloat() * (max.x - min.x);
        float y = min.y + random.nextFloat() * (max.y - min.y);
        float z = min.z + random.nextFloat() * (max.z - min.z);
        return new Vector3f(x, y, z);
    }

    public float randomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public static class Builder {
        private Vector3f startSpeed = new Vector3f();
        private Vector4f startColor = new Vector4f(1, 1, 1, 1);
        private float startDelay = 0f;
        private float startScale = 1f;
        private float startLife = 5f;
        private float duration = 5f;
        private boolean looping = true;
        private boolean playOnAwake = true;
        private Vector3f gravity = new Vector3f(0, -9.81f, 0);
        private List<DxParticleModule> modules = new ArrayList<>();
        private DxEmitterShape emitterShape;
        private Matrix4f transform = new Matrix4f();
        private String textureName = "smookalpha.png"; // Default texture
        private BlendMode blendMode = BlendMode.ALPHA; // Default blend mode

        public Builder startSpeed(Vector3f val) { startSpeed = val; return this; }
        public Builder startColor(Vector4f val) { startColor = val; return this; }
        public Builder startDelay(float val) { startDelay = val; return this; }
        public Builder startScale(float val) { startScale = val; return this; }
        public Builder startLife(float val) { startLife = val; return this; }
        public Builder duration(float val) { duration = val; return this; }
        public Builder looping(boolean val) { looping = val; return this; }
        public Builder playOnAwake(boolean val) { playOnAwake = val; return this; }
        public Builder gravity(Vector3f val) { gravity = val; return this; }
        public Builder addModule(DxParticleModule val) { modules.add(val); return this; }
        public Builder emitter(DxEmitterShape val) { emitterShape = val; return this; }
        public Builder position(Vector3f val) { this.transform.setTranslation(val); return this; }
        public Builder texture(String val) { this.textureName = val; return this; }
        public Builder blendMode(BlendMode val) { this.blendMode = val; return this; }

        public DxParticleSystem build() {
            return new DxParticleSystem(this);
        }
    }
}