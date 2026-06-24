package za.co.capitecbank.transaction_event_consumer.velocity;

public record VelocityResult(
        long recentTxnCount10m,
        long recentTxnCount1h,
        long recentTxnCount24h,
        boolean degradedMode
) {
    public static VelocityResult zeros() {
        return new VelocityResult(0, 0, 0, false);
    }

    public static VelocityResult degraded(long count10m, long count1h, long count24h) {
        return new VelocityResult(count10m, count1h, count24h, true);
    }
}
