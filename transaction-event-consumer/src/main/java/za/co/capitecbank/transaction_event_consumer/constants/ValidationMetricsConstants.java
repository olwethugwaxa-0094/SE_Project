package za.co.capitecbank.transaction_event_consumer.constants;

public final class ValidationMetricsConstants {

    private ValidationMetricsConstants() {}

    public static final String METRIC_VALIDATION        = "transaction.event.validation";

    public static final String TAG_RESULT               = "result";
    public static final String TAG_RESULT_VALID         = "valid";
    public static final String TAG_RESULT_INVALID       = "invalid";

    public static final String TAG_CATEGORY             = "category";
}
