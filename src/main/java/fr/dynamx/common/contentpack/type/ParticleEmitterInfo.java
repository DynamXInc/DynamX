package fr.dynamx.common.contentpack.type;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import net.minecraft.util.EnumParticleTypes;

import java.util.Collections;
import java.util.List;

@RegisteredSubInfoType(name = "emitter", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class ParticleEmitterInfo<T extends ISubInfoTypeOwner<T> & ParticleEmitterInfo.IParticleEmitterContainer> extends SubInfoType<T> {
    private final String emitterName;

    @PackFileProperty(configNames = "Type", type = DefinitionType.DynamXDefinitionTypes.PARTICLE_TYPE)
    public EnumParticleTypes particleType;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    public Vector3f position = new Vector3f();
    @PackFileProperty(configNames = "Velocity", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    public Vector3f velocity = new Vector3f();

    public ParticleEmitterInfo(ISubInfoTypeOwner<T> owner, String emitterName) {
        super(owner);
        if (emitterName.startsWith("Emitter_")) {
            this.emitterName = emitterName.substring("Emitter_".length());
        } else {
            this.emitterName = emitterName.substring("Emitter".length());
        }
    }

    public String getEmitterName() {
        return emitterName;
    }

    @Override
    public String getName() {
        return "ParticleEmitterInfo_" + emitterName;
    }

    @Override
    public void appendTo(T owner) {
        owner.addParticleEmitter(this);
    }

    public interface IParticleEmitterContainer {
        default void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        }

        default List<ParticleEmitterInfo<?>> getParticleEmitters() {
            return Collections.emptyList();
        }
    }
}
