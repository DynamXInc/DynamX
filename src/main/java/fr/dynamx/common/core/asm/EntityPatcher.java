package fr.dynamx.common.core.asm;

import fr.dynamx.common.core.DynamXCoreMod;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class EntityPatcher implements IClassTransformer {
    public static boolean runtimeDeobfuscationEnabled;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.entity.Entity")) {
            DynamXCoreMod.LOG.info("Patching " + transformedName + " ! (V.1.0)");

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

            MethodNode mnode = null; //Find the method
            for (MethodNode node : classNode.methods) {
                if (node.name.equals(runtimeDeobfuscationEnabled ? "a" : "move") && node.desc.equals(runtimeDeobfuscationEnabled ? "(Lvv;DDD)V" : "(Lnet/minecraft/entity/MoverType;DDD)V")) {
                    mnode = node;
                    break;
                }
            }
            if (mnode == null) {
                DynamXCoreMod.LOG.warn("The function 'move' wasn't found, aborting !");
                return basicClass;
            }
            //mnode.visitVarInsn(Opcodes.AALOAD, 0);
            //mnode.visitMethodInsn(Opcodes.INVOKESTATIC, "fr/mvp/core/AABBCollisionHandler", "postProcessEntityMove",
            //		"(Lnet/minecraft/entity/Entity;)V");
            InsnList instr = mnode.instructions;
            instr.insert(new InsnNode(Opcodes.RETURN));
            instr.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "fr/dynamx/common/core/AABBCollisionHandler", "vanillaMove",
                    runtimeDeobfuscationEnabled ? "(Lvg;Lvv;DDD)V" : "(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/MoverType;DDD)V", true)); //Call the custom method
            instr.insert(new VarInsnNode(Opcodes.DLOAD, 6));
            instr.insert(new VarInsnNode(Opcodes.DLOAD, 4));
            instr.insert(new VarInsnNode(Opcodes.DLOAD, 2));
            instr.insert(new VarInsnNode(Opcodes.ALOAD, 1));
            instr.insert(new VarInsnNode(Opcodes.ALOAD, 0));

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw); //Patch
            DynamXCoreMod.LOG.info("Entity patched");
            return cw.toByteArray();
        }
        return basicClass;
    }
}
