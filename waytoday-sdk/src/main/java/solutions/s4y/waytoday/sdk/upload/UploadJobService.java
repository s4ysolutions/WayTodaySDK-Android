package solutions.s4y.waytoday.sdk.upload;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.JobIntentService;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import solutions.s4y.waytoday.sdk.errors.ErrorsObservable;
import solutions.s4y.waytoday.sdk.grpc.GRPCChannelProvider;
import solutions.s4y.waytoday.sdk.grpc.GRPCMetadataKeys;
import solutions.s4y.waytoday.sdk.grpc.LocationOuterClass;
import solutions.s4y.waytoday.sdk.grpc.TrackerGrpc;
import solutions.s4y.waytoday.sdk.grpc.TrackerOuterClass;
import solutions.s4y.waytoday.sdk.BuildConfig;
import solutions.s4y.waytoday.sdk.utils.Bear;
import solutions.s4y.waytoday.sdk.wsse.Wsse;

import static solutions.s4y.waytoday.sdk.utils.FConv.i;
import static java.util.UUID.randomUUID;

/**
 * Ready for use JobIntentService to upload the locations to the WayToday server
 */
public class UploadJobService extends JobIntentService {
    private static String secret;
    private static String provider;

    private static Boolean sIsUploading = false;
    private static Boolean sIsError = false;
    private static final Deque<Location> uploadQueue = new LinkedList<>();
    private final static int MAX_LOCATIONS_MEMORY = 500;
    private final static int PACK_SIZE = 16;

    private final GRPCChannelProvider grpcChannelProvider = GRPCChannelProvider.getInstance();

    private static boolean sPrevIsError;
    private static boolean sPrevIsUploading;
    private static int sPrevSize;
    private static UploadStatus sPrevUploadStatus;
    @VisibleForTesting
    public static final List<IUploadStatusChangeListener> sListeners =
            new ArrayList<>(2);
    protected ManagedChannel ch = null;

    /**
     * Statuses the uploader can be in
     * Intended to be used to show the status of the uploader in UI
     */
    public enum UploadStatus {EMPTY, QUEUED, UPLOADING, ERROR}

    public interface IUploadStatusChangeListener {
        void onStatusChange(UploadStatus uploadStatus);
    }

    /**
     * Must be called before the very first request to upload locations
     *
     * @param secret mandatory string to athorize the application against WayToday server.
     *               Currently it might be any characters string.
     * @param provider optional string to be passed with every location up to the server and
     *                 down to clients in order to identify the locations sent by the application.
     *                 Keep it as shot as possible.
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void init(@NonNull String secret, String provider) {
       UploadJobService.secret = secret;
       UploadJobService.provider = provider;
    }

    /**
     * @return current status of the uploader
     */
    public static UploadStatus uploadStatus() {
        if (sIsError)
            return UploadStatus.ERROR;
        if (sIsUploading)
            return UploadStatus.UPLOADING;
        int size;
        synchronized (uploadQueue) {
            size = uploadQueue.size();
        }
        if (size > 0)
            return UploadStatus.QUEUED;
        return UploadStatus.EMPTY;
    }

    /**
     * Subscribe the listener to changes of the uploader status
     * @param listener an implementation of IUploadStatusChangeListener
     */

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void addUploadStatusChangeListener(IUploadStatusChangeListener listener) {
        sListeners.add(listener);
    }


    /**
     * Unsubscribe the listener from changes of the uploader status
     * @param listener an implementation of IUploadStatusChangeListener
     *                 used in the previous call of addUploadStatusChangeListener
     */

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void removeUploadStatusListener(IUploadStatusChangeListener listener) {
        sListeners.remove(listener);
    }

    /**
     * Adds the location to the queue of the locations to be uploaded to WayToday server
     * @param context a context to be used to create an intent used to enqueue work unit
     *                which will decide either it is good time to upload the locations to
     *                the WayToday server
     * @param location - a GPS location too be added to the queue
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void enqueueUploadLocation(Context context, Location location) {
        synchronized (uploadQueue) {
            uploadQueue.add(location);
        }
        notifyUpdateState();
        enqueueUploadLocations(context);
    }

    private static void enqueueUploadLocations(Context context) {
        Intent intent = new Intent(context, UploadJobService.class);
        enqueueWork(context, UploadJobService.class, 1000, intent);
    }

    private static void notifyUploadStatus() {
        UploadStatus uploadStatus = uploadStatus();
        for (IUploadStatusChangeListener listener : sListeners) {
            listener.onStatusChange(uploadStatus);
        }
    }

    private static void notifyUpdateState() {
        if (BuildConfig.DEBUG) {
            boolean changed = false;
            if (sIsError != sPrevIsError) {
                sPrevIsError = sIsError;
                changed = true;
            }
            if (sIsUploading != sPrevIsUploading) {
                sPrevIsUploading = sIsUploading;
                changed = true;
            }
            int size = uploadQueue.size();
            if (size != sPrevSize) {
                sPrevSize = size;
                changed = true;
            }
            if (changed) {
                UploadStatus uploadStatus = uploadStatus();
                if (sPrevUploadStatus == uploadStatus) {
                    ErrorsObservable.notify(new Exception("Status must not be the same"), true);
                }
                notifyUploadStatus();
                sPrevUploadStatus = uploadStatus;
            } else {
                ErrorsObservable.notify(new Exception("Should never be called without changes"), true);
            }
        } else {
            notifyUploadStatus();
        }
    }

    @VisibleForTesting()
    public void destroyGrpcChannel() {
        if (ch != null) {
            try {
                ch.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ch = null;
        }
    }

    @Override
    public void onDestroy() {
        destroyGrpcChannel();
        super.onDestroy();
    }

    private void uploadStore() {
        // TODO: do not have store yet
    }

    private void saveQueueToStore() {
        /*
        TODO: while there's no persist store just remove
        the oldest locations from the queue
        */
        synchronized (uploadQueue) {
            while (uploadQueue.size() > MAX_LOCATIONS_MEMORY) {
                uploadQueue.pollFirst();
            }
        }
    }

    private boolean uploadQueue(@NonNull final String tid) {
        List<Location> pack = new ArrayList<>();
        boolean completed = false;

        for (; ; ) {
            pack.clear();
            int packSize;
            synchronized (uploadQueue) {
                packSize = Math.min(uploadQueue.size(), PACK_SIZE);
                for (int i = 0; i < packSize; i++) {
                    Location head = uploadQueue.peekFirst();
                    if (head != null) {
                        pack.add(head);
                    }
                }
            }
            if (pack.size() > 0) {
                TrackerOuterClass.AddLocationsRequest.Builder req =
                        TrackerOuterClass.AddLocationsRequest
                                .newBuilder();
                req.setTid(tid);
                for (Location location : pack) {
                    req.addLocations(marshall(tid, location));
                }
                if (ch == null) ch = grpcChannelProvider.channel();
                TrackerGrpc.TrackerBlockingStub grpcStub = getGrpcStub();
                try {
                    Metadata headers = new Metadata();
                    headers.put(GRPCMetadataKeys.wsseKey, Wsse.getToken(secret));

                    grpcStub = MetadataUtils.attachHeaders(grpcStub, headers);
                    TrackerOuterClass.AddLocationResponse resp = grpcStub.addLocations(req.build());
                    if (resp.getOk()) {
                        for (int i = 0; i < packSize; i++) {
                            synchronized (uploadQueue) {
                                uploadQueue.pollFirst();
                            }
                        }
                    } else {
                        ErrorsObservable.notify(new Exception("upload_failed"), false);
                        break;
                    }
                } catch (Exception e) {
                    ErrorsObservable.notify(e, true);
                    break;
                }
            }
            synchronized (uploadQueue) {
                if (uploadQueue.size() == 0) {
                    completed = true;
                    break;
                }
            }
        }
        return completed;
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    private LocationOuterClass.Location marshall(String tid, Location location) {
        Intent batteryIntent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status;
        int level;
        int scale;
        if (batteryIntent != null) {
            status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        } else {
            level = -1;
            scale = -1;
            status = 0;
        }

        return LocationOuterClass.Location.newBuilder()
                .setAcc(i(location.getAccuracy()))
                .setAlt(i(location.getAltitude()))
                .setBatp((level == -1 || scale == -1) ? 50 : Math.round((float) level * 100.0f / (float) scale))
                .setBats(status != 0)
                .setBear(location.hasBearing() ? i(location.getBearing()) : Bear.EMPTY_BEAR)
                .setLat(i(location.getLatitude()))
                .setLon(i(location.getLongitude()))
                .setProvider(provider == null ? location.getProvider() : provider)
                .setSid(randomUUID().toString())
                .setSpeed(i(location.getSpeed()))
                .setTid(tid)
                .setTs(System.currentTimeMillis() / 1000)
                .build();
    }

    @VisibleForTesting()
    public TrackerGrpc.TrackerBlockingStub getGrpcStub() {
        return TrackerGrpc.newBlockingStub(ch);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String tid = preferences.getString("tid", "");

        if ("".equals(tid)) return;

        if (sIsUploading) {
            ErrorsObservable.notify(new Error("UploadJobService re-entry"), BuildConfig.DEBUG);
        }
        sIsUploading = true;
        if (uploadQueue.size() > 0) {
            sIsError = false;

            notifyUpdateState();

            boolean completed = false;
            if (isConnected()) {
                uploadStore();
                completed = uploadQueue(tid);
            }
            if (!completed) {
                if (uploadQueue.size() > MAX_LOCATIONS_MEMORY) {
                    saveQueueToStore();
                }
                destroyGrpcChannel();
                sIsError = true;
            }
            sIsUploading = false;
            notifyUpdateState();
        } else {
            sIsUploading = false;
        }
    }


}
