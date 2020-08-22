package com.tk.system.io.rpcdemo.rpc.transport;

import com.tk.system.io.rpcdemo.rpc.ResponseMappingCallback;
import com.tk.system.io.rpcdemo.rpc.protocol.MyContent;
import com.tk.system.io.rpcdemo.rpc.protocol.MyHeader;
import com.tk.system.io.rpcdemo.utils.SerDerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * client 工厂 单例
 */
public class ClientFactory {

    private int poolSize = 10;
    private Random random = new Random();
    private NioEventLoopGroup group;
    private static final ClientFactory factory;

    static {
        factory = new ClientFactory();
    }

    private ClientFactory() {}

    public static ClientFactory getFactory() {
        return factory;
    }

    public static CompletableFuture<Object> transport(MyContent content) {

        byte[] msgBody = SerDerUtil.ser(content);
        MyHeader header = MyHeader.createHeader(msgBody);
        byte[] msgHead = SerDerUtil.ser(header);
        System.out.println("msgHead: " + msgHead.length);

        //连接池 获取连接\
        NioSocketChannel channel = factory.getClient(new InetSocketAddress("192.168.150.1", 9999));

        //发送 -> 走 io  out -> 走netty
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHead.length + msgBody.length);

        long requestID = header.getRequestID();
        CompletableFuture<Object> res = new CompletableFuture<>();
        ResponseMappingCallback.addCallback(requestID, res);
        byteBuf.writeBytes(msgHead).writeBytes(msgBody);
        channel.writeAndFlush(byteBuf);

        return res;
    }

    //一个consumer 可以连接很多的provider，每一个provider都有自己的pool  K,V
    ConcurrentHashMap<InetSocketAddress, ClientPool> outBoxs = new ConcurrentHashMap<>();

    //获取连接
    public NioSocketChannel getClient(InetSocketAddress address) {

        ClientPool clientPool = outBoxs.get(address);
        if (clientPool == null) {
            synchronized (outBoxs) {
                if (clientPool == null) {
                    outBoxs.putIfAbsent(address, new ClientPool(poolSize));
                    clientPool = outBoxs.get(address);
                }
            }
        }

        int i = random.nextInt(poolSize);
        if (clientPool.getClients()[i] != null && clientPool.getClients()[i].isActive()) {
            return clientPool.getClients()[i];
        } else {
            synchronized (clientPool.getLocks()[i]) {
                if (clientPool.getClients()[i] == null || !clientPool.getClients()[i].isActive()) {
                    clientPool.getClients()[i] = create(address);
                }
            }
        }
        return clientPool.getClients()[i];
    }

    //netty客户端
    private NioSocketChannel create(InetSocketAddress address) {

        group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();

        ChannelFuture connect = bs.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MyDecode()).addLast(new ClientHandler()); //决定给谁
                    }
                }).connect(address);

        try {
            NioSocketChannel client = (NioSocketChannel) connect.sync().channel();
            return client;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
