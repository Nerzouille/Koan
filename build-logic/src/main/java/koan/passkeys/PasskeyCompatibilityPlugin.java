package koan.passkeys;

import com.android.build.api.instrumentation.FramesComputationMode;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import kotlin.Unit;

/** Isolated build plugin: does not replace the application's AGP/Kotlin classpath. */
public class PasskeyCompatibilityPlugin implements Plugin<Project> {
    @Override @SuppressWarnings({"rawtypes", "unchecked"})
    public void apply(Project project) {
        project.getPlugins().withId("com.android.application", ignored -> {
            String version = project.getExtensions().getByType(VersionCatalogsExtension.class)
                    .named("libs").findVersion("geckoview").orElseThrow().getRequiredVersion();
            if (!"157.0.20261005135250".equals(version)) throw new IllegalStateException(
                    "Review/remove the Credential Manager adapter before upgrading GeckoView");
            AndroidComponentsExtension components = project.getExtensions().getByType(AndroidComponentsExtension.class);
            components.onVariants(components.selector().all(), (Action<Variant>) variant -> {
                variant.getInstrumentation().transformClassesWith(
                        PasskeyCapabilityVisitorFactory.class, InstrumentationScope.ALL, parameters -> Unit.INSTANCE);
                variant.getInstrumentation().setAsmFramesComputationMode(
                        FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS);
            });
        });
    }
}
