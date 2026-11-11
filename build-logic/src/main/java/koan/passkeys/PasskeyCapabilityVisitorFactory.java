package koan.passkeys;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * GeckoView 157 uses GMS for isUVPAA even when authentication uses Android Credential Manager.
 * Add the platform check before its GMS fallback, and fail promptly if that service is absent.
 * No website APIs, origin validation, or Credential Manager signing flows are replaced.
 * Remove this adapter when GeckoView handles Credential Manager in its own capability probe.
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
        return TARGET.equals(data.getClassName());
    }

    @Override public ClassVisitor createClassVisitor(ClassContext context, ClassVisitor next) {
        return patch(next);
    }

    static ClassVisitor patch(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
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
        };
    }
}
