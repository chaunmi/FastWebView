package com.ryan.github.webview.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chaunmi.fastwebview.view.base.WebResource;
import com.google.gson.Gson;
import com.chaunmi.fastwebview.view.system.FastWebView;
import com.chaunmi.fastwebview.utils.FastWebViewPool;
import com.chaunmi.fastwebview.config.CacheConfig;
import com.chaunmi.fastwebview.config.DefaultMimeTypeFilter;
import com.chaunmi.fastwebview.config.FastCacheMode;
import com.chaunmi.fastwebview.cookie.CookieInterceptor;
import com.chaunmi.fastwebview.cookie.FastCookieManager;
import com.chaunmi.fastwebview.offline.Chain;
import com.chaunmi.fastwebview.offline.ResourceInterceptor;
import com.chaunmi.fastwebview.utils.LogUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static com.ryan.github.webview.sample.MainActivity.sUseWebViewPool;

/**
 * Created by Ryan
 * at 2019/11/4
 */
public class WebViewActivity extends Activity {

    private static final String TAG = "FastWebView_activity";
    private FastWebView fastWebView;
    private long initStartTime;
    private long startTime;

    public static final String url = "https://192.168.0.132/";  //"http://192.168.10.37:5500/h5/dist/index.html#/index"; //  "https://xw.qq.com/"; // "https://github.com/Ryan-Shz";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FastWebView.setDebug(true);
        LogUtils.d("------------- start once load -------------");
        startTime = SystemClock.uptimeMillis();
        initStartTime = SystemClock.uptimeMillis();
        if (sUseWebViewPool) {
            fastWebView = FastWebViewPool.acquire(this);
        } else {
            LogUtils.d("create new webview instance.");
            fastWebView = new FastWebView(this);
        }
        fastWebView.setWebChromeClient(new MonitorWebChromeClient());
        fastWebView.setWebViewClient(new MonitorWebViewClient());
        setContentView(fastWebView);
        fastWebView.setFocusable(true);
        fastWebView.setFocusableInTouchMode(true);
        WebSettings webSettings = fastWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setBlockNetworkImage(true);

        // 设置正确的cache mode以支持离线加载
        int cacheMode = NetworkUtils.isAvailable(this) ?
                WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK;
        webSettings.setCacheMode(cacheMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(fastWebView, true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CacheConfig config = new CacheConfig.Builder(this)
                .setCacheDir(getCacheDir() + File.separator + "custom")
        //        .setExtensionFilter(new CustomMimeTypeFilter())
        //        .setAssetsDir("static")
                .setOtherStorageDir(getExternalCacheDir().getPath())
                .build();
        fastWebView.setCacheMode(FastCacheMode.FORCE, config);

        fastWebView.addResourceInterceptor(new ResourceInterceptor() {
            @Override
            public WebResource load(Chain chain) {
                String url = chain.getRequest().getUrl();
                Log.d(TAG, " load url: " + url);
                return chain.process(chain.getRequest());
            }
        });


        fastWebView.addJavascriptInterface(this, "android");


        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();// 移除旧的[可以省略]
        cookieManager.setCookie(url, "custom=12345678910;");
        CookieSyncManager.getInstance().sync();

        FastCookieManager fastCookieManager = fastWebView.getFastCookieManager();
        fastCookieManager.addRequestCookieInterceptor(new CookieInterceptor() {
            @Override
            public List<Cookie> newCookies(HttpUrl url, List<Cookie> originCookies) {
                for (Cookie cookie : originCookies) {
                    Log.v(TAG, "request cookies: " + cookie.toString());
                }
                return originCookies;
            }
        });
        fastCookieManager.addResponseCookieInterceptor(new CookieInterceptor() {
            @Override
            public List<Cookie> newCookies(HttpUrl url, List<Cookie> originCookies) {
                for (Cookie cookie : originCookies) {
                    Log.v(TAG, "response cookies: " + cookie.toString());
                }
                return originCookies;
            }
        });

        Map<String, String> headers = new HashMap<>();
        headers.put("custom", "test");
        fastWebView.loadUrl(url, headers);
    }

    @JavascriptInterface
    public void sendResource(String timing) {
        Performance performance = new Gson().fromJson(timing, Performance.class);
        Log.v(TAG, "request cost time: " + (performance.getResponseEnd() - performance.getRequestStart()) + "ms");
        Log.v(TAG, "dom build time: " + (performance.getDomComplete() - performance.getDomInteractive()) + "ms.");
        Log.v(TAG, "dom ready time: " + (performance.getDomContentLoadedEventEnd() - performance.getNavigationStart()) + "ms.");
        Log.v(TAG, "load time: " + (performance.getLoadEventEnd() - performance.getNavigationStart()) + "ms.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fastWebView != null) {
            if (sUseWebViewPool) {
                FastWebViewPool.release(fastWebView);
            } else {
                fastWebView.destroy();
            }
        }
    }

    public class MonitorWebViewClient extends WebViewClient {

        private boolean first = true;
        long startTime = 0;
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            startTime = System.currentTimeMillis();
            LogUtils.d(" onPageStarted url: " + url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            LogUtils.d(" onLoadResource url: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            LogUtils.d(" onPageFinished cost: " + (System.currentTimeMillis() - startTime) + ", url: " + url);
            view.getSettings().setBlockNetworkImage(false);
            view.loadUrl("javascript:android.sendResource(JSON.stringify(window.performance.timing))");
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (first) {
                LogUtils.d("init cost time: " + (SystemClock.uptimeMillis() - initStartTime));
                first = false;
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LogUtils.i(" shouldOverrideUrlLoading: " + url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            String scheme = request.getUrl().getScheme();
            LogUtils.i(" shouldOverrideUrlLoading request: " + url);
            if(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                return super.shouldOverrideUrlLoading(view, request);
            }else {
//                try{
//                    if(url.startsWith("baiduboxapp://") || url.startsWith("baiduboxlite://" )){
//                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                        startActivity(intent);
//                        return true;
//                    }
//                }catch (Exception e) {
//                    LogUtils.e(" shouldOverrideUrlLoading request error: " + e.getLocalizedMessage());
//                    return false;
//                }
//                view.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            LogUtils.e(" onReceivedError errorCode: " + errorCode + ", description: " + description + ", failingUrl: " + failingUrl);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            LogUtils.e(" onReceivedHttpError ");
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            LogUtils.e(" onReceivedSslError " + error.toString());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fastWebView.canGoBack()) {
                fastWebView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public class MonitorWebChromeClient extends WebChromeClient {

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.d(TAG, "white screen time: " + (SystemClock.uptimeMillis() - startTime));
        }
    }

    public class CustomMimeTypeFilter extends DefaultMimeTypeFilter {
        CustomMimeTypeFilter() {
            addMimeType("text/html");
        }
    }
}
