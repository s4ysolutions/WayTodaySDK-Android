# WayToday SDK for Android

Android client library for the [way.today](https://github.com/s4ysolutions/way-today) real-time GPS tracking service. Wraps [WayTodaySDK-Java](https://github.com/s4ysolutions/WayTodaySDK-Java) with WorkManager-based background workers for tracker ID requests and location uploads, plus GPS management via [GPSAndroidSDK](https://github.com/s4ysolutions/GPSAndroidSDK).

## Requirements

- Android minSdk 19 (Android 4.4+)
- Java 8+

## Add Dependency

[![Release](https://jitpack.io/v/s4ysolutions/WayTodaySDK-Android.svg)](https://jitpack.io/#s4ysolutions/WayTodaySDK-Android)

> JitPack builds the artifact on first request. To pre-trigger the build for a new version, open
> `https://jitpack.io/#s4ysolutions/WayTodaySDK-Android` and click **Get it**.

Add JitPack repository and dependency to your `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.s4ysolutions:WayTodaySDK-Android:4.4.1'
}
```

## Quick Start

```java
// Create client (connects to tracker.way.today:9101 over TLS by default)
AndroidWayTodayClient client = new AndroidWayTodayClient(
    context,
    "your-principal",   // app identifier
    "your-secret",      // app secret
    "your-provider"     // location provider label
);

// Listen to tracker ID and error events
client.wtClient.addTrackIdChangeListener(id -> Log.d(TAG, "Tracker ID: " + id));
client.wtClient.addErrorsListener(err -> Log.e(TAG, err.getMessage()));

// Request a tracker ID (runs in WorkManager background worker)
client.enqueueTrackIdWorkRequest(context);

// Start GPS tracking
client.turnTrackingOn();

// Upload queued locations (runs in WorkManager background worker)
client.enqueueUploadLocationsWorkRequest(context);

// Stop tracking and release resources when done
client.turnTrackingOff();
client.close();
```

### Custom host / port

```java
AndroidWayTodayClient client = new AndroidWayTodayClient(
    context, principal, secret,
    /*tls*/ true, /*host*/ "tracker.way.today", /*port*/ 9101,
    provider
);
```

## API

### `AndroidWayTodayClient`

| Method | Description |
|---|---|
| `enqueueTrackIdWorkRequest(context)` | Enqueue a WorkManager one-time request to allocate a tracker ID |
| `createRequestTrackIdWorker()` | Build a `OneTimeWorkRequest` without enqueuing |
| `enqueueUploadLocationsWorkRequest(context)` | Enqueue a WorkManager one-time request to upload queued locations |
| `createUploadLocationsWorker()` | Build a `OneTimeWorkRequest` without enqueuing |
| `turnTrackingOn()` | Enable tracking preference and start GPS updates |
| `turnTrackingOff()` | Stop GPS updates |
| `enableTrackingOn()` | Set tracking preference only (does not start GPS) |
| `isTrackingOn()` | Return current tracking state |
| `isTrackingOn(context)` | Static utility — read tracking state from preferences |
| `close()` | Release GPS listener and updates manager |

### Public fields

| Field | Type | Description |
|---|---|---|
| `wtClient` | `WayTodayClient` | Core Java SDK client — add listeners, check status |
| `gpsUpdatesManager` | `GPSUpdatesManager` | Manage GPS update stream directly |
| `powerManager` | `GPSPowerManager` | GPS power management |
| `uploadLocationImmediately` | `boolean` | Upload on each GPS fix instead of batching |

## Architecture

```
AndroidWayTodayClient
├── WayTodayClient (WayTodaySDK-Java)   ← gRPC to way.today service
│   └── PreferencesPersistedState       ← tracker ID stored in SharedPreferences
├── GPSUpdatesManager (GPSAndroidSDK)   ← GPS fix stream
└── WorkManager workers
    ├── RequestTrackIdWorker
    └── UploadLocationsWorker
```

## Build

```bash
./gradlew assembleRelease
```

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
