package com.tk.system.io.rpcdemo.rpc;

import com.tk.system.io.rpcdemo.utils.PackageMsg;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  响应处理回调函数
 */
public class ResponseMappingCallback {
    //    private static ConcurrentHashMap<Long, Runnable> mapping = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void addCallback(long requestID, CompletableFuture cb) {
        mapping.putIfAbsent(requestID, cb);
    }

    public static void runCallback(PackageMsg pm) {
        CompletableFuture cf = mapping.get(pm.getHeader().getRequestID());
        cf.complete(pm.getContent().getRes());
        removeCB(pm.getHeader().getRequestID());
    }

    private static void removeCB(long requestID) {
        mapping.remove(requestID);
    }
}
