package com.tk.system.io.rpcdemo.service;

public class MyFly implements Fly {

    @Override
    public void xxoo(String msg) {
        System.out.println("MyFly :: server get client arg: " + msg);
    }
}
