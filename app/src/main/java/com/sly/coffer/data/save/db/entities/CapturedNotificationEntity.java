package com.sly.coffer.data.save.db.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(
        tableName = "capturedNotifications",
        indices = {
                @Index(value = "title"),
                @Index(value = "content"),
                @Index(value = "appName")
        }
)
public class CapturedNotificationEntity {
    @PrimaryKey(autoGenerate = true)
    private long notificationId;    //主键
    private String title;           //标题
    private String content;         //内容
    private String packageName;     //发送通知的应用的包名
    private String appName;         //发送通知的应用名称
    private LocalDateTime time;     //捕获的时间

    public CapturedNotificationEntity(String title, String content, String packageName, String appName, LocalDateTime time) {
        this.title = title;
        this.content = content;
        this.packageName = packageName;
        this.appName = appName;
        this.time = time;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
