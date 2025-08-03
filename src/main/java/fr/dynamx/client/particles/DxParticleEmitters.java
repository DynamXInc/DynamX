package fr.dynamx.client.particles;

import fr.dynamx.client.particles.emitters.DxEmitterCone;
import fr.dynamx.client.particles.modules.ColorOverLifetime;
import fr.dynamx.client.particles.modules.SizeOverLifetime;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DxParticleEmitters {

    public static DxParticleSystem createSmokeEmitter(Vector3f position) {
        // Emitter Shape
        DxEmitterCone emitterShape = new DxEmitterCone();
        emitterShape.setAngle(15);
        emitterShape.setRadius(0.1f);
        emitterShape.setRadiusThickness(1);
        emitterShape.setSpread(0.2f);

        // Color Module
        ColorOverLifetime colorModule = new ColorOverLifetime();
        colorModule.addColorPoint(0.0f, new Vector4f(0.2f, 0.2f, 0.2f, 0.5f));
        colorModule.addColorPoint(0.5f, new Vector4f(0.4f, 0.4f, 0.4f, 0.25f));
        colorModule.addColorPoint(1.0f, new Vector4f(0.6f, 0.6f, 0.6f, 0.0f));

        // Size Module
        SizeOverLifetime sizeModule = new SizeOverLifetime(false);
        sizeModule.addSizePoint(0.0f, 0.5f);
        sizeModule.addSizePoint(1.0f, 2.0f);

        return new DxParticleSystem.Builder()
                .position(position)
                .startLife(5.0f)
                .startSpeed(new Vector3f(0, 0.5f, 0))
                .startColor(new Vector4f(0.2f, 0.2f, 0.2f, 0.5f))
                .startScale(1.0f)
                .gravity(new Vector3f(0, 0.5f, 0)) // Smoke rises
                .looping(true)
                .duration(5.0f)
                .texture("smookalpha.png")
                .blendMode(BlendMode.ALPHA) // Explicitly set for clarity
                .emitter(emitterShape)
                .addModule(colorModule)
                .addModule(sizeModule)
                .build();
    }

    public static DxParticleSystem createFireEmitter(Vector3f position) {
        // Emitter Shape
        DxEmitterCone emitterShape = new DxEmitterCone();
        emitterShape.setAngle(10);
        emitterShape.setRadius(0.2f);

        // Color: Bright Yellow -> Orange -> Dark Red -> Fade
        ColorOverLifetime colorModule = new ColorOverLifetime();
        colorModule.addColorPoint(0.0f, new Vector4f(1.0f, 1.0f, 0.0f, 1.0f)); // Yellow
        colorModule.addColorPoint(0.4f, new Vector4f(1.0f, 0.5f, 0.0f, 1.0f)); // Orange
        colorModule.addColorPoint(0.8f, new Vector4f(0.5f, 0.0f, 0.0f, 0.5f)); // Dark Red
        colorModule.addColorPoint(1.0f, new Vector4f(0.1f, 0.1f, 0.1f, 0.0f)); // Fade to black

        // Size: Large -> Small
        SizeOverLifetime sizeModule = new SizeOverLifetime(false);
        sizeModule.addSizePoint(0.0f, 1.5f);
        sizeModule.addSizePoint(1.0f, 0.2f);

        return new DxParticleSystem.Builder()
                .position(position)
                .startLife(1.5f)
                .startSpeed(new Vector3f(0, 1.0f, 0))
                .startColor(new Vector4f(1.0f, 1.0f, 0.0f, 1.0f))
                .startScale(1.0f)
                .gravity(new Vector3f(0, 1.5f, 0)) // Fire rises fast
                .looping(true)
                .duration(2.0f)
                .texture("flame.png") // A suitable flame texture
                .blendMode(BlendMode.ADDITIVE) // Use additive blending for a glowing effect
                .emitter(emitterShape)
                .addModule(colorModule)
                .addModule(sizeModule)
                .build();
    }
}
