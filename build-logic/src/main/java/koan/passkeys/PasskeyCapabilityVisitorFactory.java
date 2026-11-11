package koan.passkeys;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * GeckoView 157 uses GMS for isUVPAA even when authentication uses Android Credential Manager.
 * Add the platform check before its GMS fallback, and fail promptly if that service is absent.
 * No website APIs, origin validation, or Credential Manager signing flows are replaced.
 * Remove each compatibility behavior when GeckoView implements it natively.
 */
public abstract class PasskeyCapabilityVisitorFactory
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    static final String TARGET = "org.mozilla.geckoview.WebAuthnTokenManager";
    static final String METHOD = "webAuthnIsUserVerifyingPlatformAuthenticatorAvailable";
    static final String DESCRIPTOR = "()Lorg/mozilla/geckoview/GeckoResult;";
    static final Map<String, String> METHODS = Map.of(
            METHOD, DESCRIPTOR,
            "makeCredential", "(Lorg/mozilla/gecko/util/GeckoBundle;[B[B[Lorg/mozilla/gecko/util/WebAuthnUtils$WebAuthnPublicCredential;Lorg/mozilla/gecko/util/GeckoBundle;Lorg/mozilla/gecko/util/GeckoBundle;)Lorg/mozilla/geckoview/GeckoResult;",
            "getAssertion", "([B[Lorg/mozilla/gecko/util/WebAuthnUtils$WebAuthnPublicCredential;Lorg/mozilla/gecko/util/GeckoBundle;Lorg/mozilla/gecko/util/GeckoBundle;)Lorg/mozilla/geckoview/GeckoResult;",
            "hasCredentialInGMS", "(Ljava/lang/String;[Lorg/mozilla/gecko/util/WebAuthnUtils$WebAuthnPublicCredential;)Lorg/mozilla/geckoview/GeckoResult;");

    @Override public boolean isInstrumentable(ClassData data) {
        return TARGET.equals(data.getClassName()) || CredentialContextVisitor.TARGET.equals(data.getClassName());
    }

    @Override public ClassVisitor createClassVisitor(ClassContext context, ClassVisitor next) {
        if (CredentialContextVisitor.TARGET.equals(context.getCurrentClassData().getClassName())) {
            return CredentialContextVisitor.patch(next);
        }
        return patch(next);
    }

    static ClassVisitor patch(ClassVisitor next) {
        return LegacyFidoCallbacks.patch(new ClassVisitor(Opcodes.ASM9, next) {
            private final Set<String> found = new HashSet<>();

            @Override public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor nextMethod = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!descriptor.equals(METHODS.get(name))) return nextMethod;
                if ((access & Opcodes.ACC_STATIC) == 0) {
                    throw new IllegalStateException("GeckoView WebAuthn probe changed; review the adapter");
                }
                found.add(name);
                return new MethodVisitor(Opcodes.ASM9, nextMethod) {
                    private int lastLoaded = -1;
                    private int resultLocal = -1;
                    private int failureHooks = 0;

                    @Override public void visitVarInsn(int opcode, int slot) {
                        if (opcode == Opcodes.ALOAD) lastLoaded = slot;
                        super.visitVarInsn(opcode, slot);
                    }

                    @Override public void visitInvokeDynamicInsn(String invokedName, String desc,
                            Handle bootstrap, Object... arguments) {
                        if (legacyRequest() && "onSuccess".equals(invokedName) &&
                                "(Lorg/mozilla/geckoview/GeckoResult;)Lcom/google/android/gms/tasks/OnSuccessListener;".equals(desc)) {
                            resultLocal = lastLoaded;
                        }
                        super.visitInvokeDynamicInsn(invokedName, desc, bootstrap, arguments);
                    }

                    @Override public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        super.visitMethodInsn(opcode, owner, method, desc, itf);
                        if (legacyRequest() && "com/google/android/gms/tasks/Task".equals(owner) &&
                                "addOnSuccessListener".equals(method) &&
                                "(Lcom/google/android/gms/tasks/OnSuccessListener;)Lcom/google/android/gms/tasks/Task;".equals(desc)) {
                            if (resultLocal < 0) throw new IllegalStateException("GeckoView FIDO result capture changed");
                            mv.visitVarInsn(Opcodes.ALOAD, resultLocal);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/nerzouille/koan/engine/auth/LegacyFidoResult",
                                    "getAssertion".equals(name) ? "observe" : "observeCancellation",
                                    "(Lcom/google/android/gms/tasks/Task;Lorg/mozilla/geckoview/GeckoResult;)Lcom/google/android/gms/tasks/Task;", false);
                            failureHooks++;
                        }
                    }

                    @Override public void visitEnd() {
                        if (legacyRequest() && failureHooks != 1) {
                            throw new IllegalStateException("GeckoView FIDO task changed; review failure handling");
                        }
                        super.visitEnd();
                    }

                    private boolean legacyRequest() {
                        return "getAssertion".equals(name) || "makeCredential".equals(name);
                    }

                    @Override public void visitCode() {
                        super.visitCode();
                        if (METHOD.equals(name)) {
                            checkService("hasPlatformAuthenticator");
                            Label legacy = new Label();
                            mv.visitJumpInsn(Opcodes.IFEQ, legacy);
                            booleanResult(true);
                            mv.visitLabel(legacy);
                        }
                        checkService("canAttemptGoogleFido");
                        Label available = new Label();
                        mv.visitJumpInsn(Opcodes.IFNE, available);
                        if (METHOD.equals(name) || "hasCredentialInGMS".equals(name)) {
                            booleanResult(false);
                        } else {
                            // Don't leave a website waiting for a disabled/missing GMS service.
                            String error = "org/mozilla/gecko/util/WebAuthnUtils$Exception";
                            mv.visitTypeInsn(Opcodes.NEW, error);
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitLdcInsn("NOT_SUPPORTED_ERR");
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, error, "<init>", "(Ljava/lang/String;)V", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/geckoview/GeckoResult",
                                    "fromException", "(Ljava/lang/Throwable;)Lorg/mozilla/geckoview/GeckoResult;", false);
                            mv.visitInsn(Opcodes.ARETURN);
                        }
                        mv.visitLabel(available);
                    }

                    private void checkService(String helper) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/gecko/GeckoAppShell",
                                "getApplicationContext", "()Landroid/content/Context;", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "dev/nerzouille/koan/engine/auth/PasskeyCapabilities", helper,
                                "(Landroid/content/Context;)Z", false);
                    }

                    private void booleanResult(boolean value) {
                        mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",
                                "valueOf", "(Z)Ljava/lang/Boolean;", false);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/geckoview/GeckoResult",
                                "fromValue", "(Ljava/lang/Object;)Lorg/mozilla/geckoview/GeckoResult;", false);
                        mv.visitInsn(Opcodes.ARETURN);
                    }
                };
            }

            @Override public void visitEnd() {
                if (!found.equals(METHODS.keySet())) throw new IllegalStateException(
                        "GeckoView WebAuthn methods changed; review/remove the credential adapter");
                super.visitEnd();
            }
        });
    }
}
