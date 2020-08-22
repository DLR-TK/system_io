package com.tk.system.io.rpcdemo.rpc.transport;

import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 连接池
 */
public class ClientPool {

    private NioSocketChannel[] clients;
    private Object[] locks;

    public ClientPool(int size) {
        clients = new NioSocketChannel[size]; //只初始化
        locks = new Object[size];
        for (int i = 0; i < size; i++) { //初始化锁
            locks[i] = new Object();
        }
    }

    public NioSocketChannel[] getClients() {
        return clients;
    }

    public void setClients(NioSocketChannel[] clients) {
        this.clients = clients;
    }

    public Object[] getLocks() {
        return locks;
    }

    public void setLocks(Object[] locks) {
        this.locks = locks;
    }
}
