package com.chaunmi.fastwebview.offline;


import com.chaunmi.fastwebview.config.CacheConfig;
import com.chaunmi.fastwebview.utils.LogUtils;
import com.chaunmi.fastwebview.view.base.WebResource;


/**
 * Created by Ryan
 * on 2020/4/14
 *
 */
public class MemResourceInterceptor implements ResourceInterceptor, Destroyable {

    public MemResourceInterceptor(CacheConfig cacheConfig) {
        int memorySize = cacheConfig.getMemCacheSize();
        if (memorySize > 0) {
            LruCacheManager.getInstance().init(memorySize);
        }
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        WebResource resource = LruCacheManager.getInstance().get(request.getKey());
        if (checkResourceValid(resource)) {
            LogUtils.d(String.format("mem cache hit: %s", request.getUrl()));
            return resource;
        }
        resource = chain.process(request);
        if (checkResourceValid(resource) && resource.isCacheable() && resource.isCacheByOurselves()) {
            LruCacheManager.getInstance().put(request.getKey(), resource);
        }
        return resource;
    }

    private boolean checkResourceValid(WebResource resource) {
        return resource != null
                && resource.getOriginBytes() != null
                && resource.getOriginBytes().length >= 0
                && resource.getResponseHeaders() != null
                && !resource.getResponseHeaders().isEmpty();
    }


    @Override
    public void destroy() {
        LruCacheManager.getInstance().destroy();
    }
}
