# Building from source

LDIC uses the GregTechCEu buildscript on RetroFuturaGradle. It needs a Java 17 (or newer) JDK
to run Gradle; the mod itself targets Java 8.

```sh
git clone https://github.com/Mica-Technologies/LDIC.git
cd LDIC
./gradlew build        # jar in build/libs/
./gradlew test         # unit tests only
./gradlew runClient    # dev client; logs in run/logs/latest.log
```

Do not run a build while `runClient` is open: it replaces classes under the running game.

## The wiki

The wiki is the `docs/` folder, built with MkDocs Material and published to GitHub Pages on every
push to `main` that changes it. To preview it locally:

```sh
python -m pip install -r docs/requirements.txt
mkdocs serve
```

`mkdocs build --strict` must pass: CI uses it, so a broken link fails the deploy.

## Releases

Every push to `main` publishes a dated pre-release. A full release is a manual run of the
release workflow. See `.github/workflows/`.
