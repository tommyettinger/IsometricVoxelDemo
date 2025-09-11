# VoxelExample

![Rotating the world](docs/rotation.gif)

A sample [libGDX](https://libgdx.com/) project showing voxels using 2D art but 3D positions.

Based on [voxelia by bergice](https://gitlab.com/bergice/voxelia/-/tree/master).
License is [MIT](LICENSE).

Uses [art by Gustavo Vituri](https://gvituri.itch.io/isometric-trpg).
License says ['You are free to use all sprites in both personal and commercial projects.'](assets/isometric-trpg-License.txt),
and to credit Gustavo Vituri. Don't use the sprites in any NFT; it isn't permitted.

Contains a [scene2d.ui skin by Raymond Buckley](https://ray3k.wordpress.com/clean-crispy-ui-skin-for-libgdx/).
License is [CC-BY 4.0](assets/Skin-License.txt).

Contains a [song by Komiku, 'Road 4 Fight'](https://freemusicarchive.org/music/Komiku/Helice_Awesome_Dance_Adventure_/road-4-fight/)
License is [CC0](assets/Komiku%20-%20Road%204%20Fight%20-%20License.txt).

Contains the font [Cozette, by ines](https://github.com/the-moonwitch/Cozette).
It is [MIT-licensed](assets/Cozette-License.txt).

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `android`: Android mobile platform. Needs Android SDK.
- `teavm`: Web platform using TeaVM and WebGL.
- `ios-moe`: iOS mobile backend using Multi-OS Engine.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `android:lint`: performs Android project validation.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `teavm:build`: builds the JavaScript application into the build/dist/webapp folder.
- `teavm:run`: serves the JavaScript application at http://localhost:8080 via a local Jetty server.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
