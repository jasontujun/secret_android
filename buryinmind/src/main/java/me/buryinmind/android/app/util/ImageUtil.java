package me.buryinmind.android.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * Created by jasontujun on 2016/5/16.
 */
public class ImageUtil {

    //计算图片的缩放值
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height/ (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }


    public static Bitmap getBitmap(Context context, String filePath) {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        android.view.Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return getBitmap(filePath, point.x, point.y);
    }

    // 根据路径获得图片并压缩，返回bitmap用于显示
    public static Bitmap getBitmap(String filePath,
                                   int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static boolean compress(Context context, String inPath, String outPath, long maxSize) {
        Bitmap bitmap = getBitmap(context, inPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (maxSize > 0) {
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            while (baos.toByteArray().length > maxSize) {
                baos.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        }
        try {
            FileOutputStream fos = new FileOutputStream(outPath);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
