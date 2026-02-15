package fr.hermes.api.utils;

import fr.hermes.api.mc.entities.*;

import java.util.function.Consumer;

public class HmEntityLogicMatcher {
    /*private final Predicate<HmEntity> predicate;
    private final Function<HmEntity, T> caster;

    private HmEntityLogicMatcher(Predicate<HmEntity> predicate, Function<HmEntity, T> caster) {
        this.predicate = predicate;
        this.caster = caster;
    }

    public static <T extends HmEntityLogic> HmEntityLogicMatcher<T> matcher(Class<T> clazz) {
        return new HmEntityLogicMatcher<>(entity -> entity instanceof HmModEntity && clazz.isInstance(((HmModEntity) entity).getLogic()), entity -> (T) ((HmModEntity) entity).getLogic());
    }*/

    public static <T extends HmEntityLogic> Consumer<HmEntity> consumer(Class<T> clazz, Consumer<T> consumer) {
        return entity -> {
            if (is(entity, clazz)) {
                consumer.accept(cast(entity, clazz));
            }
        };
    }

    public static boolean is(HmEntity entity, Class<? extends HmEntityLogic> clazz) {
        return entity instanceof HmModEntity && clazz.isInstance(((HmModEntity) entity).getLogic());
    }

    public static <T extends HmEntityLogic> T cast(HmEntity entity, Class<T> clazz) {
        if(!is(entity, clazz)) {
            return null;
        }
        return (T) ((HmModEntity) entity).getLogic();
    }
}
