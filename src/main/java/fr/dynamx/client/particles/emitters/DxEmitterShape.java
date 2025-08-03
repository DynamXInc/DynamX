package fr.dynamx.client.particles.emitters;

import fr.dynamx.client.particles.DxParticle;
import fr.dynamx.client.particles.DxParticleSystem;

public interface DxEmitterShape {

    void init(DxParticleSystem system);

    void transformParticle(DxParticle particle);
}