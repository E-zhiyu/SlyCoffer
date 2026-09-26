package com.sly.coffer.helpers.appearence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class ImageHelper {
    private static final int TARGET_ICON_SIZE = 48;    //图标的目标大小（dp）
    private static final int CORNER_RADIUS = 24;       //圆角大小（dp）

    /**
     * 将Drawable图标转换为Bitmap
     *
     * @param drawable 原始Drawable图标
     * @param height   目标高度(px)
     * @param width    目标宽度(px)
     * @return 转换后的Bitmap图标
     */
    private static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 获取统一大小的圆角图标
     *
     * @param context  上下文
     * @param drawable 原始Drawable图标
     * @return 重新缩放后的圆角Bitmap图标
     */
    @NonNull
    public static Bitmap getRoundedCornerIcon(Context context, @NonNull Drawable drawable) {
        int targetSize = AppearanceHelper.dpToPx(context, TARGET_ICON_SIZE);

        //将Drawable转换为Bitmap
        Bitmap originalBitmap = drawableToBitmap(drawable, targetSize, targetSize);

        //创建目标Bitmap
        Bitmap output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        //创建Paint并设置BitmapShader
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(originalBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        //创建圆角矩形路径并绘制
        RectF rect = new RectF(0, 0, targetSize, targetSize);
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint);

        return output;
    }
}
