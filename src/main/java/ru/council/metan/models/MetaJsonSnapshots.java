package ru.council.metan.models;

import ru.council.metan.enums.JsonElementType;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a detached copy of a MetaJson tree. Parent links are reconstructed.
 *
 * <p>This method must not be called while another thread mutates the source.
 * The synchronized wrapper performs copies while holding its lock.
 * Unsupported mutable scalar values are rejected instead of silently aliased.
 */
public final class MetaJsonSnapshots {
    private MetaJsonSnapshots() { }

    public static MetaJson copy(MetaJson root) {
        Objects.requireNonNull(root, "root");
        Set<MetaJson> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        return copyValue(root, ancestors);
    }

    private static MetaJson copyValue(MetaJson source, Set<MetaJson> ancestors) {
        if (source == null) {
            return null;
        }
        if (!ancestors.add(source)) {
            throw new IllegalArgumentException("Cycle in MetaJson graph");
        }
        try {
            MetaJson target = new MetaJson(source.getType());
            if (source.isNode()) {
                for (Map.Entry<String, MetaJson> item : source.getNode().entrySet()) {
                    MetaJson child = copyValue(item.getValue(), ancestors);
                    if (child == null) {
                        target.getNode().put(item.getKey(), null);
                    } else {
                        target.put(item.getKey(), child);
                    }
                }
            } else if (source.isArray()) {
                for (MetaJson element : source.getArray()) {
                    MetaJson child = copyValue(element, ancestors);
                    if (child == null) {
                        target.getArray().add(null);
                    } else {
                        target.add(child);
                    }
                }
            } else if (source.isScalar() && source.getJson() != null) {
                Scalar<?> original = source.getScalar();
                Object valueCopy = scalarCopy(original.getValue());
                target.setJson(copyScalar(original.getType(), valueCopy));
            } else if (source.getType() != JsonElementType.Scalar) {
                throw new IllegalStateException("Unknown MetaJson element type");
            }
            return target;
        } finally {
            ancestors.remove(source);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Scalar<?> copyScalar(Class<?> type, Object value) {
        return new Scalar(type, value);
    }

    private static Object scalarCopy(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof BigInteger
                || value instanceof BigDecimal
                || value instanceof UUID
                || value instanceof Enum<?>
                || value instanceof java.time.Instant
                || value instanceof java.time.LocalDate
                || value instanceof java.time.LocalTime
                || value instanceof java.time.LocalDateTime
                || value instanceof java.time.OffsetTime
                || value instanceof java.time.OffsetDateTime
                || value instanceof java.time.ZonedDateTime
                || value instanceof java.time.Year
                || value instanceof java.time.YearMonth
                || value instanceof java.time.MonthDay
                || value instanceof java.time.Duration
                || value instanceof java.time.Period
                || value instanceof java.time.ZoneId
                || value instanceof java.time.format.DateTimeFormatter) {
            return value;
        }
        if (value instanceof Timestamp) {
            Timestamp original = (Timestamp) value;
            Timestamp result = new Timestamp(original.getTime());
            result.setNanos(original.getNanos());
            return result;
        }
        if (value instanceof java.sql.Date) {
            return new java.sql.Date(((java.sql.Date) value).getTime());
        }
        if (value instanceof java.sql.Time) {
            return new java.sql.Time(((java.sql.Time) value).getTime());
        }
        if (value instanceof Date) {
            return new Date(((Date) value).getTime());
        }
        Class<?> type = value.getClass();
        if (type.isArray() && type.getComponentType().isPrimitive()) {
            int length = Array.getLength(value);
            Object result = Array.newInstance(type.getComponentType(), length);
            System.arraycopy(value, 0, result, 0, length);
            return result;
        }
        throw new IllegalArgumentException(
                "Cannot safely snapshot mutable or unsupported scalar: " + type.getName());
    }
}
