package za.co.capitecbank.transaction_producer.constants;

public final class KafkaMessageConstants {

    private KafkaMessageConstants() {}

    public static final String HEADER_CHECKSUM        = "checksum";
    public static final String HEADER_SCHEMA_VERSION  = "schemaVersion";
    public static final String HEADER_SOURCE          = "source";

    public static final String SOURCE_TRANSACTION_PRODUCER = "transaction-producer";
}