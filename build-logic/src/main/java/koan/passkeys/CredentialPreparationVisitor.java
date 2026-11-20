package koan.passkeys;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Find the typed public preparation callback, independent of its generated class number. */
final class CredentialPreparationVisitor {
    static final String RESPONSE = "android/credentials/PrepareGetCredentialResponse";
    static final String CALLBACK = "(L" + RESPONSE + ";)V";

    static ClassVisitor patch(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, desc, signature, exceptions);
                if (!"onResult".equals(name) || !CALLBACK.equals(desc)) return method;
                if ((access & Opcodes.ACC_STATIC) != 0) throw new IllegalStateException("GeckoView preparation callback changed");
                return new MethodVisitor(Opcodes.ASM9, method) {
                    private int hooks;
                    @Override public void visitMethodInsn(int opcode, String owner, String invoked, String descriptor, boolean itf) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && RESPONSE.equals(owner) &&
                                "hasCredentialResults".equals(invoked) && "(Ljava/lang/String;)Z".equals(descriptor)) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "dev/nerzouille/koan/engine/auth/CredentialPreparation", "hasCredentialOrRemote",
                                    "(L" + RESPONSE + ";Ljava/lang/String;)Z", false);
                            hooks++;
                        } else super.visitMethodInsn(opcode, owner, invoked, descriptor, itf);
                    }
                    @Override public void visitEnd() {
                        if (hooks != 1) throw new IllegalStateException("GeckoView preparation result check changed");
                        super.visitEnd();
                    }
                };
            }
        };
    }
}
