package com.tk.system.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class SelectorThread extends ThreadLocal<LinkedBlockingQueue<Channel>> implements Runnable{

    public Selector selector = null;
    public LinkedBlockingQueue<Channel> lbq = new LinkedBlockingQueue();
//    public LinkedBlockingQueue<Channel> lbq = get();//lbq  在接口或者类中是固定使用方式逻辑写死了。你需要是lbq每个线程持有自己的独立对象
    public SelectorThreadGroup stg;

    /**
     * ThreadLocal
     */
//    @Override
//    protected LinkedBlockingQueue<Channel> initialValue() {
//        return new LinkedBlockingQueue(); //丰富逻辑
//    }

    /**
     * 混杂模式 不区分boss与worker写法
     */
//    public SelectorThread(SelectorThreadGroup stg) {
//        try {
//            this.stg = stg;
//            this.selector = Selector.open();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public SelectorThread() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                //1.select()
                int num = selector.select();
                //2.selectedKeys()
                if (num > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandle(key);
                        } else if (key.isReadable()) {
                            readHandle(key);
                        } else if (key.isWritable()) {
                            writeHandle(key);
                        }
                    }
                }
                //3. task
                if (!lbq.isEmpty()) {
                    Channel channel = lbq.take();
                    if (channel instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) channel;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println(Thread.currentThread().getName() + "---register listen---");
                    } else if (channel instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                        System.out.println(Thread.currentThread().getName() + "---register client---" + client.getRemoteAddress());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeHandle(SelectionKey key) {

    }

    private void readHandle(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + "---readHandle---");

        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();

        while (true) {
            try {
                int num = client.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                } else if (num == 0) {
                    break;
                } else {//num < 0 客户端断开连接
                    System.out.println("client: " + client.getRemoteAddress() + "closed.....");
                    key.cancel();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptHandle(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + "---acceptHandle---");

        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            // choose and register
            stg.nextSelectorV3(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWorker(SelectorThreadGroup stg) {
        this.stg = stg;
    }
}
