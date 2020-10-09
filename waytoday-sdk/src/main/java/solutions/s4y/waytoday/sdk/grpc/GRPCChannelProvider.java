package solutions.s4y.waytoday.sdk.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import solutions.s4y.waytoday.sdk.BuildConfig;

public class GRPCChannelProvider {
    private final boolean tls;
    private final String host;
    private final int port;

    private static GRPCChannelProvider sInstance;

    public ManagedChannel channel() {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
                .forAddress(host, port);
        if (!tls)
            channelBuilder.usePlaintext();
        return channelBuilder.build();
    }

    private GRPCChannelProvider(String host, int port) {
        this.host = host;
        this.port = port;
        tls = (port % 1000) > 100;
    }

    public static GRPCChannelProvider getInstance() {
        if (sInstance == null) {
            sInstance = new GRPCChannelProvider(BuildConfig.GRPC_HOST, BuildConfig.GRPC_PORT);
        }
        return sInstance;
    }
}
