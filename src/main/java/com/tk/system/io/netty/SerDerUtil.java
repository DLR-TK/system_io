//package com.tk.system.io.netty;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//
//public class SerDerUtil {
//
//    private static ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//    public synchronized static byte[] ser(Object o) {
//        out.reset();
//        ObjectOutputStream oopt = null;
//        byte[] msgBody = null;
//        try {
//            oopt = new ObjectOutputStream(out);
//            oopt.writeObject(o);
//            msgBody = out.toByteArray();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return msgBody;
//    }
//}
