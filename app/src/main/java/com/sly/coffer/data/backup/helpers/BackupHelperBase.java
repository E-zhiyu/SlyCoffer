package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import androidx.room.RoomDatabase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.auxiliary.classes.ImportResult;
import com.sly.coffer.auxiliary.enums.types.ImportDataVersionType;
import com.sly.coffer.helpers.file.FileHelper;

import java.io.File;
import java.io.IOException;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * 数据帮助器基类
 *
 * @param <D> 数据库实例类型
 * @param <M> 数据字典类型
 */
abstract public class BackupHelperBase<D extends RoomDatabase, M> {
    protected D db;                     //数据库帮助器
    protected Class<M> mapClass;        //数据字典类型
    protected String tempDataFileName;  //临时数据文件名称

    public BackupHelperBase(Context context) {
        db = getDatabase(context);
        mapClass = getMapClass();
        tempDataFileName = getTempDataFileName();
    }

    /**
     * 子类继承用来获取数据集合类型的方法
     *
     * @return 数据集合类型
     */
    protected abstract Class<M> getMapClass();

    /**
     * 子类继承用来获取数据库实例的方法
     *
     * @param context 上下文
     * @return 数据库实例
     */
    protected abstract D getDatabase(Context context);

    /**
     * 从数据库中获取数据集合
     *
     * @return 数据集合
     */
    protected abstract M getAllDataInMap();

    /**
     * 将 map 中的数据保存至数据库的方法
     *
     * @param context 上下文
     * @param map     数据集合
     */
    protected abstract ImportResult saveDataInMapToDb(Context context, M map);

    /**
     * 待继承的设置临时 JSON 文件名称的方法
     *
     * @return 临时 JSON 文件名称
     */
    protected abstract String getTempDataFileName();

    /**
     * 将 Room（v1.9.0）重构之前的旧数据的 JSON 文本转换为新数据集合
     *
     * @param json v1.9.0之前旧数据的 JSON 文本
     * @return 现有的数据集合
     * @throws JsonProcessingException JSON 反序列化出错时引发的异常
     */
    protected abstract M convertOldDataBeforeRoom(String json) throws JsonProcessingException;

    /**
     * 将 Extra（v1.13.0）重构之前的旧数据的 JSON 文本转换为新的数据集合
     *
     * @param json v1.13.0之前旧数据的 JSON 文本
     * @return 现有的数据集合
     * @throws JsonProcessingException JSON 反序列化出错时引发的异常
     */
    protected abstract M convertOldDataBeforeExtra(String json) throws JsonProcessingException;

    /**
     * 从临时文件中导入数据
     *
     * @param context 上下文
     * @param file        临时文件对象
     * @param versionType 导入数据的版本种类
     * @return 带有导入结果的{@link Single}对象
     */
    public Single<ImportResult> importDataFromTempFile(Context context, File file, ImportDataVersionType versionType) {
        return Single.defer(() -> {
            try {
                //读取文件内容
                String json = FileHelper.readContent(file);

                //得到数据字典实例
                ObjectMapper mapper = new ObjectMapper();
                M dataMap;
                if (versionType == ImportDataVersionType.BEFORE_ROOM) {
                    dataMap = convertOldDataBeforeRoom(json);
                } else if (versionType == ImportDataVersionType.BEFORE_EXTRA) {
                    dataMap = convertOldDataBeforeExtra(json);
                } else {
                    dataMap = mapper.readValue(json, mapClass);
                }

                //将对应的数据写入数据库
                ImportResult result = saveDataInMapToDb(context, dataMap);
                return Single.just(result);
            } catch (IOException e) {
                return Single.error(e);
            }
        });
    }

    /**
     * 将数据库中的数据转换为JSON字符串
     *
     * @return 转换得到的JSON字符串
     * @throws JsonProcessingException JSON解析失败引发的异常
     */
    private String getDataInJSON() throws JsonProcessingException {
        M dataMap = getAllDataInMap();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(dataMap);
    }

    /**
     * 导出数据并将JSON序列化后的字符串写入到临时数据文件中
     *
     * @param context 上下文
     * @return 是否成功
     */
    public Completable exportDataToTempFile(Context context) {
        return Completable.defer(() -> {
            try {
                String content = getDataInJSON();
                FileHelper.writeContentToTempDataFile(context, tempDataFileName, content);

                return Completable.complete();
            } catch (IOException e) {
                return Completable.error(e);
            }
        });
    }
}
