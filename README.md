# WayTodaySDK for Android

## Usage

Add the library to the list of dependencies
```
    implementation  "solutions.s4y.waytoday:waytoday-sdk:1.0.15"
```

and make sure you have the following repositories in you app build.gradle
```
    jcenter()
    mavenLocal()
```

Currently the 3rd party dependency `mad-location-manager:0.1.14` is not uploaded to a public repo and
you have to clone the fork https://github.com/s4ysolutions/mad-location-manager and issue
`./gradlew publishToMavenLocal`

## Services

WayToday SDK provides the services for requesting a personal track ID and upload the locations received
from Android services to WayToday server.

Besides the services there's a helper package to wrap Android GPS provider with the convenient
methods and and listeners

### IDJobService

IDJobService extends Android JobIntentService and adds methods to initiate request to generate
tracker id and to subscribe on the response(s).

```
 TrackIDJobService.init("password")
 ...
 TrackIDJobService.addOnTrackIDChangeListener(new ITrackIDChangeListener(){
       @Override
       public void onTrackID(@NonNull String trackID) {
           Log.d(LT, "onTrackID: " + trackID);
       }
 });
 ...
 TrackIDJobService.enqueueRetrieveId(this/*context*/);
 ...
 TrackIDJobService.removeOnTrackIDChangeListener(this/*context*/);
```

### UploadJobService

UploadJobService extends Android JobIntentService and adds methods to initiate request to generate
tracker id and to subscribe on the response(s).

```
 UploadJobService.init("secret","Some WT app");
 ...
 UploadJobService.enqueueUploadLocation(this/*context*/, location /*filtered or raw location*/);
```

## GPS Helpers
It is not necessary to utilize the classes below, one can use any location source available.

### Location updaters

There might be different updaters implementing `ILocationUpdater` interface, but currently
WayToday SDK has the only `LocationGPSUpdater` for Android GPS provider. In the future there might
be coarse GSM positioning added.

<!-- language: lang-or-tag-here -->
    LocationGPSUpdater --(implements)--> ILocationUpdater
        |
        +---> requestLocationUpdates
        |
        +---> cancelLocationUpdates
        |
        +---(notifies 1:1)------> android.location.LocationListener (raw coordinates)
        |
        +---(notifies 1:1)------> IRequestListener (success, fail)
        |
        +---(notifies 1:many) --> IPermissionListener


### Location tracker

The main purpose of `Tracker` class it to apply Kalman filter and to provide the unified
interface to different Location updaters.

<!-- language: lang-or-tag-here -->
    Tracker --(uses)--> ILocationUpdater
        |
        +---> start
        |
        +---> stop
        |
        +---> reset filter
        |
        +---> (notifies 1:many) --> ILocationListener (filtered coordinates)
        |
        +---> (notifies 1:many) --> ITrackingStateListener (isUpdating: true,false; isSuspended: true, false)
