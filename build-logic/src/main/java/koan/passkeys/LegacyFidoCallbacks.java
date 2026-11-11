package koan.passkeys;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Follow the legacy task's callback edges, without depending on generated names or numbers. */
final class LegacyFidoCallbacks {
    private static final String RESULT = "Lorg/mozilla/geckoview/GeckoResult;";
    private static final String CAPTURE = "(" + RESULT + ")";
    private static final String SUCCESS = "Lcom/google/android/gms/tasks/OnSuccessListener;";
    private static final String CONSUMER = "Lorg/mozilla/geckoview/GeckoResult$Consumer;";

    static ClassVisitor patch(ClassVisitor next) {
        ClassWriter buffer = new ClassWriter(0);
        Set<String> handlers = new HashSet<>();
        Set<String> operations = new HashSet<>();
        return new ClassVisitor(Opcodes.ASM9, buffer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, desc, signature, exceptions);
                boolean legacy = "makeCredential".equals(name) || "getAssertion".equals(name);
                if (!legacy || !desc.equals(PasskeyCapabilityVisitorFactory.METHODS.get(name))) return method;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override public void visitInvokeDynamicInsn(String invoked, String descriptor, Handle bootstrap, Object... args) {
                        if ("onSuccess".equals(invoked) && (CAPTURE + SUCCESS).equals(descriptor)) {
                            Handle target = implementation(bootstrap, args);
                            if (!handlers.add(target.getName() + target.getDesc()) || !operations.add(name)) {
                                throw new IllegalStateException("Ambiguous GeckoView FIDO callback");
                            }
                        }
                        super.visitInvokeDynamicInsn(invoked, descriptor, bootstrap, args);
                    }
                };
            }

            @Override public void visitEnd() {
                super.visitEnd();
                if (handlers.size() != 2 || !operations.equals(Set.of("makeCredential", "getAssertion"))) {
                    throw new IllegalStateException("GeckoView FIDO task callbacks changed");
                }
                // Buffer first so generated methods may appear in any order in the class file.
                new ClassReader(buffer.toByteArray()).accept(adapt(next, handlers), 0);
            }
        };
    }

    private static Handle implementation(Handle bootstrap, Object[] args) {
        if (!"java/lang/invoke/LambdaMetafactory".equals(bootstrap.getOwner()) ||
                args.length != 3 || !(args[1] instanceof Handle target) ||
                target.getTag() != Opcodes.H_INVOKESTATIC ||
                !PasskeyCapabilityVisitorFactory.TARGET.replace('.', '/').equals(target.getOwner())) {
            throw new IllegalStateException("GeckoView FIDO callback factory changed");
        }
        return target;
    }

    private static ClassVisitor adapt(ClassVisitor next, Set<String> handlers) {
        Set<String> adapted = new HashSet<>();
        return new ClassVisitor(Opcodes.ASM9, next) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, desc, signature, exceptions);
                if (!handlers.contains(name + desc)) return method;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    private int hooks;
                    @Override public void visitInvokeDynamicInsn(String invoked, String descriptor, Handle bootstrap, Object... args) {
                        if ("accept".equals(invoked) && (CAPTURE + CONSUMER).equals(descriptor) &&
                                args.length == 3 && Type.getMethodType("(Ljava/lang/Throwable;)V").equals(args[2])) {
                            Handle target = implementation(bootstrap, args);
                            if (!("(" + RESULT + "Ljava/lang/Throwable;)V").equals(target.getDesc())) {
                                throw new IllegalStateException("GeckoView Activity failure capture changed");
                            }
                            // Consume the same captured result, but create our failure consumer.
                            // Native success parsing and the original failure-handler body stay intact.
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "dev/nerzouille/koan/engine/auth/LegacyFidoResult", "activityFailure",
                                    CAPTURE + CONSUMER, false);
                            hooks++;
                            return;
                        }
                        super.visitInvokeDynamicInsn(invoked, descriptor, bootstrap, args);
                    }

                    @Override public void visitEnd() {
                        if (hooks != 1) throw new IllegalStateException("GeckoView Activity callback edges changed");
                        adapted.add(name + desc);
                        super.visitEnd();
                    }
                };
            }

            @Override public void visitEnd() {
                if (!adapted.equals(handlers)) throw new IllegalStateException("Missing GeckoView FIDO callback");
                super.visitEnd();
            }
        };
    }
}
