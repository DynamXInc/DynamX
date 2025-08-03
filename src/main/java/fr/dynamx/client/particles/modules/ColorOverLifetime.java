package fr.dynamx.client.particles.modules;

import fr.dynamx.client.particles.DxParticle;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.util.TreeMap;

public class ColorOverLifetime extends DxParticleModule {

    private final TreeMap<Float, Vector4f> colors = new TreeMap<>();

    public ColorOverLifetime() {
        super();
    }

    public void addColorPoint(float lifeFraction, Vector4f color) {
        colors.put(lifeFraction, color);
    }

    public void addColorPoint(float lifeFraction, Vector4i color) {
        addColorPoint(lifeFraction, new Vector4f(color.x / 255f, color.y / 255f, color.z / 255f, color.w / 255f));
    }

    public void getColor(float lifeFraction, Vector4f store) {
        if (colors.isEmpty()) {
            store.set(1, 1, 1, 1);
            return;
        }
        Float lowerKey = colors.floorKey(lifeFraction);
        Float upperKey = colors.ceilingKey(lifeFraction);

        if (lowerKey == null) {
            store.set(colors.firstEntry().getValue());
            return;
        }
        if (upperKey == null) {
            store.set(colors.lastEntry().getValue());
            return;
        }

        Vector4f lowerColor = colors.get(lowerKey);
        Vector4f upperColor = colors.get(upperKey);

        if (lowerKey.equals(upperKey)) {
            store.set(lowerColor);
            return;
        }

        float delta = (lifeFraction - lowerKey) / (upperKey - lowerKey);
        store.set(
                lowerColor.x + (upperColor.x - lowerColor.x) * delta,
                lowerColor.y + (upperColor.y - lowerColor.y) * delta,
                lowerColor.z + (upperColor.z - lowerColor.z) * delta,
                lowerColor.w + (upperColor.w - lowerColor.w) * delta
        );
    }

    @Override
    public void transformParticle(DxParticle particle) {
        float lifeFraction = 1.0f - (particle.getLife() / system.getStartLife());
        getColor(lifeFraction, particle.getColor());
    }
}
