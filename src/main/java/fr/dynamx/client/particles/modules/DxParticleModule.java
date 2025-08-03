package fr.dynamx.client.particles.modules;

import fr.dynamx.client.particles.DxParticle;
import fr.dynamx.client.particles.DxParticleSystem;

public abstract class DxParticleModule {

    protected DxParticleSystem system;

    public DxParticleModule() {
    }

    public void init(DxParticleSystem system) {
        this.system = system;
    }

    public abstract void transformParticle(DxParticle particle);
}