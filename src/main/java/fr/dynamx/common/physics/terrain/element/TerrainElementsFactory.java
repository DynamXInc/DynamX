package fr.dynamx.common.physics.terrain.element;

import fr.dynamx.api.physics.terrain.ITerrainElement;

import java.util.concurrent.Callable;

/**
 * All implemented DynamX {@link ITerrainElement}
 */
public enum TerrainElementsFactory {
    ERRORED(() -> {
        throw new IllegalStateException("Cannot load an errored terrain element !");
    }),
    COMPOUND_BOX(CompoundBoxTerrainElement::new),
    CUSTOM_SLOPE(() -> new CustomSlopeTerrainElement((byte) 1)),
    COMPOUND_STAIRS(CompoundStairsTerrainElement::new),
    DYNAMX_BLOCK(DynamXBlockTerrainElement::new);

    private final Callable<ITerrainElement> factory;

    TerrainElementsFactory(Callable<ITerrainElement> factory) {
        this.factory = factory;
    }

    public ITerrainElement call() throws Exception {
        return factory.call();
    }

    public static ITerrainElement getById(byte id) throws Exception {
        if (id > 0 && id < values().length)
            return values()[id].call();
        return ERRORED.call();
    }
}
