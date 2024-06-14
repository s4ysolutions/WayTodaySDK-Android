package solutions.s4y.waytoday.sdk;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import kotlin.Unit;
import s4y.gps.sdk.GPSUpdate;
import s4y.gps.sdk.GPSUpdatesManager;
import s4y.gps.sdk.android.GPSPowerManager;
import s4y.gps.sdk.android.GPSPreferences;
import s4y.gps.sdk.android.implementation.FusedGPSUpdatesProvider;
import s4y.gps.sdk.dependencies.IGPSUpdatesProvider;

/**
 * @noinspection unused
 */
public class AndroidWayTodayClient implements Closeable {
    public final WayTodayClient wtClient;
    public final GPSUpdatesManager gpsUpdatesManager;

    public boolean uploadLocationImmediately = true;
    public final GPSPowerManager powerManager;

    private final String provider;
    private final Context context;
    private final GPSPreferences gpsPreferences;

    private static final HashMap<String, WayTodayClient> clients = new HashMap<>();

    private final Data workerData = new Data.Builder()
            .putString("clientID", UUID.randomUUID().toString())
            .build();

    public AndroidWayTodayClient(Context context, String principal, String secret, boolean tls, String host, int port, String provider, Looper looper) {
        this.context = context;
        this.provider = provider;
        gpsPreferences = new GPSPreferences(context);

        powerManager = new GPSPowerManager(context);

        final GrpcClient grpcClient = new GrpcClient(principal, secret, tls, host, port, provider);
        wtClient = new WayTodayClient(new PreferencesPersistedState(context), grpcClient);
        String clientID = workerData.getString("clientID");
        clients.put(clientID, wtClient);

        IGPSUpdatesProvider gpsUpdatesProvider = new FusedGPSUpdatesProvider(context, looper);
        gpsUpdatesManager = new GPSUpdatesManager(gpsUpdatesProvider, 500);
        gpsUpdatesManager.getLast().addListener(this::gpsUpdatesListener);
        if (gpsPreferences.getKeepAlive()) {
            gpsUpdatesManager.start();
        }
    }

    public AndroidWayTodayClient(Context context, String principal, String secret, boolean tls, String host, int port, String provider) {
        this(context, principal, secret, tls, host, port, provider, Looper.getMainLooper());
    }

    public AndroidWayTodayClient(Context context, String principal, String secret, String provider) {
        this(context, principal, secret, true, "tracker.way.today", 9101, provider);
    }

    /** @noinspection RedundantThrows*/
    @Override
    public void close() throws IOException {
        gpsUpdatesManager.getLast().removeListener(this::gpsUpdatesListener);
        gpsUpdatesManager.close();
    }

    static public class RequestTrackIdWorker extends Worker {
        static final Object workerClientLock = new Object();
        static WayTodayClient workerClient;

        public RequestTrackIdWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            String clientID = getInputData().getString("clientID");
            WayTodayClient wtClient = clients.get(clientID);
            if (wtClient == null) {
                return Result.failure();
            }
            final String tid = wtClient.requestNewTrackerId(wtClient.getCurrentTrackerId());
            if (tid.isEmpty()) {
                return Result.failure();
            }
            final Data tidData = new Data.Builder()
                    .putString("id", tid)
                    .build();
            return Result.success(tidData);
        }
    }

    static public class UploadLocationsWorker extends Worker {
        public UploadLocationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            String clientID = getInputData().getString("clientID");
            WayTodayClient wtClient = clients.get(clientID);
            if (wtClient == null) {
                return Result.failure();
            }
            wtClient.uploadLocations();
            if (wtClient.getUploadingLocationsStatus() == UploadingLocationsStatus.ERROR) {
                return Result.failure();
            }
            return Result.success();
        }
    }

    public OneTimeWorkRequest createRequestTrackIdWorker() {
        return new OneTimeWorkRequest
                .Builder(RequestTrackIdWorker.class)
                .setInputData(workerData)
                .build();
    }

    /** @noinspection UnusedReturnValue*/
    public Operation enqueueTrackIdWorkRequest(Context context) {
        OneTimeWorkRequest request = createRequestTrackIdWorker();
        return WorkManager.getInstance(context).enqueue(request);
    }

    public OneTimeWorkRequest createUploadLocationsWorker() {
        return new OneTimeWorkRequest.Builder(UploadLocationsWorker.class)
                .setInputData(workerData)
                .build();
    }

    /** @noinspection UnusedReturnValue*/
    public Operation enqueueUploadLocationsWorkRequest(Context context) {
        OneTimeWorkRequest request = createUploadLocationsWorker();
        return WorkManager.getInstance(context).enqueue(request);
    }

    private Unit gpsUpdatesListener(GPSUpdate gpsUpdate) {
        String tid = wtClient.getCurrentTrackerId();
        if (tid.isEmpty()) {
            enqueueTrackIdWorkRequest(context);
        } else {
            wtClient.enqueueLocationToUpload(
                    new Location(
                            tid,
                            gpsUpdate.getLatitude(),
                            gpsUpdate.getLongitude(),
                            0.0,
                            (long) gpsUpdate.getBearing(),
                            gpsUpdate.getTs(),
                            0L, // TODO:
                            false, //TODO:
                            provider,
                            gpsUpdate.getVelocity(),
                            gpsUpdate.getAccuracy()
                    ));
            if (uploadLocationImmediately) {
                enqueueUploadLocationsWorkRequest(context);
            }
        }
        return Unit.INSTANCE;
    }

    public void enableTrackingOn() {
        gpsPreferences.setKeepAlive(true);
    }

    public void turnTrackingOn() {
        enableTrackingOn();
        gpsUpdatesManager.start();
    }

    public void turnTrackingOff() {
        gpsPreferences.setKeepAlive(false);
        gpsUpdatesManager.stop();
    }

    public boolean isTrackingOn() {
        return gpsPreferences.getKeepAlive();
    }

    static public boolean isTrackingOn(Context context) {
        return GPSPreferences.keepAlive(context);
    }
}
