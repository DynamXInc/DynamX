package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collision data of a chunk
 */
public class ChunkTerrain
{
    private final List<ITerrainElement> elements;
    private final List<ITerrainElement.IPersistentTerrainElement> persistentElements;

    public ChunkTerrain() {
        this(new ArrayList<>(), new ArrayList<>());
    }
    public ChunkTerrain(List<ITerrainElement.IPersistentTerrainElement> persistentElements) {
        this(new ArrayList<>(), persistentElements);
    }
    public ChunkTerrain(List<ITerrainElement> elements, List<ITerrainElement.IPersistentTerrainElement> persistentElements) {
        this.elements = elements;
        this.persistentElements = persistentElements;
    }

    public List<ITerrainElement> getElements() {
        return elements;
    }

    public List<ITerrainElement.IPersistentTerrainElement> getPersistentElements() {
        return persistentElements;
    }

    /**
     * @return An unmodifiable copy of all elements of this chunk
     */
    public ChunkTerrain unmodifiableCopy() {
        return new ChunkTerrain(Collections.unmodifiableList(elements), Collections.unmodifiableList(persistentElements));
    }

    /**
     * @return The list of the elements of the given type. If type is ALL, a copy is returned.
     */
    public List<ITerrainElement> getElements(TerrainElementType type) {
        switch (type)
        {
            case ALL:
                List<ITerrainElement> elements = new ArrayList<>(this.elements);
                elements.addAll(this.persistentElements);
                return elements;
            case COMPUTED_TERRAIN:
                return this.elements;
            case PERSISTENT_ELEMENTS:
                return (List<ITerrainElement>) (List<?>) this.persistentElements;
        }
        return null;
    }
}
