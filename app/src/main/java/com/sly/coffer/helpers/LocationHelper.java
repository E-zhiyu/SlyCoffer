package com.sly.coffer.helpers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.sly.coffer.auxiliary.enums.unique.LogTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class LocationHelper {
    static class LocationRequestListener implements LocationListener {
        private final Function<Location, Void> successFun;  //位置更新回调
        private final Runnable clearAction;                 //清理方法
        private final Function<String, Void> providerDisabledFun;   //提供者被禁用的回调

        /**
         * @param updateFun           位置更新回调
         * @param clearAction         清理方法
         * @param providerDisabledFun 提供者被禁用的回调
         */
        LocationRequestListener(Function<Location, Void> updateFun, Function<String, Void> providerDisabledFun, Runnable clearAction) {
            this.successFun = updateFun;
            this.clearAction = clearAction;
            this.providerDisabledFun = providerDisabledFun;
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            successFun.apply(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            clearAction.run();
            providerDisabledFun.apply(provider);
        }
    }

    public interface CurrentLocationListener {
        /**
         * 位置更新回调
         *
         * @param location 更新后的位置
         */
        void onUpdated(Location location);

        /**
         * 定位服务关闭回调
         *
         * @param provider 关闭的位置 Provider
         */
        void onProviderDisabled(String provider);

        /**
         * 开始监听的回调
         */
        void onListenStarted();

        /**
         * 请求失败回调
         *
         * @param message 失败提示
         */
        void onRequestFailed(String message);
    }

    /**
     * 持续监听当前位置
     *
     * @param context  上下文
     * @param listener 位置监听器
     * @return 用于取消监听的{@link LocationListener}实例
     */
    @Nullable
    public static List<LocationListener> requestCurrentLocation(@NonNull Context context, @NonNull CurrentLocationListener listener) {
        //获取定位服务
        Log.d(LogTags.LOCATION_HELPER.n(), "开始获取当前位置");
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            Log.e(LogTags.LOCATION_HELPER.n(), "无法获取LocationManager");
            listener.onRequestFailed("位置服务初始化异常");
            return null;
        }

        //检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LogTags.LOCATION_HELPER.n(), "没有定位权限");
            listener.onRequestFailed("请授予定位权限");
            return null;
        }

        //检查手机的 GPS 或网络定位开关是否开启
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.e(LogTags.LOCATION_HELPER.n(), "定位服务开关未启用");
            listener.onRequestFailed("请开启定位服务");

            //跳转到定位服务开关界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);

            return null;
        }

        //实例化位置更新监听
        List<LocationListener> listenerList = new ArrayList<>();
        LocationRequestListener gpsListener = new LocationRequestListener(
                location -> {
                    Log.i(LogTags.LOCATION_HELPER.n(), "位置成功更新" + location);
                    listener.onUpdated(location);
                    return null;
                },
                provider -> {
                    Log.w(LogTags.LOCATION_HELPER.n(), provider + "关闭");
                    listener.onProviderDisabled(provider);
                    return null;
                },
                () -> removeLocationListeners(context, listenerList)
        );
        listenerList.add(gpsListener);
        LocationRequestListener networkListener = new LocationRequestListener(
                location -> {
                    Log.i(LogTags.LOCATION_HELPER.n(), "位置成功更新" + location);
                    listener.onUpdated(location);
                    return null;
                },
                provider -> {
                    Log.w(LogTags.LOCATION_HELPER.n(), provider + "关闭");
                    listener.onProviderDisabled(provider);
                    return null;
                },
                () -> removeLocationListeners(context, listenerList)
        );
        listenerList.add(networkListener);

        //同时监听 GPS 和网络的位置
        listener.onListenStarted();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1500,
                1.0f,
                gpsListener
        );
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1500,
                1.0f,
                networkListener
        );

        return listenerList;
    }

    /**
     * 取消位置监听
     *
     * @param context      上下文
     * @param listenerList 需要取消监听的监听器
     */
    public static void removeLocationListeners(Context context, @Nullable List<LocationListener> listenerList) {
        //判空
        Log.d(LogTags.LOCATION_HELPER.n(), "开始移除位置监听");
        if (listenerList == null || listenerList.isEmpty()) {
            Log.w(LogTags.LOCATION_HELPER.n(), "监听器列表为空");
            return;
        }

        //获取服务
        LocationManager manager = context.getSystemService(LocationManager.class);
        if (manager == null) return;

        //移除监听
        for (int i = 0; i < listenerList.size(); i++) {
            LocationListener savedListener = listenerList.get(i);
            if (savedListener != null) {
                manager.removeUpdates(savedListener);
                listenerList.set(i, null);
            }
        }
    }

    /**
     * 将经纬度和海拔转换为表示坐标的字符串
     *
     * @param latitude       纬度
     * @param longitude      经度
     * @param altitude       海拔
     * @param isFineLocation 是否为精确位置
     * @return 表示坐标的字符串
     */
    @NonNull
    public static String locationToString(
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Double altitude,
            boolean isFineLocation
    ) {
        StringBuilder builder = new StringBuilder();

        //纬度
        if (latitude != null) {
            builder.append(convertToDMS(latitude, true));
        }

        //经度
        if (longitude != null) {
            if (latitude != null) {
                builder.append(" ");
            }

            builder.append(convertToDMS(longitude, false));
        }

        //是否为精确位置
        if (!isFineLocation) {
            builder.append("（粗略）");
        }

        //海拔
        if (altitude != null) {
            builder.append("\n");

            String altitudeStr = String.format(
                    Locale.getDefault(),
                    "海拔：%.2f米",
                    altitude
            );
            builder.append(altitudeStr);
        }

        return builder.toString();
    }

    /**
     * 将单个十进制度数值转换为度分秒字符串
     *
     * @param coordinate 经度或者纬度
     * @param isLatitude 是否为纬度
     */
    @NonNull
    private static String convertToDMS(double coordinate, boolean isLatitude) {
        char hemisphere;
        if (isLatitude) {
            hemisphere = coordinate >= 0 ? 'N' : 'S'; // 北纬 N，南纬 S
        } else {
            hemisphere = coordinate >= 0 ? 'E' : 'W'; // 东经 E，西经 W
        }

        double abs = Math.abs(coordinate);

        //度
        int degrees = (int) abs;

        //分
        double minutesNotTruncated = (abs - degrees) * 60;
        int minutes = (int) minutesNotTruncated;

        //秒
        double seconds = (minutesNotTruncated - minutes) * 60;

        return String.format(Locale.getDefault(),
                "%d°%d′%.2f″%c",
                degrees, minutes, seconds, hemisphere);
    }
}
