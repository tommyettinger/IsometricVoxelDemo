package gdx.liftoff;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class SVMRegistrationFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeJNIAccess.register(String.class);
        RuntimeJNIAccess.register(DoubleBuffer.class, IntBuffer.class, FloatBuffer.class, Buffer.class, LongBuffer.class,
            CharBuffer.class, ByteBuffer.class, ShortBuffer.class);
    }
}
