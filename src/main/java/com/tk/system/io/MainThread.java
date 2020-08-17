package com.tk.system.io;

public class MainThread {

    public static void main(String[] args) {

        //创建一个或多个IO thread
//        SelectorThreadGroup stg = new SelectorThreadGroup(3);


        //区分监听与连接
        SelectorThreadGroup boss = new SelectorThreadGroup(3);
        SelectorThreadGroup worker = new SelectorThreadGroup(3);

        //将监听（9999）的Server注册到某个selector上
        boss.setWorker(worker);
        boss.bind(9999);
        boss.bind(8888);
        boss.bind(7777);
        boss.bind(6666);
    }
}
