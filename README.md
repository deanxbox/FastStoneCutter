# Fast Stone Cutter

A Minecraft Java Edition 26.2 Fabric client mod that adds a professional `Cut All Stone` button to the vanilla stonecutter screen.

![Fast Stone Cutter demo](FastStoneCutterDemo.gif)

## Behavior

1. Put a stonecutter-compatible material into the stonecutter input slot.
2. Select the desired output recipe in the vanilla recipe list.
3. Press `Cut All Stone`.

The mod sends normal vanilla stonecutter button and slot-click actions to convert all matching source stacks from your inventory into the selected output. It does not send custom packets or bypass server-side validation.

## Build

Minecraft 26.2 requires Java 25 for the Gradle JVM.

```powershell
.\gradlew.bat build
```

## Release

Bump `mod_version` in `gradle.properties`, commit the change on the branch to release, then push a matching version tag:

```sh
git tag vX.Y.Z && git push origin vX.Y.Z
```
