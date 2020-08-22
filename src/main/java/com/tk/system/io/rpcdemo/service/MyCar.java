package com.tk.system.io.rpcdemo.service;

public class MyCar implements Car {

    @Override
    public String move(String msg) {
        System.out.println("server get client arg: " + msg);
        return "MyCar :: server res :" + msg;
    }
}
