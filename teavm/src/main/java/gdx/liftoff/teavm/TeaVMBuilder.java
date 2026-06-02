package gdx.liftoff.teavm;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.File;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;

/** Builds the TeaVM/HTML application. */
public class TeaVMBuilder {
    public static void main(String[] args) {
        // Typically set by the Gradle task, but can also be set here or with the command-line arg "debug"
        boolean debug = false;
        // Typically set by the Gradle task, but can also be set here or with the command-line arg "run"
        boolean startJetty = false;
        // If true, enables WASM output and allows a higher optimization level
        boolean wasm = false;
        for (String arg : args) {
            if ("debug".equals(arg)) debug = true;
            else if ("run".equals(arg)) startJetty = true;
        }
        new TeaCompiler(
            new WebBackend()
                .setHtmlWidth(900) /* Change this to fit your game's requirements. */
                .setHtmlHeight(650) /* Change this to fit your game's requirements. */
                .setHtmlTitle("Isometric Voxel Demo")
                .setWebAssembly(wasm) /* Uncomment this line to use WASM output instead of JavaScript output. */
                .setStartJettyAfterBuild(startJetty)
                .setJettyPort(8080)
        )
            .addAssets(new AssetFileHandle("../assets"))

            // Right now, if WASM is produced, we can use ADVANCED opts, but if JS is produced, we need SIMPLE opts.
            // Something breaks in scene2d's actions when ADVANCED opts are used with JS.
            .setOptimizationLevel(wasm ? TeaVMOptimizationLevel.ADVANCED : TeaVMOptimizationLevel.SIMPLE)
            .setMainClass(TeaVMLauncher.class.getName())
            .setObfuscated(!debug)
            .setDebugInformationGenerated(debug)
            .setSourceMapsFileGenerated(debug)
            .setSourceFilePolicy(TeaVMSourceFilePolicy.COPY)
            .addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")))
            // You can also register any classes or packages that require reflection here:
            //.addReflectionClass("com.github.tommyettinger.reflect")
//            .addReflectionClass("com.badlogic.gdx.scenes.scene2d.actions")
            .build(new File("build/dist"));
    }
}
