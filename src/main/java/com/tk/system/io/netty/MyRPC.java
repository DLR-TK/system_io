package com.tk.system.io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2020/08/17 TK
 */
public class MyRPC {

    /**
     * 服务端
     */
    @Test
    public void startServer() {
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
                                .addLast(new ServerRequestHandler());
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

        new Thread(() -> {
            startServer();
        }).start();

        System.out.println("server started...");

        AtomicInteger num = new AtomicInteger(0);
        int size = 20;
        Runnable runnable = () -> {
            Car car = proxyGet(Car.class); //动态代理
            String arg = "hello tk" + num.incrementAndGet();
            String res = car.move(arg);
            System.out.println(res + " ---src arg: " + arg);
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

    /**
     * 动态代理
     */
    private <T>T proxyGet(Class<T> interfaceInfo) {

        //获取类加载器
        ClassLoader classLoader = interfaceInfo.getClassLoader();
        //获取接口类
        Class<?>[] methodInfo = {interfaceInfo};

        return (T) Proxy.newProxyInstance(classLoader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                //设计consumer对provider的调用过程
                //1.调用 服务、方法、参数 --> 封装成message
                String name = interfaceInfo.getName();//服务名称
                String methodName = method.getName();//方法名称
                Class<?>[] parameterTypes = method.getParameterTypes();//参数类型
                MyContent content = new MyContent();

                content.setName(name);
                content.setMethodName(methodName);
                content.setParameterType(parameterTypes);
                content.setArgs(args);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oopt = new ObjectOutputStream(out);
                oopt.writeObject(content);
                byte[] msgBody = out.toByteArray();
//                byte[] msgBody = SerDerUtil.ser(content);
                
                //2.request + message  本地缓存
                MyHeader header = createHeader(msgBody);

                out.reset();
                oopt = new ObjectOutputStream(out);
                oopt.writeObject(header);
                byte[] msgHead = out.toByteArray();
//                System.out.println("msgHead: " + msgHead.length);
                //3.连接池 获取连接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel channel = factory.getClient(new InetSocketAddress("192.168.150.1", 9999));

                //4.发送 -> 走 io  out -> 走netty
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHead.length + msgBody.length);

//                CountDownLatch countDownLatch = new CountDownLatch(1);
                long requestID = header.getRequestID();
                CompletableFuture<String> res = new CompletableFuture<>();
//                ResponseMappingCallback.addCallback(requestID, new Runnable() {
//                    @Override
//                    public void run() {
//                        countDownLatch.countDown();
//                    }
//                });
                ResponseMappingCallback.addCallback(requestID, res);
                byteBuf.writeBytes(msgHead).writeBytes(msgBody);
                ChannelFuture future = channel.writeAndFlush(byteBuf);
                future.sync();//只是out的sync
//                channel.closeFuture().sync();
                //5.如果IO回来 执行下方逻辑，(门闩 CountDownLatch 阻塞， 返回值 CompletableFuture )
//                countDownLatch.await();
                return res.get(); //阻塞
            }
        });
    }

    private static MyHeader createHeader(byte[] msgBody) {
        MyHeader header = new MyHeader();
        int flag = 0x14141414;
        int dataLength = msgBody.length;
        long requestID = Math.abs(UUID.randomUUID().getLeastSignificantBits());

        header.setFlag(flag);
        header.setRequestID(requestID);
        header.setDataLength(dataLength);
        return header;
    }
}

/**
 * 信息头 （协议）
 *
 * flag协议值
 * UUID：requestID
 * data length
 */
class MyHeader implements Serializable {

    private int flag;
    private long requestID;
    private long dataLength;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }
}

/**
 * 信息体（内容）
 */
class MyContent implements Serializable {

    private String name;
    private String methodName;
    private Class<?>[] parameterType;
    private Object[] args;
    private String res;

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterType() {
        return parameterType;
    }

    public void setParameterType(Class<?>[] parameterType) {
        this.parameterType = parameterType;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}

/**
 * 连接池
 */
class ClientPool {

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

/**
 * client 工厂 单例
 */
class ClientFactory {

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

    //一个consumer 可以连接很多的provider，每一个provider都有自己的pool  K,V
    ConcurrentHashMap<InetSocketAddress, ClientPool> outBoxs = new ConcurrentHashMap<>();

    //获取连接
    public synchronized NioSocketChannel getClient(InetSocketAddress address) {

        ClientPool clientPool = outBoxs.get(address);
        if (clientPool == null) {
            outBoxs.putIfAbsent(address, new ClientPool(poolSize));
            clientPool = outBoxs.get(address);
        }

        int i = random.nextInt(poolSize);
        if (clientPool.getClients()[i] != null && clientPool.getClients()[i].isActive()) {
            return clientPool.getClients()[i];
        }

        synchronized (clientPool.getLocks()[i]) {
            return clientPool.getClients()[i] = create(address);
        }
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

/**
 * 客户端响应处理函数
 */
class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        PackageMsg pm = (PackageMsg) msg;
        ResponseMappingCallback.runCallback(pm);
    }
}

/**
 *  响应处理回调函数
 */
class ResponseMappingCallback {
//    private static ConcurrentHashMap<Long, Runnable> mapping = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void addCallback(long requestID, CompletableFuture cb) {
        mapping.putIfAbsent(requestID, cb);
    }

    public static void runCallback(PackageMsg pm) {
        CompletableFuture cf = mapping.get(pm.getHeader().getRequestID());
        cf.complete(pm.getContent().getRes());
        removeCB(pm.getHeader().getRequestID());
    }

    private static void removeCB(long requestID) {
        mapping.remove(requestID);
    }
}

/**
 * 解码器
 * 新老buf拼接  { (bufHead1,bufBody1) , (bufHead2,bufBody2) , (bufHead3,buf }   第一次读 只处理1,2
 *              { Body3) , (bufHead4,bufBody4) , (bufHead5,bufBody5)}           第二次读 拼接3 处理3,4,5
 *              拼接    bufHead3,bufBody3
 *
 *  父类有拼接逻辑 执行顺序  老buf拼新buf -> decode() -> (遍历out执行handle) -> 剩余buf
 *                           1.老buf拼新buf ==> channelRead() -> cumulator.cumulate() 拼接逻辑
 *                          first = cumulation == null; first ? Unpooled.EMPTY_BUFFER : cumulation
 *                          2.decode()
 *                          channelRead() -> callDecode() -> decodeRemovalReentryProtection() -> decode()
 *                          3.剩余buf
 *                          channelRead() -> discardSomeReadBytes() 丢弃已读buf
 */

class MyDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

//        System.out.println("channel start : " + buf.readableBytes());
        while (buf.readableBytes() >= 104) { //int 100    long 104
            //读取信息头
            byte[] bytes = new byte[104];
            buf.getBytes(buf.readerIndex(), bytes); //不会移动指针
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader header = (MyHeader) oin.readObject();
//            int dataLength = ;
//            System.out.println("server RequestID : " + header.getRequestID());
//            System.out.println("dataLength : " + dataLength);

            //获取信息体
            if (buf.readableBytes() >= header.getDataLength()) {
                //移动指针
                buf.readBytes(104);
                byte[] data = new byte[(int) header.getDataLength()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);

                if (header.getFlag() == 0x14141414) {
                    MyContent content = (MyContent) doin.readObject();
                    //添加结果至out
                    out.add(new PackageMsg(header, content));
                } else if (header.getFlag() == 0x14141424) {
                    MyContent content = (MyContent) doin.readObject();
                    out.add(new PackageMsg(header, content));
                }
            } else {
                break;
            }
        }
    }
}

/**
 * 服务端处理器
 */
class ServerRequestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        PackageMsg pm = (PackageMsg) msg;
//        System.out.println("server handle: " + pm.getContent().getArgs()[0]);

        //处理完成返回给客户端 注意事项
        //RPC 需要requestID
        //通信协议 来 flag 0x14141414 出 0x14141424
        //创建新的header和context(构建返回信息res)
        String ioThreadName = Thread.currentThread().getName();
        //1.直接在当前方法处理IO和业务 返回
        //2.自定义线程池
        //3.使用netty的eventLoop处理业务返回
//        ctx.executor().execute(() -> {
        //使用其他eventLoop处理业务返回
        ctx.executor().parent().next().execute(() -> {
            String execThreadName = Thread.currentThread().getName();
            MyContent content = new MyContent();

            //返回信息 io线程名称 + exec线程名称 + 返回参数
            String s = "ioThreadName : " + ioThreadName
                    + " execThreadName : " + execThreadName
                    + " from " + pm.getContent().getArgs()[0];
//            System.out.println(s);
            content.setRes(s);
            byte[] msgBody = SerDerUtil.ser(content);

            MyHeader header = new MyHeader();
            header.setFlag(0x14141424);
            header.setRequestID(pm.getHeader().getRequestID());
            header.setDataLength(msgBody.length);
            byte[] msgHead = SerDerUtil.ser(header);
//            System.out.println("msgBody.length" + msgBody.length);
            ByteBuf sendBuf = PooledByteBufAllocator
                    .DEFAULT.directBuffer(msgHead.length + msgBody.length);
            sendBuf.writeBytes(msgHead).writeBytes(msgBody);
            ctx.writeAndFlush(sendBuf);
        });
    }
}

interface Car{
    String move(String msg);
}

class MyCar implements Car {

    @Override
    public String move(String msg) {
        System.out.println("server get client arg: " + msg);
        return "server res :" + msg;
    }
}

interface Fly{
    void xxoo(String msg);
}
