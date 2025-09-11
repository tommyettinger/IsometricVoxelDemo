# VoxelExample

A sample [libGDX](https://libgdx.com/) project showing voxels using 2D art but 3D positions.

![Rotating the world](docs/rotation.gif)

[**Playable Here!**](https://tommyettinger.github.io/IsometricVoxelDemo/Aug_1_2025_build1/index.html)
----

## Controls

Your character is the one tiny person wearing blue; all enemies are green orcs.
You start blinking for a few seconds; while blinking you are invincible.
Ideally, use the numpad to move around, but you can also use the four keys `f`, `g`,
`t`, and `r` to move in those four diagonal directions, as if those four keys on a
QWERTY keyboard were arrows pointing in an X shape.
Spacebar, numpad 0, and numpad 5 let you jump. You can press the `[` and `]` keys
to rotate the world, letting you see things you normally wouldn't be able to.
You can press `z` to reset the game and make a new map.

Your goal is to pick up goldfish (by walking into them) without bumping into orcs
too many times. Pick up all 10 and you win!

... Yeah, there's no real gameplay. The orcs can't be harmed at all. This is meant
as a well-documented sample project to show how isometric pixel art can work. In
particular, I wanted to show how depth sorting isn't that complicated if you set
things up just right. I also am trying to publicize the usage of f/g/h axes in
isometric worlds instead of x/y/z... because x/y/z have no standard meanings in
this context. Some games use y for elevation and some use z; some have 0,0,0 at
the bottom center, others the top center or center left... It's a mess.

The f/g/h concept has a simple real-world analogy based on a map of Europe:
 - The f-axis is the diagonal from France to Finland (going northeast).
 - The g-axis is the diagonal from Germany to Greenland (going northwest).
 - The h-axis is the elevation line, from your heels to your head (going up).

As you can see in the GIF at the start of this README.md, the map can be rotated.
There's no compass currently, but the f and g axes still point toward the upper
right and upper left corners of the screen, respectively, when the world rotates.
The controls stay the same; it's as if the world was spun 90 degrees around a
central "carousel pole" every time you press `[` or `]`.

## Info

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
