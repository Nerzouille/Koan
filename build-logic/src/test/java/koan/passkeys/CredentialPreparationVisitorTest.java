package koan.passkeys;

import static org.junit.Assert.*;
import android.credentials.PrepareGetCredentialResponse;
import org.junit.Test;
import org.objectweb.asm.*;

public class CredentialPreparationVisitorTest {
    @Test public void remoteOnlyPreparationRemainsUsableWithoutChangingLocalOrUnlockCases() throws Exception {
        for (String name : new String[]{"GeneratedCallback17", "GeneratedCallback83"}) {
            Class<?> type = transformed(name, 1);
            Object receiver = type.getConstructor().newInstance();
            for (int bits = 0; bits < 8; bits++) {
                boolean local = (bits & 1) != 0, unlock = (bits & 2) != 0, remote = (bits & 4) != 0;
                type.getMethod("onResult", PrepareGetCredentialResponse.class)
                        .invoke(receiver, new PrepareGetCredentialResponse(local, unlock, remote));
                assertEquals("candidate combination " + bits, local || unlock || remote, type.getField("usable").getBoolean(null));
            }
        }
    }

    @Test public void changedPreparationCheckFailsTheBuild() {
        assertThrows(IllegalStateException.class, () -> transformed("NoCheck", 0));
        assertThrows(IllegalStateException.class, () -> transformed("AmbiguousCheck", 2));
    }

    private Class<?> transformed(String name, int checks) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "usable", "Z", null, null).visitEnd();
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN); init.visitMaxs(0, 0); init.visitEnd();
        MethodVisitor m = writer.visitMethod(Opcodes.ACC_PUBLIC, "onResult", CredentialPreparationVisitor.CALLBACK, null, null);
        m.visitCode();
        for (int i = 0; i < checks; i++) {
            if (i > 0) m.visitInsn(Opcodes.POP);
            m.visitVarInsn(Opcodes.ALOAD, 1); m.visitLdcInsn("public-key");
            m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CredentialPreparationVisitor.RESPONSE,
                    "hasCredentialResults", "(Ljava/lang/String;)Z", false);
        }
        if (checks == 0) m.visitInsn(Opcodes.ICONST_0);
        m.visitVarInsn(Opcodes.ALOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CredentialPreparationVisitor.RESPONSE,
                "hasAuthenticationResults", "()Z", false);
        m.visitInsn(Opcodes.IOR); m.visitFieldInsn(Opcodes.PUTSTATIC, name, "usable", "Z");
        m.visitInsn(Opcodes.RETURN); m.visitMaxs(0, 0); m.visitEnd(); writer.visitEnd();
        ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        new ClassReader(writer.toByteArray()).accept(CredentialPreparationVisitor.patch(output), 0);
        byte[] bytes = output.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() { return defineClass(name, bytes, 0, bytes.length); }
        }.define();
    }
}
