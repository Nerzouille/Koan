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
import dev.nerzouille.koan.engine.auth.PasskeyCapabilities;

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
        assertEquals(Boolean.FALSE, ((GeckoResult) request.invoke(null, new Object[]{null, null, null, null})).value);
        assertEquals(1, GeckoAppShell.legacyCalls);
    }

    @Test public void changedEngineSignatureFailsRatherThanSilentlySkippingPatch() {
        assertThrows(IllegalStateException.class, () -> transformed(original("()Ljava/lang/Object;")));
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
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, PasskeyCapabilityVisitorFactory.TARGET.replace('.', '/'),
                null, "java/lang/Object", null);
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
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        MethodVisitor other = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "unrelated", "()I", null, null);
        other.visitCode();
        other.visitIntInsn(Opcodes.BIPUSH, 42);
        other.visitInsn(Opcodes.IRETURN);
        other.visitMaxs(0, 0);
        other.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
