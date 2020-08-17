package com.tk.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorThreadGroup {

    private SelectorThread[] sts;
    private ServerSocketChannel server = null;
    private AtomicInteger xid = new AtomicInteger(0);
    private SelectorThreadGroup stg = this;

    public SelectorThreadGroup() {
    }

    public void setWorker(SelectorThreadGroup worker) {
        stg = worker;
    }

    public SelectorThreadGroup(int num) {
        sts = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            //混杂模式
//            sts[i] = new SelectorThread(this);
            sts[i] = new SelectorThread();
            new Thread(sts[i]).start();
        }
    }


    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            //注册selector
            nextSelectorV3(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void nextSelector(Channel channel) {
//        SelectorThread st = next();
//        st.lbq.add(channel);
//        st.selector.wakeup();
//
//    }

    private SelectorThread next() {
        int i = xid.incrementAndGet() % sts.length;
        return sts[i];
    }

    /**
     * 混杂模式中 区分监听线程与读写线程
     */
    //将sts[0]设置为监听服务
//    public void nextSelectorV2(Channel channel) {
//        try {
//
//            if (channel instanceof ServerSocketChannel) {
//                sts[0].lbq.put(channel);
//                sts[0].selector.wakeup();
//            } else {
//                SelectorThread st = nextV2();
//                st.lbq.put(channel);
//                st.selector.wakeup();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    //不在sts[0]上分配
//    private SelectorThread nextV2() {
//        int i = xid.incrementAndGet() % (sts.length-1);
//        return sts[i + 1];
//    }

    /**
     * 区分 boss 与 worker 写法
     */
    public void nextSelectorV3(Channel channel) {
        try {
            if (channel instanceof ServerSocketChannel) {
                SelectorThread st = next(); //选择了 boss组中的一个线程后需要更新该线程的worker组
                st.setWorker(stg);
                st.lbq.put(channel);
                st.selector.wakeup();
            } else {
                SelectorThread st = nextV3();
                st.lbq.put(channel);
                st.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //worker组分配 stg为worker组引用
    private SelectorThread nextV3() {
        int i = xid.incrementAndGet() % stg.sts.length;  //从worker组中分配
        return stg.sts[i];
    }

}
