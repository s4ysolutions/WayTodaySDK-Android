# WayTodaySDK-Android — Agent Instructions

## Versions

- This SDK: `4.3.0` (waytoday-sdk/build.gradle `publishing.publications.Release.version`)
- Depends on WayTodaySDK-Java: `3.1.0-alpha1`
- Compatible Java SDK: [WayTodaySDK-Java](https://github.com/s4ysolutions/WayTodaySDK-Java) `3.1.0-alpha1`

## Compatibility rules

When bumping WayTodaySDK-Java dependency version:
1. Update `WayTodaySDK-Java` version in `waytoday-sdk/build.gradle`
2. Bump this SDK's minor version in `waytoday-sdk/build.gradle` publishing block
3. Update `README.md` dependency snippet to new version
4. Update the Java SDK README compatibility note (`## Android` section)

When bumping this SDK's version alone:
1. Update `version` in `waytoday-sdk/build.gradle` publishing block
2. Update `README.md` dependency snippet

## Build

```bash
./gradlew assembleRelease
```

## Git

Artifacts published via JitPack on tag push. Tag must match the publishing version exactly.
No CI — JitPack builds on first artifact request or via jitpack.io UI.
