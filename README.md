# LDIC: Limitless Development: Immersive Client

A client-only Minecraft Forge 1.12.2 mod for modpacks that want to control what players see first:

- **Loading screen**: a themeable replacement for Forge's startup splash, with progress, status
  text, images and animation, and optional notices such as a photosensitivity warning.
- **Main menu**: a fully customizable title screen (layout, buttons, actions, backgrounds,
  panoramas), with built-in modern themes as a starting point.

Status: early scaffold. See `docs/` for plans as they land.

## Building

```sh
JAVA_HOME=/path/to/jdk17 ./gradlew build
```

The jar is written to `build/libs/`.

## License

LGPL 2.1; see [LICENSE](LICENSE).
