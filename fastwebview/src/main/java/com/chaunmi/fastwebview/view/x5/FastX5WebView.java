package com.chaunmi.fastwebview.view.x5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.chaunmi.fastwebview.view.base.FastOpenApi;
import com.chaunmi.fastwebview.config.CacheConfig;
import com.chaunmi.fastwebview.config.FastCacheMode;
import com.chaunmi.fastwebview.cookie.FastCookieManager;
import com.chaunmi.fastwebview.offline.DiskCacheManager;
import com.chaunmi.fastwebview.offline.LruCacheManager;
import com.chaunmi.fastwebview.offline.ResourceInterceptor;
import com.chaunmi.fastwebview.utils.LogUtils;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.File;
import java.io.IOException;

public class FastX5WebView extends WebView implements FastOpenApi {

    private InnerX5FastClient mFastClient;
    private WebViewClient mUserWebViewClient;
    private boolean mRecycled = false;


    public FastX5WebView(Context context) {
        super(context);
    }

    public FastX5WebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastX5WebView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        LogUtils.e(" x5 webview destroy ");
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
        ViewParent viewParent = this.getParent();
        if (viewParent != null && viewParent instanceof ViewGroup) {
            LogUtils.e(" release X5 ");
            stopLoading();
            loadUrl("");
            setRecycled(true);
            setWebViewClient(null);
            //必须调用这个来真正释放
            super.setWebViewClient(null);

            setWebChromeClient(null);
            WebSettings settings = getSettings();
            settings.setJavaScriptEnabled(false);
            settings.setBlockNetworkImage(false);
            clearHistory();
            clearCache(true);
            //不能调用下面的removeAllViews()，X5内核WebView内部包了一层，如果调用removeAllViews()就不能正常显示H5页面了
        //    removeAllViews();
            int childCount =  getChildCount();
            LogUtils.e(" release x5 children: " + childCount);
            if (mFastClient != null) {
                mFastClient.destroy();
                mFastClient = null;
            }
            getFastCookieManager().destroy();

            ((ViewGroup) viewParent).removeView(this);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void preload(Context context, String url, FastCacheMode cacheMode, CacheConfig cacheConfig) {
        FastX5WebView webView = new FastX5WebView(context);
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
            mFastClient = new InnerX5FastClient(this);
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
        FastCookieManager.getInstance().setCookieJar(new X5CookieJarImpl());
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
