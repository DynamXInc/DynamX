package fr.dynamx.client.particles.modules;

import fr.dynamx.client.particles.DxParticle;

import java.util.TreeMap;

public class SizeOverLifetime extends DxParticleModule {

    private final TreeMap<Float, Float> sizes = new TreeMap<>();
    private boolean separateAxes = false;

    public SizeOverLifetime(boolean separateAxes) {
        super();
        this.separateAxes = separateAxes;
    }

    public void addSizePoint(float lifeFraction, float sizeFraction) {
        sizes.put(lifeFraction, sizeFraction);
    }

    public float getSizeFraction(float lifeFraction) {
        if(sizes.isEmpty())
            return 1.0f;
        Float lowerKey = sizes.floorKey(lifeFraction);
        Float upperKey = sizes.ceilingKey(lifeFraction);

        if (lowerKey == null) return sizes.firstEntry().getValue();
        if (upperKey == null) return sizes.lastEntry().getValue();

        float lowerSize = sizes.get(lowerKey);
        float upperSize = sizes.get(upperKey);

        if (lowerKey.equals(upperKey)) {
            return lowerSize;
        }

        float delta = (lifeFraction - lowerKey) / (upperKey - lowerKey);
        return lowerSize + (upperSize - lowerSize) * delta;
    }

    @Override
    public void transformParticle(DxParticle particle) {
        float lifeFraction = 1.0f - (particle.getLife() / system.getStartLife());
        float sizeFraction = getSizeFraction(lifeFraction);
        float newSize = sizeFraction * particle.getInitialScale();

        if (separateAxes) {
           // particle.setScale(new Vector3f(newSize, newSize, newSize));
        } else {
            particle.setScale(newSize);
        }
    }
}