package com.sly.coffer.auxiliary.enums.types;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum ImportDataVersionType {
    BEFORE_ROOM(62),
    BEFORE_EXTRA(81),
    LATEST(Long.MAX_VALUE);
    private final long versionCode; //该数据版本在哪个应用版本代码之前

    ImportDataVersionType(long versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * 根据应用版本号获取数据版本
     *
     * @param versionCode 应用版本号
     * @return 数据版本
     */
    public static ImportDataVersionType getDataVersionByVersionCode(long versionCode) {
        List<ImportDataVersionType> versionTypeList = Arrays.stream(ImportDataVersionType.values())
                .sorted(Comparator.comparing(version -> version.versionCode))
                .collect(Collectors.toList());
        for (ImportDataVersionType type : versionTypeList) {
            if (type.versionCode > versionCode) return type;
        }

        return LATEST;
    }
}
