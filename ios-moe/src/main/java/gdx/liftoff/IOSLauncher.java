package gdx.liftoff;

import apple.uikit.c.UIKit;
import com.badlogic.gdx.backends.iosmoe.IOSApplication;
import com.badlogic.gdx.backends.iosmoe.IOSApplicationConfiguration;
import org.moe.natj.general.Pointer;

/** Launches the iOS (Multi-Os Engine) application. */
public class IOSLauncher extends IOSApplication.Delegate {
    protected IOSLauncher(Pointer peer) {
        super(peer);
    }

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
        return new IOSApplication(new IsoEngine3D(), configuration);
    }

    public static void main(String[] argv) {
        UIKit.UIApplicationMain(0, null, null, IOSLauncher.class.getName());
    }
}
