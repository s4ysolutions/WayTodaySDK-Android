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

### publish / deploy

Use when requested to publish, deploy, finalize, or release.

1. **Push** latest changes to `origin/main`
2. **Tag** with a fix version bump (e.g. `4.4.4` → `4.4.5`): `git tag 4.4.5 && git push origin 4.4.5`
3. **Trigger JitPack build** by fetching the artifact: open `https://jitpack.io/#s4ysolutions/WayTodaySDK-Android/<tag>` or `https://jitpack.io/com/github/s4ysolutions/WayTodaySDK-Android/<tag>/build.log`
4. **Monitor the build log** at `https://jitpack.io/com/github/s4ysolutions/WayTodaySDK-Android/<tag>/build.log`
5. If **build fails**, fix the issue and repeat from step 1 (push fix → delete old tag → tag new version → trigger → monitor)
6. If **build succeeds**, artifact is live at `com.github.s4ysolutions:WayTodaySDK-Android:<tag>`

> To delete a failed tag: `git tag -d <tag> && git push origin :refs/tags/<tag>`
