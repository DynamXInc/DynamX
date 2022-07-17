package fr.dynamx.common.core.asm;

import fr.dynamx.common.core.DynamXCoreMod;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class EntityLivingBasePatcher implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.entity.EntityLivingBase")) {
            DynamXCoreMod.LOG.info("Patching " + transformedName + " ! (V.1.0)");

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            MethodNode mnode = null; //Find the method
            for (MethodNode node : classNode.methods) {
                if (node.name.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "A" : "dismountEntity") && node.desc.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvg;)V" : "(Lnet/minecraft/entity/Entity;)V")) {
                    mnode = node;
                    break;
                }
            }
            if (mnode == null) {
                DynamXCoreMod.LOG.warn("The function 'dismountEntity' wasn't found, aborting !");
                return basicClass;
            }
            //    ALOAD 0
            //    ALOAD 1
            //    INVOKESTATIC fr/dynamx/common/core/DismountHelper.dismount (Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)V
            InsnList instr = mnode.instructions;
            instr.insert(new InsnNode(Opcodes.RETURN));
            instr.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "fr/dynamx/common/core/DismountHelper", "preDismount",
                    EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvp;Lvg;)V" : "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/Entity;)V", true)); //Call the custom method
            instr.insert(new VarInsnNode(Opcodes.ALOAD, 1));
            instr.insert(new VarInsnNode(Opcodes.ALOAD, 0));

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw); //Patch
            DynamXCoreMod.LOG.info("EntityLivingBase patched");
            return cw.toByteArray();
        }
		/*else if(transformedName.equals("net.minecraftforge.client.model.ModelLoader"))
		{
			DynamXCoreMod.LOG.info("Patching " + transformedName + " ! (V.1.0)");

			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(basicClass);
			classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

			MethodNode mnode = null; //Find the method
			for(MethodNode node : classNode.methods)
			{
				//TODOOLD OBF NAMES
				if(node.name.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "A" : "setupModelRegistry") && node.desc.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvg;)V" : "()Lnet/minecraft/util/registry/IRegistry;"))
				{
					mnode = node;
					break;
				}
			}
			if(mnode == null)
			{
				DynamXCoreMod.LOG.warn("The function 'setupModelRegistry' wasn't found, aborting !");
				return basicClass;
			}
			//    ALOAD 0
			//    INVOKESTATIC fr/aym/threadedloder/ThreadMcModelBaker.setupModelRegistry (Lfr/dynamx/client/ClientProxy;)Lnet/minecraft/util/registry/IRegistry;
			//    ARETURN
			InsnList instr = mnode.instructions;
			instr.insert(new InsnNode(Opcodes.ARETURN));
			instr.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "fr/aym/threadedloder/ThreadMcModelBaker", "setupModelRegistry",
					EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvp;Lvg;)V" : "(Lnet/minecraftforge/client/model/ModelLoader;)Lnet/minecraft/util/registry/IRegistry;", true)); //Call the custom method //TODOOLD OBF NAMES
			instr.insert(new VarInsnNode(Opcodes.ALOAD, 0));

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(cw); //Patch
			DynamXCoreMod.LOG.info("ModelLoader patched");
			return cw.toByteArray();
		}
		else if(transformedName.equals("net.minecraft.client.renderer.block.model.ModelManager"))
		{
			DynamXCoreMod.LOG.info("Patching " + transformedName + " ! (V.1.0)");

			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(basicClass);
			classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

			MethodNode mnode = null; //Find the method
			for(MethodNode node : classNode.methods)
			{
				//TODOOLD OBF NAMES
				if(node.name.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "A" : "onResourceManagerReload") && node.desc.equals(EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvg;)V" : "(Lnet/minecraft/client/resources/IResourceManager;)V"))
				{
					mnode = node;
					break;
				}
			}
			if(mnode == null)
			{
				DynamXCoreMod.LOG.warn("The function 'setupModelRegistry' wasn't found, aborting !");
				return basicClass;
			}
			//
			InsnList instr = mnode.instructions;
			instr.insert(new InsnNode(Opcodes.RETURN));
			instr.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "fr/aym/threadedloder/ThreadMcModelBaker", "onResourceManagerReload",
					EntityPatcher.runtimeDeobfuscationEnabled ? "(Lvp;Lvg;)V" : "(Lnet/minecraft/client/renderer/block/model/ModelManager;Lnet/minecraft/client/resources/IResourceManager;)V", true)); //Call the custom method //TODOOLD OBF NAMES
			instr.insert(new VarInsnNode(Opcodes.ALOAD, 1));
			instr.insert(new VarInsnNode(Opcodes.ALOAD, 0));

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(cw); //Patch
			DynamXCoreMod.LOG.info("ModelManager patched");
			return cw.toByteArray();
		}*/
        return basicClass;
    }
}
