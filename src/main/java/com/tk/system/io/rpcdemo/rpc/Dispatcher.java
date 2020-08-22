package com.tk.system.io.rpcdemo.rpc;

import java.util.concurrent.ConcurrentHashMap;

public class Dispatcher {

    private static Dispatcher dispatcher = null;
    static {
        dispatcher = new Dispatcher();
    }

    public static Dispatcher getDispatcher() {
        return dispatcher;
    }

    private Dispatcher() {}

    private static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

    public void register(String k, Object o) {
        invokeMap.putIfAbsent(k, o);
    }

    public Object get(String k) {
        return invokeMap.get(k);
    }
}
