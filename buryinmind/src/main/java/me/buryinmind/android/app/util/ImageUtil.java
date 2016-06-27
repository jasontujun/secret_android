package me.buryinmind.android.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.WindowManager;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadOptions;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XStorageUtil;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.controller.ResultListener;

/**
 * Created by jasontujun on 2016/5/16.
 */
public abstract class ImageUtil {

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


    public static int[] getDimension(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return new int[]{options.outWidth, options.outHeight};
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

    public static boolean compress(Context context, String inPath,
                                   String outPath, long maxSize) {
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

    public static void asyncCompress(final Context context, final String inPath,
                                     final File cacheDir, final long maxSize,
                                     final ResultListener<String> listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                if (cacheDir != null && cacheDir.exists() &&
                        !XStorageUtil.isFull(cacheDir.getAbsolutePath(), maxSize)) {
                    File tmpFile = new File(cacheDir, "tmp_" + System.currentTimeMillis() + ".jpg");
                    if (ImageUtil.compress(context, inPath, tmpFile.getAbsolutePath(), maxSize)) {
                        return tmpFile.getAbsolutePath();
                    } else {
                        return null;
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(String result) {
                if (!XStringUtil.isEmpty(result)) {
                    if (listener != null)
                        listener.onResult(true, result);
                } else {
                    if (listener != null)
                        listener.onResult(false, null);
                }
            }
        }.execute();
    }


    public static void uploadImage(final XHttpRequest tokenRequest,
                                   final String filePath,
                                   final ProgressListener<String> listener) {
        final long totalSize = new File(filePath).length();
        if (listener != null)
            listener.onProgress(filePath, 0, totalSize);
        // 从业务服务器获取上传凭证
        MyApplication.getAsyncHttp().execute(
                tokenRequest,
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onCancelled() {

                    }

                    @Override
                    public void onNetworkError() {
                        if (listener != null)
                            listener.onResult(false, null);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        if (listener != null)
                            listener.onResult(false, null);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jsonObject) {
                        try {
                            String key = null;
                            if (jsonObject.has("key") && !jsonObject.isNull("key")) {
                                key = jsonObject.getString("key");
                            }
                           String token = null;
                            if (jsonObject.has("token") && !jsonObject.isNull("token")) {
                                token = jsonObject.getString("token");
                            }
                            if (XStringUtil.isEmpty(key) || XStringUtil.isEmpty(token)) {
                                if (listener != null)
                                    listener.onResult(false, null);
                                return;
                            }
                            // 真正开始上传
                            MyApplication.getUploadManager().put(filePath, key, token,
                                    new UpCompletionHandler() {
                                        @Override
                                        public void complete(String key, ResponseInfo info, JSONObject response) {
                                            if (info.isOK()) {
                                                if (listener != null)
                                                    listener.onResult(true, key);
                                            } else {
                                                if (listener != null)
                                                    listener.onResult(false, null);
                                            }
                                        }
                                    },
                                    new UploadOptions(null, null, false,
                                            new UpProgressHandler() {
                                                public void progress(String key, double percent) {
                                                    if (listener != null)
                                                        listener.onProgress(filePath,
                                                                (long) (totalSize * percent), totalSize);
                                                }
                                            }, null));
                        } catch (JSONException e) {
                            if (listener != null)
                                listener.onResult(false, null);
                        }
                    }
                });
    }


    public static void compressAndUploadImage(final Context context,
                                              final XHttpRequest tokenRequest,
                                              final String filePath,
                                              final ProgressListener<String> listener) {
        // 如果设置了封面图片，再上传图片
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > MyApplication.MAX_IMAGE_SIZE &&
                (filePath.toLowerCase().endsWith(".jpg") ||
                        filePath.toLowerCase().endsWith(".jpeg"))) {
            // 先进行压缩
            ImageUtil.asyncCompress(context, filePath,
                    MyApplication.getCacheDirectory(), MyApplication.MAX_IMAGE_SIZE,
                    new ResultListener<String>() {
                        @Override
                        public void onResult(boolean result, final String compressFilePath) {
                            if (result) {
                                // 压缩成功，上传图片文件
                                uploadImage(tokenRequest, compressFilePath,
                                        new ProgressListener<String>() {
                                            @Override
                                            public void onProgress(String data, long completeSize, long totalSize) {
                                                if (listener != null)
                                                    listener.onProgress(data, completeSize, totalSize);
                                            }

                                            @Override
                                            public void onResult(boolean result, String url) {
                                                // 无论上传是否成功，删除临时的压缩文件
                                                new File(compressFilePath).deleteOnExit();
                                                if (listener != null)
                                                    listener.onResult(result, url);
                                            }
                                        });
                            } else {
                                // 压缩失败
                                if (listener != null)
                                    listener.onResult(false, null);
                            }
                        }
                    });
        } else {
            // 大小没超过上限，直接上传
            uploadImage(tokenRequest, filePath, listener);
        }
    }
}
