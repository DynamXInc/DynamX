package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import net.minecraft.world.World;

public class CaterpillarEntity<T extends CaterpillarEntity.CaterpillarPhysicsHandler> extends CarEntity<T> {

    // [0;4] SkidInfo [4;8] Friction [8;12] longitudinal [12;16] lateral [16;20] getRotationDelta

    public CaterpillarEntity(World world) {
        super(world);
    }

    //caterpillar thing/client only
    public float trackProgress;
    public float prevAngle;

    public CaterpillarEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new CaterpillarPhysicsHandler(this);
    }

    public static class CaterpillarPhysicsHandler extends CarPhysicsHandler<CaterpillarEntity<?>> {
        public CaterpillarPhysicsHandler(CaterpillarEntity<?> entity) {
            super(entity);
        }
    }
}
