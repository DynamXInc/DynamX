package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IMovableModuleContainer;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.PropPhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

public class PropsEntity<T extends PackEntityPhysicsHandler<PropObject<?>, ?>> extends PackPhysicsEntity<T, PropObject<?>> implements IMovableModuleContainer {
    private final List<MutableBoundingBox> unrotatedBoxes = new ArrayList<>();

    public PropsEntity(World worldIn) {
        super(worldIn);
    }

    public PropsEntity(String infoName, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(infoName, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    protected PropObject<?> createInfo(String infoName) {
        return DynamXObjectLoaders.PROPS.findInfo(infoName);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (getPackInfo() != null) {
            if (getPackInfo().getDespawnTime() != -1) {
                if ((ticksExisted % getPackInfo().getDespawnTime()) == 0) {
                    setDead();
                }
            }
        }
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new PropPhysicsHandler(this);
    }

    @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateEntityModulesEvent<>(PropsEntity.class, this, moduleList, side));
    }

    @Override
    public int getSyncTickRate() {
        return DynamXConfig.propsSyncTickRate;
    }

    @Override
    public boolean isInRangeToRenderDist(double range) {
        //Fix npe due to render before first update
        return getPackInfo() != null && getPackInfo().getRenderDistance() >= range;
    }

    /**
     * Cache
     */
    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        if (unrotatedBoxes.size() != getPackInfo().getCollisionBoxes().size()) {
            unrotatedBoxes.clear();
            for (MutableBoundingBox shape : getPackInfo().getCollisionBoxes()) {
                MutableBoundingBox b = new MutableBoundingBox(shape);
                b.offset(physicsPosition);
                unrotatedBoxes.add(b);
            }
        } else {
            for (int i = 0; i < getPackInfo().getCollisionBoxes().size(); i++) {
                MutableBoundingBox b = unrotatedBoxes.get(i);
                b.setTo(getPackInfo().getCollisionBoxes().get(i));
                b.offset(physicsPosition);
                unrotatedBoxes.add(b);
            }
        }
        return unrotatedBoxes;
    }
}
