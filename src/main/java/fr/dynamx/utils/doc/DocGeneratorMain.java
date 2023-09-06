package fr.dynamx.utils.doc;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class DocGeneratorMain implements INamedObject
{
    public static void main(String[] args) {
// Créez une liste pour stocker les classes trouvées
        List<Class<?>> namedObjectImplementations = new ArrayList<>();

        // Obtenez le classloader courant
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Recherchez les ressources (fichiers .class) dans le classpath
            String resourcePath = "fr/dynamx/common"; // Remplacez par le chemin de votre package
            String resourcePrefix = resourcePath.replace('.', '/');
            InputStream inputStream = classLoader.getResourceAsStream(resourcePrefix);

//            if (inputStream != null) {
//                String className;
//                while ((className = getNextClassName(inputStream)) != null) {
//                    Class<?> clazz = classLoader.loadClass(className);
//                    System.out.println("Test " + clazz);
//                    if (INamedObject.class.isAssignableFrom(clazz)) {
//                        namedObjectImplementations.add(clazz);
//                    }
//                }
//            }
            if (inputStream != null) {
                byte[] classBytes;
                while ((classBytes = getNextClassBytes(inputStream)) != null) {
                    ClassReader classReader = new ClassReader(classBytes);
                    String className = classReader.getClassName().replace('/', '.');
                    Class<?> clazz = classLoader.loadClass(className);
                    System.out.println("Test " + clazz);
                    if (INamedObject.class.isAssignableFrom(clazz)) {
                        namedObjectImplementations.add(clazz);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Classes implémentant INamedObject :");
        for (Class<?> clazz : namedObjectImplementations) {
            System.out.println(clazz.getName());
        }
        System.out.println("======");
        for (Class<?> clazz : namedObjectImplementations) {
            SubInfoTypeAnnotationCache.getOrLoadData(clazz);
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPackName() {
        return null;
    }
    private static String getNextClassName(InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ClassNameVisitor classNameVisitor = new ClassNameVisitor();
        classReader.accept(classNameVisitor, ClassReader.SKIP_CODE);
        return classNameVisitor.getClassName();
    }

    private static byte[] getNextClassBytes(InputStream inputStream) throws IOException {
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1) {
            return null;
        }
        return buffer;
    }

    private static class ClassNameVisitor extends ClassVisitor {

        private String className;

        public ClassNameVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = Type.getObjectType(name).getClassName();
        }

        public String getClassName() {
            return className;
        }
    }
}
