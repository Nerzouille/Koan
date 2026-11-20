package koan.passkeys;

import static org.junit.Assert.*;
import android.credentials.PrepareGetCredentialResponse;
import dev.nerzouille.koan.engine.auth.CredentialUiHost;
import org.junit.Test;
import org.mozilla.geckoview.GeckoResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CredentialContextVisitorTest {
    @Test public void onlyUiOperationsUseTheActivityContext() throws Exception {
        Class<?> type = transformed(original(1));
        GeckoResult create = (GeckoResult) type.getMethod("makeCredential", String.class, byte[].class, String.class)
                .invoke(null, new Object[]{null, null, null});
        GeckoResult get = (GeckoResult) type.getMethod("getAssertion", PrepareGetCredentialResponse.PendingGetCredentialHandle.class)
                .invoke(null, new Object[]{null});
        GeckoResult prepare = (GeckoResult) type.getMethod("prepareGetAssertion").invoke(null);
        assertSame(CredentialUiHost.UI, create.value);
        assertSame(CredentialUiHost.UI, get.value);
        assertNotSame(CredentialUiHost.UI, prepare.value);
        assertTrue(create.normalized);
        assertTrue(get.normalized);
        assertFalse(prepare.normalized);
    }

    @Test public void changedContextSitesFailRatherThanPatchingUnknownCode() {
        assertThrows(IllegalStateException.class, () -> transformed(original(0)));
        assertThrows(IllegalStateException.class, () -> transformed(original(2)));
    }

    private Class<?> transformed(byte[] original) {
        ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        new ClassReader(original).accept(CredentialContextVisitor.patch(output), 0);
        byte[] bytes = output.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() { return defineClass(CredentialContextVisitor.TARGET, bytes, 0, bytes.length); }
        }.define();
    }

    private byte[] original(int reads) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, CredentialContextVisitor.TARGET.replace('.', '/'),
                null, "java/lang/Object", null);
        CredentialContextVisitor.METHODS.forEach((name, desc) -> method(writer, name, desc, reads));
        method(writer, "prepareGetAssertion", "()Lorg/mozilla/geckoview/GeckoResult;", 1);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private void method(ClassWriter writer, String name, String descriptor, int reads) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, descriptor, null, null);
        method.visitCode();
        for (int i = 0; i < reads; i++) {
            if (i > 0) method.visitInsn(Opcodes.POP);
            method.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/gecko/GeckoAppShell", "getApplicationContext",
                    "()Landroid/content/Context;", false);
        }
        if (reads == 0) method.visitInsn(Opcodes.ACONST_NULL);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mozilla/geckoview/GeckoResult", "fromValue",
                "(Ljava/lang/Object;)Lorg/mozilla/geckoview/GeckoResult;", false);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0); method.visitEnd();
    }
}
