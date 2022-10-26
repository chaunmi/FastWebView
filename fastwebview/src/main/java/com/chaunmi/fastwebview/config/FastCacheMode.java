package com.chaunmi.fastwebview.config;

/**
 * Created by Ryan
 * at 2019/11/1
 */
public enum FastCacheMode {
    DEFAULT,  //和原生一样
    NORMAL,   //采用okhttp缓存，缓存模式与WebView设置的缓存模式一致，缓存空间最大100M
    FORCE     //自己写的DISK和内存缓存，包括内存缓存和磁盘缓存，可从assets及其他目录中读取本地文件，可过滤缓存，指定缓存文件类型和缓存大小，缓存内容可控，会缓存，返回码及header和body
}
