package com.tk.system.io.rpcdemo;

import com.tk.system.io.rpcdemo.proxy.MyProxy;
import com.tk.system.io.rpcdemo.rpc.Dispatcher;
import com.tk.system.io.rpcdemo.rpc.transport.MyDecode;
import com.tk.system.io.rpcdemo.rpc.transport.ServerRequestHandler;
import com.tk.system.io.rpcdemo.service.Car;
import com.tk.system.io.rpcdemo.service.Fly;
import com.tk.system.io.rpcdemo.service.MyCar;
import com.tk.system.io.rpcdemo.service.MyFly;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2020/08/18 TK
 */
public class MyRPC {

    /**
     * 服务端
     */
    @Test
    public void startServer() {

        MyCar myCar = new MyCar();
        MyFly myFly = new MyFly();
        Dispatcher dispatcher = Dispatcher.getDispatcher();
        dispatcher.register(Car.class.getName(), myCar);
        dispatcher.register(Fly.class.getName(), myFly);

        NioEventLoopGroup boss = new NioEventLoopGroup(20);
        NioEventLoopGroup worker = boss;
//        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss,worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
//                        System.out.println("server accept client port: " + ch.remoteAddress().getPort());
                        ch.pipeline()
                                .addLast(new MyDecode())
                                .addLast(new ServerRequestHandler(dispatcher));
                    }
                }).bind(new InetSocketAddress("192.168.150.1", 9999));
        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送get请求  请求远程 car接口的move方法
     */
    @Test
    public void get() {

//        new Thread(() -> {
//            startServer();
//        }).start();

        System.out.println("server started...");

        AtomicInteger num = new AtomicInteger(0);
        int size = 20;
        Runnable runnable = () -> {
            Car car = MyProxy.proxyGet(Car.class); //动态代理
            String arg = "hello tk" + num.incrementAndGet();
            String res = car.move(arg);
            System.out.println(res + " ---src arg: " + arg);

//            Fly fly = proxyGet(Fly.class); //动态代理
//            String arg = "hello tk" + num.incrementAndGet();
//            fly.xxoo(arg);
        };
        Thread[] threads = new Thread[size];

        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(runnable);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}


























