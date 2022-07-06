package fr.aym.acslib.services.impl.stats.core;

import fr.dynamx.common.core.asm.EntityPatcher;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Compatible with Mc 1.11.2 and 1.12
 */
public class StatsBotCorePlugin implements IClassTransformer
{
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if(transformedName.equals("net.minecraftforge.fml.common.FMLCommonHandler"))
		{
			System.out.println("Patching " + transformedName + " ! (V.1.0)");
			
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(basicClass);
			classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

			MethodNode mnode = null; //Find the method
			for(MethodNode node : classNode.methods)
			{
				if(node.name.equals("enhanceCrashReport"))
				{
					mnode = node;
					break;
				}
			}
			if(mnode == null)
			{
				System.out.println("The function 'enhanceCrashReport' wasn't found, aborting !");
				return basicClass;
			}
			InsnList instr = mnode.instructions;

			//StatsBotManager b = StatsBotManager.INSTANCE;
			
			//instr.insert(new InsnNode(Opcodes.RETURN)); //Insert a return to block vanilla code
			instr.insert(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "fr/aym/acslib/services/impl/stats/StatsBotService", "reportCrash", "(Ljava/lang/Throwable;)V", false));
			instr.insert(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EntityPatcher.runtimeDeobfuscationEnabled?"b":"net/minecraft/crash/CrashReport", EntityPatcher.runtimeDeobfuscationEnabled?"b":"getCrashCause", "()Ljava/lang/Throwable;", false));
			instr.insert(new VarInsnNode(Opcodes.ALOAD, 1));
			instr.insert(new FieldInsnNode(Opcodes.GETSTATIC, "fr/aym/acslib/services/impl/stats/StatsBotService", "INSTANCE", "Lfr/aym/acslib/services/impl/stats/StatsBotService;"));

			mnode = null; //Find the method
			for(MethodNode node : classNode.methods)
			{
				if(node.name.equals("handleExit"))
				{
					mnode = node;
					break;
				}
			}
			if(mnode == null)
			{
				System.out.println("The function 'handleExit' wasn't found, aborting !");
				return basicClass;
			}
			instr = mnode.instructions;

			//StatsBotManager b = StatsBotManager.INSTANCE;

			//instr.insert(new InsnNode(Opcodes.RETURN)); //Insert a return to block vanilla code
			instr.insert(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "fr/aym/acslib/services/impl/stats/StatsBotService", "onExit", "()V", false));
			instr.insert(new FieldInsnNode(Opcodes.GETSTATIC, "fr/aym/acslib/services/impl/stats/StatsBotService", "INSTANCE", "Lfr/aym/acslib/services/impl/stats/StatsBotService;"));

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(cw); //Patch
			System.out.println("FMLCommonHandler patched");
			return cw.toByteArray();
		}
		return basicClass;
	}
}
