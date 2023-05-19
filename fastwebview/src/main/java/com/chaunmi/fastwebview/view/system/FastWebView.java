package com.chaunmi.fastwebview.view.system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chaunmi.fastwebview.view.base.FastOpenApi;
import com.chaunmi.fastwebview.config.CacheConfig;
import com.chaunmi.fastwebview.config.FastCacheMode;
import com.chaunmi.fastwebview.cookie.FastCookieManager;
import com.chaunmi.fastwebview.offline.DiskCacheManager;
import com.chaunmi.fastwebview.offline.LruCacheManager;
import com.chaunmi.fastwebview.offline.ResourceInterceptor;
import com.chaunmi.fastwebview.utils.LogUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Ryan
 * 2018/2/7 下午3:33
 */
public class FastWebView extends WebView implements FastOpenApi {

    private InnerFastClient mFastClient;
    private WebViewClient mUserWebViewClient;
    private boolean mRecycled = false;

    public FastWebView(Context context) {
        super(context);
    }

    public FastWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        if (mFastClient != null) {
            mFastClient.updateProxyClient(client);
        } else {
            super.setWebViewClient(client);
        }
        mUserWebViewClient = client;
    }

    @Override
    public void destroy() {
        release();
        super.destroy();
    }

    public static void clearMemCache() {
        LruCacheManager.getInstance().clear();
    }

    public static void clearDiskCache(File directory, int appVersion, long maxSize) throws IOException {
        DiskCacheManager.getInstance().clear(directory, appVersion, maxSize);
    }


    public void release() {
        LogUtils.e(" release ");
        stopLoading();
        loadUrl("");
        setRecycled(true);
        setWebViewClient(null);
        super.setWebViewClient(null);
        setWebChromeClient(null);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setBlockNetworkImage(false);
        clearHistory();
        clearCache(true);
        int childCount =  getChildCount();
        LogUtils.e(" release children: " + childCount);
        removeAllViews();
        ViewParent viewParent = this.getParent();
        if (viewParent != null && viewParent instanceof ViewGroup) {
            ((ViewGroup) viewParent).removeView(this);
        }
        if (mFastClient != null) {
            mFastClient.destroy();
            mFastClient = null;
        }
        getFastCookieManager().destroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void preload(Context context, String url, FastCacheMode cacheMode, CacheConfig cacheConfig) {
        FastWebView webView = new FastWebView(context);
        webView.setCacheMode(cacheMode, cacheConfig);
        WebSettings webSettings = webView.getSettings();
        //必须要加下面这一句，否则不会去加载通过js加载的js和css等资源
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl(url);
        LogUtils.d("preload url: " + url);
    }

    public void setCacheMode(FastCacheMode mode) {
        setCacheMode(mode, null);
    }

    @Override
    public void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig) {
        if (mode == FastCacheMode.DEFAULT) {
            mFastClient = null;
            if (mUserWebViewClient != null) {
                setWebViewClient(mUserWebViewClient);
            }
        } else {
            mFastClient = new InnerFastClient(this);
            if (mUserWebViewClient != null) {
                mFastClient.updateProxyClient(mUserWebViewClient);
            }
            mFastClient.setCacheMode(mode, cacheConfig);
            super.setWebViewClient(mFastClient);
        }
    }

    public void addResourceInterceptor(ResourceInterceptor interceptor) {
        if (mFastClient != null) {
            mFastClient.addResourceInterceptor(interceptor);
        }
    }

    public void runJs(String function, Object... args) {
        StringBuilder script = new StringBuilder("javascript:");
        script.append(function).append("(");
        if (null != args && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (null == arg) {
                    continue;
                }
                if (arg instanceof String) {
                    arg = "'" + arg + "'";
                }
                script.append(arg);
                if (i != args.length - 1) {
                    script.append(",");
                }
            }
        }
        script.append(");");
        runJs(script.toString());
    }

    private void runJs(String script) {
        this.loadUrl(script);
    }

    @Override
    public WebViewClient getWebViewClient() {
        return mUserWebViewClient != null ? mUserWebViewClient : super.getWebViewClient();
    }

    public FastCookieManager getFastCookieManager() {
        return FastCookieManager.getInstance();
    }

    @Override
    public boolean canGoBack() {
        return !mRecycled && super.canGoBack();
    }

    public boolean isRecycled() {
        return mRecycled;
    }

    public void setRecycled(boolean recycled) {
        this.mRecycled = recycled;
    }

    public static void setDebug(boolean debug) {
        LogUtils.DEBUG = debug;
    }
}
