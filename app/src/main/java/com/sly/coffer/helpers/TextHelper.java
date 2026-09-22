package com.sly.coffer.helpers;

import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class TextHelper {
    //英文数字缩写体系（1000进制）
    private static final String[] ENGLISH_SUFFIXES = {
            "", "K", "M", "B", "T"  // Thousand, Million, Billion, Trillion
    };
    //正则表达式中需要转义的字符
    private static final String[] REGEX_CHAR = {
            "?", "+", "*", "(", ")", "{", "}", "$", "^"
    };
    //文件大小符号
    private static final String[] FILE_SIZE_CHAR = {
            "", "K", "M", "G", "T"
    };

    /**
     * 将单位为 B 的文件大小简写为字符串
     *
     * @param fileSize 要转换的Double值
     * @return 简写后的字符串
     */
    @NonNull
    public static String shortenFileSize(long fileSize) {
        double absValue = Math.abs(fileSize);
        int divisor = 1000;

        // 计算应该使用哪个单位
        int index = 0;
        double scaledValue = absValue;

        while (scaledValue >= divisor && index < FILE_SIZE_CHAR.length - 1) {
            scaledValue /= divisor;
            index++;
        }

        // 有后缀：使用普通格式（# 表示可选，不强制显示小数）
        String pattern = "#0." + "#".repeat(1);
        DecimalFormat df = new DecimalFormat(pattern);
        String formatted = df.format(scaledValue);

        // 组装结果
        return formatted + FILE_SIZE_CHAR[index];
    }

    /**
     * 获取指定无障碍节点及其子节点出现的所有文本
     *
     * @param node 任意无障碍节点
     * @return 该节点及其子节点出现过的文本集合
     */
    @NonNull
    public static Set<String> extractAllTextsFromNode(AccessibilityNodeInfo node) {
        Set<String> texts = new HashSet<>();
        if (node == null) return texts;

        // 使用栈实现非递归遍历，避免递归过深导致栈溢出
        Stack<AccessibilityNodeInfo> stack = new Stack<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            AccessibilityNodeInfo currentNode = stack.pop();
            if (currentNode == null) continue;

            // 收集文本
            CharSequence text = currentNode.getText();
            if (text != null && !TextUtils.isEmpty(text)) {
                texts.add(text.toString().toLowerCase()); // 转小写实现大小写不敏感
            }

            // 子节点入栈
            for (int i = 0; i < currentNode.getChildCount(); i++) {
                AccessibilityNodeInfo child = currentNode.getChild(i);
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return texts;
    }

    /**
     * 检查文本集合是否包含某个关键词（包含匹配）
     *
     * @param keyword 关键词
     * @param texts   待匹配的文本集合
     */
    @Contract(pure = true)
    public static boolean containsKeyword(@NonNull Set<String> texts, String keyword) {
        for (String text : texts) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断一组关键词是否都在文本集合中出现（包含匹配）
     *
     * @param keywords 关键词组合
     * @param allTexts 待搜索的文本集合
     * @return keywords 中的所有关键词是否都在 allTexts 中出现
     */
    public static boolean checkGroupMatchOptimized(@NonNull String[] keywords, Set<String> allTexts) {
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }

            if (!containsKeyword(allTexts, keyword)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 自动缩写数字（可配置语言和精度）
     *
     * @param value 要转换的Double值
     * @return 缩写后的字符串
     */
    @NonNull
    public static String abbreviate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }

        // 处理负数
        boolean negative = value < 0;
        double absValue = Math.abs(value);

        int divisor = 1000;

        // 计算应该使用哪个单位
        int index = 0;
        double scaledValue = absValue;

        while (scaledValue >= divisor && index < ENGLISH_SUFFIXES.length - 1) {
            scaledValue /= divisor;
            index++;
        }

        // 格式化数字
        String suffix = ENGLISH_SUFFIXES[index];
        String formatted;
        int decimalPlaces = absValue < 0.1 ? 2 : 1;
        if (suffix.isEmpty()) {
            // 没有后缀（数值 < 1000 或 < 10000）：强制显示 .0
            String pattern = "0." + "0".repeat(decimalPlaces);
            DecimalFormat df = new DecimalFormat(pattern);
            formatted = df.format(scaledValue);
        } else {
            // 有后缀：使用普通格式（# 表示可选，不强制显示小数）
            String pattern = "#0." + "#".repeat(decimalPlaces);
            DecimalFormat df = new DecimalFormat(pattern);
            formatted = df.format(scaledValue);
        }

        // 组装结果
        return (negative ? "-" : "") + formatted + ENGLISH_SUFFIXES[index];
    }

    /**
     * 从文本中删除部分正则表达式中带有特殊含义的字符
     *
     * @param origin 原始字符串
     * @return 删除字符后的字符串
     */
    public static String removeRegexChar(String origin) {
        String result = origin;
        for (String target : REGEX_CHAR) {
            result = result.replace(target, "\\" + target);
        }
        return result;
    }
}
