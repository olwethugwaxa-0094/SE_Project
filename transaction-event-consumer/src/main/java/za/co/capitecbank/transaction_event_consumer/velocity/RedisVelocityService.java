package za.co.capitecbank.transaction_event_consumer.velocity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_event_consumer.service.RulesEngineMetrics;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "velocity.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisVelocityService implements VelocityService {

    private static final String KEY_PREFIX = "rules:velocity:";
    // Lua script: add member + prune window + count — atomic single round-trip
    private static final String LUA_ZADD_COUNT = """
            local key    = KEYS[1]
            local member = ARGV[1]
            local now    = tonumber(ARGV[2])
            local cutoff = tonumber(ARGV[3])
            local ttl    = tonumber(ARGV[4])
            redis.call('ZADD', key, now, member)
            redis.call('ZREMRANGEBYSCORE', key, '-inf', cutoff)
            redis.call('EXPIRE', key, ttl)
            return redis.call('ZCOUNT', key, cutoff, '+inf')
            """;

    private final StringRedisTemplate redis;
    private final JdbcTemplate fallbackJdbc;
    private final RulesEngineMetrics metrics;

    public RedisVelocityService(StringRedisTemplate redis,
                                @Qualifier("velocityFallbackJdbc") JdbcTemplate fallbackJdbc,
                                RulesEngineMetrics metrics) {
        this.redis        = redis;
        this.fallbackJdbc = fallbackJdbc;
        this.metrics      = metrics;
    }

    @Override
    @CircuitBreaker(name = "velocity", fallbackMethod = "fallback")
    public VelocityResult fetchAndRecord(String cifNr, LocalDateTime transactionDate, String transactionId) {
        var sample = metrics.startVelocityQuery();
        try {
            long nowEpochMs   = toEpochMs(transactionDate);
            String member     = transactionId + ":" + nowEpochMs;
            String key        = KEY_PREFIX + cifNr;

            long count10m  = runScript(key, member, nowEpochMs, windowCutoff(nowEpochMs, 10),  86400);
            long count1h   = runScript(key, member, nowEpochMs, windowCutoff(nowEpochMs, 60),  86400);
            long count24h  = runScript(key, member, nowEpochMs, windowCutoff(nowEpochMs, 1440), 86400);

            return new VelocityResult(count10m, count1h, count24h, false);
        } finally {
            metrics.stopVelocityQuery(sample);
        }
    }

    // Called by Resilience4j when circuit is open or Redis throws
    @SuppressWarnings("unused")
    public VelocityResult fallback(String cifNr, LocalDateTime transactionDate, String transactionId, Throwable t) {
        log.warn("Velocity Redis circuit open or error for cifNr={} — falling back to DW query: {}", cifNr, t.getMessage());
        metrics.recordVelocityFallback();
        return fallbackQuery(cifNr, transactionDate);
    }

    private VelocityResult fallbackQuery(String cifNr, LocalDateTime transactionDate) {
        var sample = metrics.startVelocityQuery();
        try {
            long c10m  = countDw(cifNr, transactionDate, 10);
            long c1h   = countDw(cifNr, transactionDate, 60);
            long c24h  = countDw(cifNr, transactionDate, 1440);
            return VelocityResult.degraded(c10m, c1h, c24h);
        } finally {
            metrics.stopVelocityQuery(sample);
        }
    }

    private long countDw(String cifNr, LocalDateTime now, int minutesBack) {
        String sql = """
                SELECT COUNT(*) FROM rules_engine.fact_transaction
                WHERE cif_nr = ?
                  AND transaction_date >= ?
                  AND transaction_date <= ?
                """;
        Long count = fallbackJdbc.queryForObject(
                sql,
                Long.class,
                Long.parseLong(cifNr),
                now.minusMinutes(minutesBack),
                now);
        return count != null ? count : 0L;
    }

    private long runScript(String key, String member, long nowMs, long cutoffMs, int ttlSeconds) {
        Object result = redis.execute(
                connection -> connection.eval(
                        LUA_ZADD_COUNT.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        key.getBytes(),
                        member.getBytes(),
                        String.valueOf(nowMs).getBytes(),
                        String.valueOf(cutoffMs).getBytes(),
                        String.valueOf(ttlSeconds).getBytes()),
                true);
        return result instanceof Long l ? l : 0L;
    }

    private long toEpochMs(LocalDateTime dt) {
        return dt != null ? dt.toInstant(ZoneOffset.UTC).toEpochMilli() : System.currentTimeMillis();
    }

    private long windowCutoff(long nowMs, int minutesBack) {
        return nowMs - (long) minutesBack * 60 * 1000;
    }
}
