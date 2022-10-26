package com.chaunmi.fastwebview.okhttp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.chaunmi.fastwebview.cookie.FastCookieManager;
import com.chaunmi.fastwebview.utils.HttpsUtils;
import com.chaunmi.fastwebview.utils.LogUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Ryan
 * at 2019/9/26
 */
public class OkHttpClientProvider {
    private static final String TAG = "OkHttpClientProvider";
    private static final String CACHE_OKHTTP_DIR_NAME = "cached_webview_okhttp";
    private static final int OKHTTP_CACHE_SIZE = 100 * 1024 * 1024;
    private static volatile OkHttpClientProvider sInstance;
    private OkHttpClient mClient;

    private OkHttpClientProvider(Context context) {
        createOkHttpClient(context);
    }

    private static OkHttpClientProvider getInstance(Context context) {
        if (sInstance == null) {
            synchronized (OkHttpClientProvider.class) {
                if (sInstance == null) {
                    sInstance = new OkHttpClientProvider(context);
                }
            }
        }
        return sInstance;
    }


    private void createOkHttpClient(Context context) {
        HttpsUtils.SSLParams params = HttpsUtils.getSslSocketFactory(null, null, null);
        String dir = context.getCacheDir() + File.separator + CACHE_OKHTTP_DIR_NAME;
        mClient = new OkHttpClient.Builder()
                .cookieJar(FastCookieManager.getInstance().getCookieJar())
                .addInterceptor(getLoggingInterceptor())
                .cache(new Cache(new File(dir), OKHTTP_CACHE_SIZE))
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .hostnameVerifier(new HttpsUtils.UnSafeHostnameVerifier())
                .sslSocketFactory(params.sSLSocketFactory, params.trustManager)
                // auto redirects is not allowed, bc we need to notify webview to do some internal processing.
             //   .followSslRedirects(false)
             //   .followRedirects(false)
                .build();
    }


    public static OkHttpClient get(Context context) {
        return getInstance(context).mClient;
    }

    public HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(@NonNull String s) {
                LogUtils.i(s);
            }
        });
        if(LogUtils.DEBUG) {
            interceptor.level(HttpLoggingInterceptor.Level.BODY);
        }else {
            interceptor.level(HttpLoggingInterceptor.Level.BASIC);
        }
        return interceptor;
    }
}
