package solutions.s4y.waytoday.sdk.tracker;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import mad.location.manager.lib.Commons.Coordinates;
import mad.location.manager.lib.Commons.GeoPoint;
import mad.location.manager.lib.Commons.Utils;
import mad.location.manager.lib.Filters.GPSAccKalmanFilter;
import solutions.s4y.waytoday.sdk.BuildConfig;
import solutions.s4y.waytoday.sdk.locations.ILocationUpdater;
import solutions.s4y.waytoday.sdk.locations.IRequestListener;

/**
 * The class to manage state of LocationUpdater, post-process the location updates
 * and notifiy the subscriber about location updater state change and about
 * location updates
 */
public class Tracker {
    private static final String LT = Tracker.class.getSimpleName();

    private final List<ILocationListener> locationListeners =
            new ArrayList<>(2);

    @VisibleForTesting
    private final List<ITrackingStateListener> stateListeners =
            new ArrayList<>(2);

    private GPSAccKalmanFilter mKalmanFilter;
    private final FilterSettings filterSettings = FilterSettings.defaultSettings;

    public static final DataItemAcc zero = new DataItemAcc(0, 0, 0);
    private DataItemGPS lastDataItemGPS = new DataItemGPS(null);

    private float minDistance = 1;

    double lastGPSTimeStamp = 0;
    double prevLat = 0;
    double prevLon = 0;
    private final LocationListener locationListener = new LocationListener() {
        private void handlePredict() {
            DataItemGPS gps = lastDataItemGPS;
            if (gps.location == null) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "kalmanFilter.predict (no location): east=" + Tracker.zero.absEastAcc + " north=" + Tracker.zero.absNorthAcc);
                }
                mKalmanFilter.predict(Tracker.zero.getTimestamp(), Tracker.zero.absEastAcc, Tracker.zero.absNorthAcc);
            } else {
                double declination = gps.getDeclination();
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "kalmanFilter.predict: (location): east=" + Tracker.zero.getAbsEastAcc(declination) + " north=" + Tracker.zero.getAbsNorthAcc(declination) + " decl=" + declination);
                }
                mKalmanFilter.predict(Tracker.zero.getTimestamp(), Tracker.zero.getAbsEastAcc(declination), Tracker.zero.getAbsNorthAcc(declination));
            }
        }

        private void handleUpdate(DataItemGPS gps, Location location) {
            double xVel = location.getSpeed() * Math.cos(location.getBearing());
            double yVel = location.getSpeed() * Math.sin(location.getBearing());

            if (BuildConfig.DEBUG) {
                if (gps.location != null) {
                    Log.d(LT, "kalmanFilter.update: lon=" + gps.location.getLongitude() + " lat=" + gps.location.getLatitude() + " xVel=" + xVel + " yVel=" + yVel);
                }
            }

            mKalmanFilter.update(
                    gps.getTimestamp(),
                    Coordinates.longitudeToMeters(location.getLongitude()),
                    Coordinates.latitudeToMeters(location.getLatitude()),
                    xVel,
                    yVel,
                    location.getAccuracy(),
                    gps.getVelErr()
            );
        }

        private Location locationAfterUpdateStep(Location location) {
            double xVel, yVel;
            Location loc = new Location("iTagAnroid");
            GeoPoint pp = Coordinates.metersToGeoPoint(mKalmanFilter.getCurrentX(),
                    mKalmanFilter.getCurrentY());
            loc.setLatitude(pp.Latitude);
            loc.setLongitude(pp.Longitude);
            loc.setAltitude(location.getAltitude());
            xVel = mKalmanFilter.getCurrentXVel();
            yVel = mKalmanFilter.getCurrentYVel();
            double speed = Math.sqrt(xVel * xVel + yVel * yVel); //scalar speed without bearing
            loc.setBearing(location.getBearing());
            loc.setSpeed((float) speed);
            loc.setTime(System.currentTimeMillis());
            loc.setElapsedRealtimeNanos(System.nanoTime());
            loc.setAccuracy(location.getAccuracy());

            if (BuildConfig.DEBUG) {
                Log.d(LT, "locationAfterUpdateStep: " + loc.getLongitude() + "," + loc.getLatitude());
            }

            return loc;
        }

        @Override
        public void onLocationChanged(@androidx.annotation.NonNull Location originalLocation) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onLocationChanged");
            }

            double lat = originalLocation.getLatitude();
            double lon = originalLocation.getLongitude();

            if (lat == 0 || lon == 0) {
                return;
            }

            lastDataItemGPS = new DataItemGPS(originalLocation);
            if (mKalmanFilter == null) {
                double x, y, xVel, yVel, posDev, course, speed;
                long timeStamp;
                speed = originalLocation.getSpeed();
                course = originalLocation.getBearing();
                x = originalLocation.getLongitude();
                y = originalLocation.getLatitude();
                xVel = speed * Math.cos(course);
                yVel = speed * Math.sin(course);
                posDev = originalLocation.getAccuracy();
                timeStamp = Utils.nano2milli(originalLocation.getElapsedRealtimeNanos());
                mKalmanFilter = new GPSAccKalmanFilter(
                        false, //todo move to settings
                        Coordinates.longitudeToMeters(x),
                        Coordinates.latitudeToMeters(y),
                        xVel,
                        yVel,
                        filterSettings.accelerationDeviation,
                        posDev,
                        timeStamp,
                        filterSettings.mVelFactor,
                        filterSettings.mPosFactor);
                handlePredict();
            }
            double ts = lastDataItemGPS.getTimestamp();
            if (ts < lastGPSTimeStamp) {
                return;
            }
            lastGPSTimeStamp = ts;

            handleUpdate(lastDataItemGPS, lastDataItemGPS.location);
            Location location = locationAfterUpdateStep(lastDataItemGPS.location);

            lat = location.getLatitude();
            lon = location.getLongitude();

            if (BuildConfig.DEBUG) {
                Log.d(LT, "Request to publish location " + lon + "," + lat);
            }

            if (Math.abs(lat - prevLat) < minDistance && Math.abs(lon - prevLon) < minDistance) {
                if (BuildConfig.DEBUG) {
                    Log.d(LT, "Skip upload because too close");
                }
                return;
            }

            prevLon = lon;
            prevLat = lat;
            notifyLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@androidx.annotation.NonNull String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderEnabled");
            }
            isSuspended = false;
            notifyStateChange();
        }

        @Override
        public void onProviderDisabled(@androidx.annotation.NonNull String provider) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onProviderDisabled");
            }
            isSuspended = true;
            notifyStateChange();
        }
    };

    private final IRequestListener requestListener = new IRequestListener() {
        @Override
        public void onRequestResult(boolean success) {
            if (BuildConfig.DEBUG) {
                Log.d(LT, "onRequestResult: " + success);
            }
            isSuspended = !success;
            notifyStateChange();
        }
    };

    private ILocationUpdater updater;

    public boolean isSuspended;
    public boolean isUpdating;

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void requestStart(@NonNull final ILocationUpdater updater, int frequency) {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "requestStart");
        }
        stop();
        this.updater = updater;
        isUpdating = true;
        isSuspended = false;
        minDistance = frequency < 5000 ? 0 : frequency < 15000 ? 0.0002f : 0.0005f;

        notifyStateChange();
        updater.requestLocationUpdates(locationListener, requestListener, frequency);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void stop() {
        if (BuildConfig.DEBUG) {
            Log.d(LT, "stop");
        }
        if (updater != null) {
            updater.cancelLocationUpdates();
            updater = null;
        }
        if (isUpdating) {
            isUpdating = false;
            notifyStateChange();
        }
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void resetFilter() {
        mKalmanFilter = null;
        double lastGPSTimeStamp = 0;
        double prevLat = 0;
        double prevLon = 0;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void addOnLocationListener(ILocationListener listener) {
        locationListeners.add(listener);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void removeOnLocationListener(ILocationListener listener) {
        locationListeners.remove(listener);
    }

    private void notifyLocation(@NonNull Location location) {
        for (ILocationListener listener : locationListeners) {
            listener.onLocation(location);
        }
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void addOnTrackingStateListener(ITrackingStateListener listener) {
        stateListeners.add(listener);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void removeOnTrackingStateListener(ITrackingStateListener listener) {
        stateListeners.remove(listener);
    }

    private void notifyStateChange() {
        TrackerState state = new TrackerState(isUpdating, isSuspended);
        for (ITrackingStateListener listener : stateListeners) {
            listener.onStateChange(state);
        }
    }
}
