package com.sly.coffer.data.backup.helpers;

import android.content.Context;

import androidx.room.RoomDatabase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sly.coffer.helpers.file.FileHelper;

import java.io.File;
import java.io.IOException;

import io.reactivex.rxjava3.core.Completable;

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

    protected abstract Class<M> getMapClass();

    protected abstract D getDatabase(Context context); //子类需要实现的生成数据库实例的方法

    protected abstract M getAllDataInMap();             //获取数据字典的方法

    protected abstract void saveDataInMapToDb(Context context, M map);   //将map中的数据保存至数据库的方法

    protected abstract String getTempDataFileName();    //设置临时数据文件名称

    protected abstract M convertOldData(String json) throws JsonProcessingException;   //将旧数据的 JSON 文本转换为新数据集合

    /**
     * 从临时文件中导入数据
     *
     * @param file      临时文件对象
     * @param isOldData 是否导入的是旧版数据（v1.9.0之前的版本）
     * @return 是否完成
     */
    public Completable importDataFromTempFile(Context context, File file, boolean isOldData) {
        return Completable.defer(() -> {
            try {
                //读取文件内容
                String json = FileHelper.readContent(file);

                //得到数据字典实例
                ObjectMapper mapper = new ObjectMapper();
                M dataMap;
                if (isOldData) {
                    dataMap = convertOldData(json);
                } else {
                    dataMap = mapper.readValue(json, mapClass);
                }

                //将对应的数据写入数据库
                saveDataInMapToDb(context, dataMap);

                return Completable.complete();
            } catch (IOException e) {
                return Completable.error(e);
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
