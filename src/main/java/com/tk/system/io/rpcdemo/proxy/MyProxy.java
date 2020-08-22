package com.tk.system.io.rpcdemo.proxy;

import com.tk.system.io.rpcdemo.rpc.Dispatcher;
import com.tk.system.io.rpcdemo.rpc.protocol.MyContent;
import com.tk.system.io.rpcdemo.rpc.transport.ClientFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

public class MyProxy {

    /**
     * 动态代理
     */
    public static  <T>T proxyGet(Class<T> interfaceInfo) {

        //获取类加载器
        ClassLoader classLoader = interfaceInfo.getClassLoader();
        //获取接口类
        Class<?>[] methodInfo = {interfaceInfo};

        //  LOCAL REMOTE  实现：  用到dispatcher  直接给你返回，还是本地调用的时候也代理一下
        Dispatcher dispatcher = Dispatcher.getDispatcher();

        return (T) Proxy.newProxyInstance(classLoader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                Object res = null;
                Object o = dispatcher.get(interfaceInfo.getName());
                if (o == null) {
                    String name = interfaceInfo.getName();//服务名称
                    String methodName = method.getName();//方法名称
                    Class<?>[] parameterTypes = method.getParameterTypes();//参数类型
                    MyContent content = new MyContent();

                    content.setName(name);
                    content.setMethodName(methodName);
                    content.setParameterType(parameterTypes);
                    content.setArgs(args);

                    CompletableFuture resFuture = ClientFactory.transport(content);
                    res = resFuture.get();
                } else {
                    System.out.println("local ......");
                    Class<?> clazz = o.getClass();
                    try {
                        Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
                        res = m.invoke(o, args);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return res; //阻塞
            }
        });
    }
}
