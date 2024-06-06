package solutions.s4y.waytoday.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** @noinspection unused*/
public class AndroidWayTodayClient {
    public final WayTodayClient client;

    public AndroidWayTodayClient(Context context, String principal, String secret, boolean tls, String host, int port, String provider) {
        final GrpcClient grpcClient = new GrpcClient(principal, secret, tls, host, port, provider);
        client = new WayTodayClient(new PreferencesPersistedState(context), grpcClient);
    }

    public AndroidWayTodayClient(Context context, String principal, String secret, String provider) {
        this(context, principal, secret, true, "tracker.way.today", 9001, provider);
    }

    public class RequestTrackIdWorker extends Worker {
        public RequestTrackIdWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            final String tid = client.requestNewTrackerId(client.getCurrentTrackerId());
            if (tid == null) {
                return Result.failure();
            }
            final Data tidData = new Data.Builder()
                    .putString("id", tid)
                    .build();
            return Result.success(tidData);
        }
    }

    public class UploadLocationsWorker extends Worker {
        public UploadLocationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            client.uploadLocations();
            if (client.getUploadingLocationsStatus() == UploadingLocationsStatus.ERROR) {
                return Result.failure();
            }
            return Result.success();
        }
    }

    public OneTimeWorkRequest createRequestTrackIdWorker() {
        return new OneTimeWorkRequest.Builder(RequestTrackIdWorker.class).build();
    }

    public OneTimeWorkRequest createUploadLocationsWorker() {
        return new OneTimeWorkRequest.Builder(UploadLocationsWorker.class).build();
    }

}
