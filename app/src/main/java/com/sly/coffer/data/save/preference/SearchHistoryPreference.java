package com.sly.coffer.data.save.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryPreference {
    private static final String PREF_NAME = "SearchHistoryPreference";
    public static final String KEY_ACCOUNT_REMARK = "account_remark";   //流水记录备注搜索历史
    public static final String KEY_APP_LIST = "app_list";               //应用列表搜索历史
    public static final String KEY_CAPTURED_NOTIFICATION = "captured_notification"; //捕获的通知
    public static final String KEY_PICKED_VIEW = "picked_view";         //拾取的视图
    public static final String KEY_DEBT_INFO = "debt_info";             //债务信息

    /**
     * 读取搜索历史
     *
     * @param key     需要读取的历史所对应的键值，具体可见{@link SearchHistoryPreference}的静态成员变量
     * @param context 上下文
     * @return 包含已保存的搜索记录的字符串列表
     */
    public static List<String> getHistory(String key, @NonNull Context context) {
        //读取原始JSON字符串
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(key, "[]");

        //将JSON转换为列表
        ObjectMapper mapper = new ObjectMapper();
        List<String> historyList;
        try {
            JavaType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class);
            historyList = mapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            historyList = new ArrayList<>();
        }

        return historyList;
    }

    /**
     * 覆写搜索历史记录
     *
     * @param key         需要读取的历史所对应的键值，具体可见{@link SearchHistoryPreference}的静态成员变量
     * @param historyList 修改后的历史记录列表
     * @param context     上下文
     */
    private static void setHistory(String key, List<String> historyList, Context context) {
        //将列表转换为JSON
        ObjectMapper mapper = new ObjectMapper();
        String json;
        try {
            json = mapper.writeValueAsString(historyList);
        } catch (JsonProcessingException e) {
            json = "[]";
        }

        //写入Preference
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(key, json).apply();
    }

    /**
     * 添加搜索关键词
     *
     * @param keyword 搜索关键词
     * @param key     保存搜索关键词的键，详见{@link SearchHistoryPreference}的静态字符串
     * @param context 上下文
     */
    public static List<String> addKeyword(String keyword, String key, Context context) {
        List<String> historyList = getHistory(key, context);

        //空关键词不保存
        if (keyword == null || keyword.isEmpty()) {
            return historyList;
        }

        historyList.remove(keyword.trim());
        historyList.add(0, keyword.trim());
        setHistory(key, historyList, context);

        return historyList;
    }

    /**
     * 移除搜索历史记录
     *
     * @param keyword 需要移除的搜索关键词
     * @param key     保存搜索关键词的键，详见{@link SearchHistoryPreference}的静态字符串
     * @param context 上下文
     * @return 移除了关键词后的列表
     */
    public static List<String> removeKeyword(String keyword, String key, Context context) {
        List<String> historyList = getHistory(key, context);
        if (keyword == null || keyword.isEmpty()) {
            return historyList;
        }

        historyList.remove(keyword.trim());
        setHistory(key, historyList, context);
        return historyList;
    }

    /**
     * 清空历史搜索记录
     *
     * @param key     需要清空的搜索记录的关键字
     * @param context 上下文
     */
    public static void clearHistory(String key, Context context) {
        setHistory(key, new ArrayList<>(), context);
    }
}
