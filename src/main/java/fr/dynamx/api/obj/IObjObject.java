package fr.dynamx.api.obj;

import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.obj.Mesh;
import fr.dynamx.common.obj.texture.TextureData;

import javax.vecmath.Vector3f;

/**
 * An object object is a part of an obj model <br>
 *      Interface used for protection system
 */
public interface IObjObject
{
    /**
     * @return Name of the part
     */
    String getName();

    /**
     * @return Center of the part (average of all vertices)
     */
    Vector3f getCenter();

    /**
     * Sets the center of the part, used internally
     */
    void setCenter(Vector3f center);

    /**
     * @return The structure of the object
     */
    Mesh getMesh();

    /**
     * Clears any computed render info
     */
    default void clearDisplayLists() {}

    /**
     * Pre-computes render info
     *
     * @param useDefault The texture to use if textureData is not available for this part
     * @param textureData The texture to use
     * @param model The model owning this object
     * @param logIfNotFound True to log texture errors
     */
    default void createList(TextureData useDefault, TextureData textureData, ObjModelClient model, boolean logIfNotFound) {}

    /**
     * Pre-computes default (texture 0) render info
     *
     * @param model The model owning this object
     */
    default void createDefaultList(ObjModelClient model) {}

    /**
     * Renders this part
     *
     * @param model The model owning this object
     * @param textureDataId The texture to use
     */
    default void render(ObjModelClient model, byte textureDataId) {}

    /**
     * Provides obj objects <br>
     *      Interface used for protection system
     */
    interface ObjObjectProvider
    {
        IObjObject createObject(String name);
    }
}
