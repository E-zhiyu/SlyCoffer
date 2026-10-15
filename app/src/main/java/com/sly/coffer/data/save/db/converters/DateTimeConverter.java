package com.sly.coffer.data.save.db.converters;

import androidx.room.TypeConverter;

import org.jetbrains.annotations.Contract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateTimeConverter {
    /**
     * 将{@link LocalDateTime}类型转换为{@link Long}时间戳类型
     *
     * @param dateTime 需要转换的{@link LocalDateTime}对象
     * @return UTC时区的时间戳
     */
    @TypeConverter
    @Contract("null -> null")
    public static Long fromLocalDateTime(LocalDateTime dateTime) {
        // 将 LocalDateTime 转为秒级或毫秒级时间戳
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * 将UTC时间戳转换为{@link LocalDateTime}类型
     *
     * @param timestamp UTC时间戳
     * @return 转换后的{@link LocalDateTime}对象
     */
    @TypeConverter
    @Contract("null -> null")
    public static LocalDateTime toLocalDateTime(Long timestamp) {
        return timestamp == null ? null :
                LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    /**
     * 将{@link LocalDate}类型转换为时间戳
     *
     * @param date 需要转换为时间戳{@link LocalDate}对象
     * @return 转换后的UTC时间戳
     */
    @TypeConverter
    @Contract("null -> null")
    public static Long fromLocalDate(LocalDate date) {
        // 将 LocalDate 转为该日凌晨的时间戳
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * 将UTC时间戳转换为{@link LocalDate}类型
     *
     * @param timestamp 需要转换的UTC时间戳
     * @return 转换后的{@link LocalDate}对象
     */
    @TypeConverter
    @Contract("null -> null")
    public static LocalDate toLocalDate(Long timestamp) {
        return timestamp == null ? null :
                Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
