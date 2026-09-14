package com.sly.coffer.helpers.time;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sly.coffer.auxiliary.enums.unique.TagStrings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateTimePickerHelper {
    public interface OnTimePickerPositiveBtnClickedListener {
        /**
         * 时间选择对话框确认按钮的点击回调
         *
         * @param timePicker 时间选择对话框
         */
        void onClicked(MaterialTimePicker timePicker);
    }

    /**
     * 选择日期范围
     *
     * @param start           初始化时的起始日期
     * @param end             初始化时的结束日期
     * @param fragmentManager 显示对话框所需Fragment管理器
     * @param context         上下文
     * @param listener        确认按钮点击回调
     */
    public static void selectDateRange(
            LocalDate start,
            LocalDate end,
            FragmentManager fragmentManager,
            Context context,
            MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> listener
    ) {
        selectDateRange(start, end, fragmentManager, "选择日期范围", context, listener);
    }

    /**
     * 选择日期范围
     *
     * @param start           初始化时的起始日期
     * @param end             初始化时的结束日期
     * @param fragmentManager 显示对话框所需Fragment管理器
     * @param title           对话框标题
     * @param context         上下文
     * @param listener        确认按钮点击回调
     */
    public static void selectDateRange(
            LocalDate start,
            LocalDate end,
            FragmentManager fragmentManager,
            String title,
            Context context,
            MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>> listener
    ) {
        MaterialDatePicker.Builder<Pair<Long, Long>> dateBuilder = MaterialDatePicker.Builder.dateRangePicker();
        dateBuilder.setTitleText(title);

        //初始化已选中的日期范围
        if (start != null && end != null) {
            if (end.isAfter(LocalDate.now())) {
                end = LocalDate.now();
            }
            long startTimeMilli = start.atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
            long endTimeMilli = end.atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
            dateBuilder.setSelection(new Pair<>(startTimeMilli, endTimeMilli));
        }

        //创建日期选择对话框构建器
        TypedValue typedValue = new TypedValue();   //获取对话框式的样式资源
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.materialCalendarTheme, typedValue, true);
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = dateBuilder
                .setTheme(typedValue.data)
                .setCalendarConstraints(
                        new CalendarConstraints.Builder()
                                .setValidator(DateValidatorPointBackward.now()) //限制为过去日期
                                .build()
                )
                .build();

        //设置回调方法
        dateRangePicker.addOnPositiveButtonClickListener(listener);

        //显示对话框
        dateRangePicker.show(fragmentManager, TagStrings.DATE_PICKER.t());
    }

    /**
     * 选择日期
     *
     * @param initDate        初始化时选中的日期
     * @param fragmentManager 显示对话框的FragmentManager
     * @param listener        确定监听器
     */
    public static void selectDate(
            @Nullable LocalDate initDate,
            FragmentManager fragmentManager,
            MaterialPickerOnPositiveButtonClickListener<Long> listener
    ) {
        selectDate(initDate, fragmentManager, "选择日期", listener);
    }

    /**
     * 选择日期
     *
     * @param initDate        初始化时选中的日期
     * @param fragmentManager 显示对话框的FragmentManager
     * @param title           日期选择对话框标题
     * @param listener        确定监听器
     */
    public static void selectDate(
            @Nullable LocalDate initDate,
            FragmentManager fragmentManager,
            String title,
            MaterialPickerOnPositiveButtonClickListener<Long> listener
    ) {
        //创建日期选择对话框构建器
        MaterialDatePicker.Builder<Long> dateBuilder = MaterialDatePicker.Builder.datePicker();
        dateBuilder.setTitleText(title);
        if (initDate != null) {
            long dateSelection = initDate.atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
            dateBuilder.setSelection(dateSelection);
        }

        //创建日期选择对话框
        MaterialDatePicker<Long> datePicker = dateBuilder
                .setCalendarConstraints(
                        new CalendarConstraints.Builder()
                                .setValidator(DateValidatorPointBackward.now()) //限制为过去日期
                                .build()
                )
                .build();

        //设置回调
        datePicker.addOnPositiveButtonClickListener(listener);

        //显示对话框
        datePicker.show(fragmentManager, TagStrings.DATE_PICKER.t());
    }

    /**
     * 选择时间
     *
     * @param dateTime        初始化的时间
     * @param fragmentManager 显示对话框所需的FragmentManager
     * @param listener        确定按钮的监听器
     */
    public static void selectTime(
            LocalDateTime dateTime,
            FragmentManager fragmentManager,
            OnTimePickerPositiveBtnClickedListener listener
    ) {
        //创建时间选择器
        MaterialTimePicker.Builder timeBuilder = new MaterialTimePicker.Builder();
        timeBuilder.setTimeFormat(TimeFormat.CLOCK_24H);    //24小时制
        timeBuilder.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK);  //默认使用时钟输入模式而不是键盘
        timeBuilder.setTitleText("选择时间");

        //初始化选择的时间
        if (dateTime != null) {
            int initHour = dateTime.getHour();                  //获取小时
            timeBuilder.setHour(initHour);
            int initMinute = dateTime.getMinute();              //获取分钟
            timeBuilder.setMinute(initMinute);
        }

        //设置回调
        MaterialTimePicker timePicker = timeBuilder.build();
        timePicker.addOnPositiveButtonClickListener(v -> listener.onClicked(timePicker));

        //显示时间选择器
        timePicker.show(fragmentManager, TagStrings.TIME_PICKER.t());
    }

    /**
     * 将日期选择对话框的时间戳转换为日期（UTC时区)
     *
     * @param timeMilli 时间戳
     * @return 该时间戳对应的日期
     */
    public static LocalDate getLocalDateFromTimeMilli(long timeMilli) {
        return Instant.ofEpochMilli(timeMilli)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }

    /**
     * 将日期选择对话框的时间戳转换为时间（UTC时区)
     *
     * @param timeMilli 时间戳
     * @return 该时间戳对应的时间
     */
    public static LocalDateTime getLocalDateTimeFromTimeMilli(long timeMilli) {
        return Instant.ofEpochMilli(timeMilli)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
