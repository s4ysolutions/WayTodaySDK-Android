# WayTodaySDK for Android

## Usage

Add the library to the list of dependencies
[![Release](https://jitpack.io/v/s4ysolutions/WayTodaySDK-Android.svg)](https://jitpack.io/#s4ysolutions/WayTodaySDK-Android)

`AndroidWayTodayClient` wraps the  WayTodayClient from [WayTodaySDK-Java](https://github.com/s4ysolutions/WayTodaySDK-Java) and provides
 methods to create and enqueue Workers to request tracker ID and to send location updates.

 Create a new instance of `AndroidWayTodayClient`
```jave
       AndroidWayTodayClient androidWayToday = new AndroidWayTodayClient(context, "app id", "app secret", "User visible app id");
       androidWayToday.wtClient.addErrorsListener(wayTodayError -> {});
       androidWayToday.enqueueTrackIdWorkRequest(context)
```