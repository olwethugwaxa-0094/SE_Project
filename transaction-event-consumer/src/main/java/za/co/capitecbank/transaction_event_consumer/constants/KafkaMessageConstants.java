package za.co.capitecbank.transaction_event_consumer.constants;

public final class KafkaMessageConstants {

    private KafkaMessageConstants() {}

    public static final String HEADER_CHECKSUM       = "checksum";
    public static final String HEADER_SCHEMA_VERSION = "schemaVersion";
    public static final String HEADER_SOURCE         = "source";
}
