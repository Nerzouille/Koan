package koan.passkeys;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Supply an Activity only to the two native Credential Manager methods that launch UI. */
final class CredentialContextVisitor {
    static final String TARGET = "org.mozilla.gecko.WebAuthnCredentialManager";
    static final Map<String, String> METHODS = Map.of(
            "makeCredential", "(Ljava/lang/String;[BLjava/lang/String;)Lorg/mozilla/geckoview/GeckoResult;",
            "getAssertion", "(Landroid/credentials/PrepareGetCredentialResponse$PendingGetCredentialHandle;)Lorg/mozilla/geckoview/GeckoResult;");

    static ClassVisitor patch(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private final Set<String> found = new HashSet<>();
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, desc, signature, exceptions);
                if (!desc.equals(METHODS.get(name))) return method;
                if ((access & Opcodes.ACC_STATIC) == 0) throw new IllegalStateException("GeckoView credential UI method changed");
                found.add(name);
                return new MethodVisitor(Opcodes.ASM9, method) {
                    private int hooks;
                    private int returns;
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            returns++;
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "dev/nerzouille/koan/engine/auth/CredentialOutcome", "normalize",
                                    "(Lorg/mozilla/geckoview/GeckoResult;)Lorg/mozilla/geckoview/GeckoResult;", false);
                        }
                        super.visitInsn(opcode);
                    }
                    @Override public void visitMethodInsn(int opcode, String owner, String invoked, String descriptor, boolean itf) {
                        super.visitMethodInsn(opcode, owner, invoked, descriptor, itf);
                        if (opcode == Opcodes.INVOKESTATIC && "org/mozilla/gecko/GeckoAppShell".equals(owner) &&
                                "getApplicationContext".equals(invoked) && "()Landroid/content/Context;".equals(descriptor)) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/nerzouille/koan/engine/auth/CredentialUiHost",
                                    "context", "(Landroid/content/Context;)Landroid/content/Context;", false);
                            hooks++;
                        }
                    }
                    @Override public void visitEnd() {
                        if (hooks != 1 || returns == 0) throw new IllegalStateException("GeckoView credential UI contract changed");
                        super.visitEnd();
                    }
                };
            }
            @Override public void visitEnd() {
                if (!found.equals(METHODS.keySet())) throw new IllegalStateException("GeckoView credential UI entry points changed");
                super.visitEnd();
            }
        };
    }
}
