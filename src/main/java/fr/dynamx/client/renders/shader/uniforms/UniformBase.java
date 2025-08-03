package fr.dynamx.client.renders.shader.uniforms;

import lombok.Getter;
import lombok.Setter;
import net.optifine.shaders.Shaders;


@Getter
public abstract class UniformBase {

    @Setter
    protected String name;

    public UniformBase(String name) {
        this.name = name;
    }

    abstract void upload(int location);

    public void sendValueTo(int location){
        if(location == -1){
            return;
        }
        upload(location);
        Shaders.checkGLError("Uniform : " + name + " : " + location);
    }

}
