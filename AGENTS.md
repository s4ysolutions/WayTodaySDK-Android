# WayTodaySDK-Android — Agent Instructions

## Versions

- This SDK: `4.4.4` (defined by git tag, matches JitPack artifact version)
- AGP: `9.2.1` | Gradle: `9.6.0` | compileSdk: `37`
- Depends on GPSAndroidSDK: `3.3.3`
- Depends on WayTodaySDK-Java: `3.1.0-alpha1`
- Compatible Java SDK: [WayTodaySDK-Java](https://github.com/s4ysolutions/WayTodaySDK-Java) `3.1.0-alpha1`

## Build

```bash
./gradlew assembleRelease
```

## Git

Artifacts published via JitPack on tag push. Tag must match the publishing version exactly.
No CI — JitPack builds on first artifact request or via jitpack.io UI.

**Note:** "Publishing" means creating a git tag matching the version (e.g., `4.4.4`) and pushing it. The artifact is then fetched from JitPack, which triggers the build. Build success/failure can be tracked via JitPack logs at `https://jitpack.io/com/github/s4ysolutions/WayTodaySDK-Android/<tag>/build.log`.
Artifact coordinates: `com.github.s4ysolutions:WayTodaySDK-Android:<version>` (tag with no `v` prefix).

## Compatibility rules

When bumping WayTodaySDK-Java dependency version:
1. Update `WayTodaySDK-Java` version in `waytoday-sdk/build.gradle`
2. Bump this SDK's minor version in `waytoday-sdk/build.gradle`
3. Update `README.md` dependency snippet to new version
4. Update the Java SDK README compatibility note (`## Android` section)

When bumping this SDK's version alone:
1. Update `README.md` dependency snippet

## Workflows

### deploy to Jitpack

Use when requested to deploy or finalize or publish.

Steps:
1. Update `api 'com.github.s4ysolutions:GPSAndroidSDK:<version>'` in `waytoday-sdk/build.gradle`
2. Update `AGENTS.md` — both the version table and the Depends on line
3. Bump this SDK's fix version in `waytoday-sdk/build.gradle` (e.g. `4.4.1` → `4.4.2`)
4. Commit with message `chore: bump GPSAndroidSDK to <version>, SDK to <new-version>`
5. Create matching tag (e.g. `4.4.2`)
6. Push branch and tag to origin
7. Check Jitpack build at `https://jitpack.io/com/github/s4ysolutions/WayTodaySDK-Android/<tag>/build.log`
8. If JDK version mismatch, add/edit `jitpack.yml` with `openjdk21`
