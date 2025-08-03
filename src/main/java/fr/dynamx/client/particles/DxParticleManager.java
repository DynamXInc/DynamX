package fr.dynamx.client.particles;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class DxParticleManager {
    public static final int MAX_PARTICLES = 100000;

    public List<DxParticleSystem> particleSystems = new ArrayList<>();

    @Getter
    private int globalParticlesCount;

    public void update(float delta) {
        globalParticlesCount = 0;
        for (DxParticleSystem system : particleSystems) {
            system.update(delta);
            globalParticlesCount += system.getParticles().size();
        }
    }

    public void render() {
        for (DxParticleSystem system : particleSystems) {
            system.getParticleRenderer().render(system);
        }
    }

    public void addParticleSystem(DxParticleSystem dxParticleSystem) {
        particleSystems.add(dxParticleSystem);
    }

    public void removeParticleSystem(DxParticleSystem dxParticleSystem) {
        particleSystems.remove(dxParticleSystem);
    }

    public void clear() {
        particleSystems.forEach(DxParticleSystem::clear);
        particleSystems.clear();
        globalParticlesCount = 0;
    }
}
