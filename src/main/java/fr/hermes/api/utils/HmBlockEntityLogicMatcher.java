package fr.hermes.api.utils;

import fr.hermes.api.mc.blocks.HmTileEntity;
import fr.hermes.api.mc.entities.HmBlockEntityLogic;
import fr.hermes.api.mc.entities.HmModBlockEntity;

import java.util.function.Consumer;

public class HmBlockEntityLogicMatcher {
    public static <T extends HmBlockEntityLogic> Consumer<HmTileEntity> consumer(Class<T> clazz, Consumer<T> consumer) {
        return entity -> {
            if (is(entity, clazz)) {
                consumer.accept(cast(entity, clazz));
            }
        };
    }

    public static boolean is(HmTileEntity entity, Class<? extends HmBlockEntityLogic> clazz) {
        return entity instanceof HmModBlockEntity && clazz.isInstance(((HmModBlockEntity) entity).getLogic());
    }

    public static <T extends HmBlockEntityLogic> T cast(HmTileEntity entity, Class<T> clazz) {
        if (!is(entity, clazz)) {
            return null;
        }
        return (T) ((HmModBlockEntity) entity).getLogic();
    }
}
