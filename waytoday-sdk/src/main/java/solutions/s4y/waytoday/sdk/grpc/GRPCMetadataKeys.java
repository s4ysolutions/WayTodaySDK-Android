package solutions.s4y.waytoday.sdk.grpc;

import io.grpc.Metadata;

public class GRPCMetadataKeys {
    public static final Metadata.Key<String> wsseKey = Metadata.Key.of("wsse", Metadata.ASCII_STRING_MARSHALLER);
}
