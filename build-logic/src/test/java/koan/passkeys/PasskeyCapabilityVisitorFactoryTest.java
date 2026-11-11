package koan.passkeys;

import static org.junit.Assert.*;
import java.lang.reflect.Method;
import org.junit.Test;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.util.GeckoBundle;
import org.mozilla.gecko.util.WebAuthnUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import dev.nerzouille.koan.engine.auth.PasskeyCapabilities;
import dev.nerzouille.koan.engine.auth.LegacyFidoResult;

public class PasskeyCapabilityVisitorFactoryTest {
    @Test public void platformSupportWinsAndLegacyFallbackIsPreserved() throws Exception {
        Class<?> type = transformed(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR));
        Method probe = type.getDeclaredMethod(PasskeyCapabilityVisitorFactory.METHOD);
        probe.setAccessible(true);
        GeckoAppShell.legacyCalls = 0;
        PasskeyCapabilities.googleAvailable = true;
        PasskeyCapabilities.available = true;
        assertEquals(Boolean.TRUE, ((GeckoResult) probe.invoke(null)).value);
        assertEquals(0, GeckoAppShell.legacyCalls);
        PasskeyCapabilities.available = false;
        assertEquals(Boolean.FALSE, ((GeckoResult) probe.invoke(null)).value);
        assertEquals(1, GeckoAppShell.legacyCalls);
        PasskeyCapabilities.googleAvailable = false;
        GeckoAppShell.legacyCalls = 0;
        assertEquals(Boolean.FALSE, ((GeckoResult) probe.invoke(null)).value);
        assertEquals(0, GeckoAppShell.legacyCalls);
        assertEquals(42, type.getMethod("unrelated").invoke(null));
    }

    @Test public void missingGoogleServiceRejectsLegacyRequestWithoutCallingIt() throws Exception {
        Class<?> type = transformed(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR));
        Method request = type.getDeclaredMethod("getAssertion", byte[].class,
                WebAuthnUtils.WebAuthnPublicCredential[].class, GeckoBundle.class, GeckoBundle.class);
        request.setAccessible(true);
        PasskeyCapabilities.googleAvailable = false;
        GeckoAppShell.legacyCalls = 0;
        GeckoResult result = (GeckoResult) request.invoke(null, new Object[]{null, null, null, null});
        assertEquals("NOT_SUPPORTED_ERR", result.error.getMessage());
        assertEquals(0, GeckoAppShell.legacyCalls);
        PasskeyCapabilities.googleAvailable = true;
        GeckoResult legacy = (GeckoResult) request.invoke(null, new Object[]{null, null, null, null});
        assertEquals(Boolean.FALSE, legacy.value);
        assertSame(legacy, LegacyFidoResult.observed);
        assertEquals(1, GeckoAppShell.legacyCalls);
    }

    @Test public void changedEngineSignatureFailsRatherThanSilentlySkippingPatch() {
        assertThrows(IllegalStateException.class, () -> transformed(original("()Ljava/lang/Object;")));
    }

    @Test public void canceledLegacyActivityIsReportedAsAbort() throws Exception {
        verifyActivityFailure(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR));
    }

    @Test public void generatedCallbackNamesAndMethodOrderDoNotMatter() throws Exception {
        verifyActivityFailure(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR, "renamed", true));
    }

    @Test public void changedFailureConsumerContractFailsTheBuild() {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR)).accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override public void visitInvokeDynamicInsn(String invoked, String descriptor, Handle bootstrap, Object... args) {
                        if ("accept".equals(invoked)) args[2] = Type.getMethodType("(Ljava/lang/RuntimeException;)V");
                        super.visitInvokeDynamicInsn(invoked, descriptor, bootstrap, args);
                    }
                };
            }
        }, 0);
        assertThrows(IllegalStateException.class, () -> transformed(writer.toByteArray()));
    }

    @Test public void originalPrivateFailureBodyIsNotRewritten() throws Exception {
        Class<?> type = transformed(original(PasskeyCapabilityVisitorFactory.DESCRIPTOR));
        Method originalFailure = type.getDeclaredMethod("failureForgetAssertiondefault", GeckoResult.class, Throwable.class);
        originalFailure.setAccessible(true);
        GeckoResult result = GeckoResult.fromValue(null);
        originalFailure.invoke(null, result, new IllegalStateException());
        assertEquals("UNKNOWN_ERR", result.error.getMessage());
    }

    private void verifyActivityFailure(byte[] original) throws Exception {
        Class<?> type = transformed(original);
        PasskeyCapabilities.googleAvailable = true;
        Method request = type.getDeclaredMethod("getAssertion", byte[].class,
                WebAuthnUtils.WebAuthnPublicCredential[].class, GeckoBundle.class, GeckoBundle.class);
        request.setAccessible(true);
        GeckoResult result = (GeckoResult) request.invoke(null, new Object[]{null, null, null, null});
        assertNull(result.error);
        com.google.android.gms.tasks.Task.fireSuccess();
        assertEquals("ABORT_ERR", result.error.getMessage());
    }

    private Class<?> transformed(byte[] original) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        new ClassReader(original).accept(PasskeyCapabilityVisitorFactory.patch(writer), 0);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() { return defineClass(PasskeyCapabilityVisitorFactory.TARGET, bytes, 0, bytes.length); }
        }.define();
    }

    private byte[] original(String descriptor) {
        return original(descriptor, "default", false);
    }

    private byte[] original(String descriptor, String suffix, boolean callbacksFirst) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, PasskeyCapabilityVisitorFactory.TARGET.replace('.', '/'),
                null, "java/lang/Object", null);
        if (callbacksFirst) callbacks(writer, suffix);
        for (var entry : PasskeyCapabilityVisitorFactory.METHODS.entrySet()) {
            String signature = entry.getKey().equals(PasskeyCapabilityVisitorFactory.METHOD) ? descriptor : entry.getValue();
            MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    entry.getKey(), signature, null, null);
            method.visitCode();
            method.visitFieldInsn(Opcodes.GETSTATIC, "org/mozilla/gecko/GeckoAppShell", "legacyCalls", "I");
            method.visitInsn(Opcodes.ICONST_1);
            method.visitInsn(Opcodes.IADD);
            method.visitFieldInsn(Opcodes.PUTSTATIC, "org/mozilla/gecko/GeckoAppShell", "legacyCalls", "I");
            method.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
            method.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/geckoview/GeckoResult", "fromValue",
                    "(Ljava/lang/Object;)Lorg/mozilla/geckoview/GeckoResult;", false);
            if (entry.getKey().equals("getAssertion") || entry.getKey().equals("makeCredential")) {
                int resultSlot = Type.getArgumentTypes(signature).length;
                method.visitVarInsn(Opcodes.ASTORE, resultSlot);
                method.visitTypeInsn(Opcodes.NEW, "com/google/android/gms/tasks/Task");
                method.visitInsn(Opcodes.DUP);
                method.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/google/android/gms/tasks/Task", "<init>", "()V", false);
                method.visitVarInsn(Opcodes.ALOAD, resultSlot);
                method.visitInvokeDynamicInsn("onSuccess", "(Lorg/mozilla/geckoview/GeckoResult;)Lcom/google/android/gms/tasks/OnSuccessListener;",
                        new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false),
                        Type.getType("(Ljava/lang/Object;)V"),
                        new Handle(Opcodes.H_INVOKESTATIC, PasskeyCapabilityVisitorFactory.TARGET.replace('.', '/'), "successFor" + entry.getKey() + suffix,
                                "(Lorg/mozilla/geckoview/GeckoResult;Ljava/lang/Object;)V", false), Type.getType("(Ljava/lang/Object;)V"));
                method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/google/android/gms/tasks/Task", "addOnSuccessListener",
                        "(Lcom/google/android/gms/tasks/OnSuccessListener;)Lcom/google/android/gms/tasks/Task;", false);
                method.visitInsn(Opcodes.POP);
                method.visitVarInsn(Opcodes.ALOAD, resultSlot);
            }
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        if (!callbacksFirst) callbacks(writer, suffix);
        MethodVisitor other = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "unrelated", "()I", null, null);
        other.visitCode();
        other.visitIntInsn(Opcodes.BIPUSH, 42);
        other.visitInsn(Opcodes.IRETURN);
        other.visitMaxs(0, 0);
        other.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private void callbacks(ClassWriter writer, String suffix) {
        String owner = PasskeyCapabilityVisitorFactory.TARGET.replace('.', '/');
        String result = "Lorg/mozilla/geckoview/GeckoResult;";
        for (String operation : new String[]{"makeCredential", "getAssertion"}) {
            MethodVisitor success = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "successFor" + operation + suffix, "(" + result + "Ljava/lang/Object;)V", null, null);
            success.visitCode();
            success.visitVarInsn(Opcodes.ALOAD, 0);
            success.visitInvokeDynamicInsn("accept", "(" + result + ")Lorg/mozilla/geckoview/GeckoResult$Consumer;",
                    new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false),
                    Type.getType("(Ljava/lang/Object;)V"),
                    new Handle(Opcodes.H_INVOKESTATIC, owner, "failureFor" + operation + suffix,
                            "(" + result + "Ljava/lang/Throwable;)V", false), Type.getType("(Ljava/lang/Throwable;)V"));
            success.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
            success.visitInsn(Opcodes.DUP);
            success.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false);
            success.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/mozilla/geckoview/GeckoResult$Consumer", "accept", "(Ljava/lang/Object;)V", true);
            success.visitInsn(Opcodes.RETURN);
            success.visitMaxs(0, 0); success.visitEnd();

            MethodVisitor failure = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "failureFor" + operation + suffix, "(" + result + "Ljava/lang/Throwable;)V", null, null);
            failure.visitCode();
            failure.visitVarInsn(Opcodes.ALOAD, 0);
            failure.visitTypeInsn(Opcodes.NEW, "org/mozilla/gecko/util/WebAuthnUtils$Exception");
            failure.visitInsn(Opcodes.DUP);
            failure.visitLdcInsn("UNKNOWN_ERR");
            failure.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/mozilla/gecko/util/WebAuthnUtils$Exception", "<init>", "(Ljava/lang/String;)V", false);
            failure.visitFieldInsn(Opcodes.PUTFIELD, "org/mozilla/geckoview/GeckoResult", "error", "Ljava/lang/Throwable;");
            failure.visitInsn(Opcodes.RETURN);
            failure.visitMaxs(0, 0); failure.visitEnd();
        }
    }
}
