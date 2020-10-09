package solutions.s4y.waytoday.sdk.locations;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import solutions.s4y.waytoday.sdk.errors.ErrorsObservable;
import solutions.s4y.waytoday.sdk.BuildConfig;
import solutions.s4y.waytoday.sdk.R;
import solutions.s4y.waytoday.sdk.tracker.Tracker;

/**
 * A helper class to send request to start GPS updates and notify about request
 * completed
 */


@SuppressWarnings({"unused", "RedundantSuppression"})
public class LocationGPSUpdater implements ILocationUpdater {
    private static final String LT = Tracker.class.getSimpleName();
    private final List<IPermissionListener> permissionListeners =
            new ArrayList<>(2);

    private final LocationManager locationManager;
    private LocationListener locationListener;

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public LocationGPSUpdater(@NonNull Context context) {
        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void requestLocationUpdates(
            @NonNull LocationListener locationListener,
            @NonNull IRequestListener requestLocationsUpdatesListener,
            int frequency
    ) {
        if (locationManager == null) {
            ErrorsObservable.toast(R.string.no_location_manager);
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.d(LT, "requestLocationUpdates " + frequency + " ms");
        }
        if (this.locationListener != null) {
            cancelLocationUpdates();
        }
        this.locationListener = locationListener;
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    frequency,
                    1,
                    this.locationListener
            );
            requestLocationsUpdatesListener.onRequestResult(true);
        } catch (IllegalArgumentException e) {
            requestLocationsUpdatesListener.onRequestResult(false);
            ErrorsObservable.toast(e);
        } catch (SecurityException e) {
            requestLocationsUpdatesListener.onRequestResult(false);
            ErrorsObservable.notify(e, true);
            notifyPermissionRequest();
        }
    }

    @Override
    public void cancelLocationUpdates() {
        if (locationManager == null) {
            ErrorsObservable.toast(R.string.no_location_manager);
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    public void addOnPermissionListener(IPermissionListener listener) {
        permissionListeners.add(listener);
    }

    public void removePermissionListener(IPermissionListener listener) {
        permissionListeners.remove(listener);
    }

    private void notifyPermissionRequest() {
        for (IPermissionListener listener : permissionListeners) {
            listener.onPermissionRequest();
        }
    }
}
