package com.chaunmi.fastwebview.utils;

import android.text.TextUtils;

import androidx.collection.ArraySet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Headers;

/**
 * Created by Ryan
 * on 2019/10/25
 */
public class HeaderUtils {

    public static Map<String, String> generateHeadersMap(Headers headers) {
        Map<String, String> headersMap = new HashMap<>();
        for (String key : headers.names()) {
            StringBuilder values = new StringBuilder();
            for (String value : listToSet(headers.values(key))) {
                if (!TextUtils.isEmpty(values)) {
                    values.append(" ");
                }
                values.append(value);
            }
            headersMap.put(key, values.toString().trim());
        }
        return headersMap;
    }

    private static Set<String> listToSet(List<String> origin) {
        Set<String> target = new ArraySet<>(origin.size());
        target.addAll(origin);
        return target;
    }

    public static Map<String, String> generateHeadersMap(Map<String, List<String>> headers) {
        Map<String, String> headersMap = new HashMap<>();
        int index = 0;
        for (String key : headers.keySet()) {
            StringBuilder values = new StringBuilder();
            for (String value : headers.get(key)) {
                values.append(value);
                if (index++ > 0) {
                    values.append(",");
                }
            }
            index = 0;
            headersMap.put(key, values.toString());
        }
        return headersMap;
    }

    /**
     * 根据header返回contentType
     * @param headers
     * @return
     */
    public static String getContentType(Map<String, String> headers) {
        String contentType = null;
        if (headers != null) {
            String uppercaseKey = "Content-Type";
            String lowercaseKey = uppercaseKey.toLowerCase();
            String contentTypeValue = headers.containsKey(uppercaseKey) ? headers.get(uppercaseKey) : headers.get(lowercaseKey);
            if (!TextUtils.isEmpty(contentTypeValue)) {
                String[] contentTypeArray = contentTypeValue.split(";");
                if (contentTypeArray.length >= 1) {
                    contentType = contentTypeArray[0];
                }
            }
        }
        return contentType;
    }

}
