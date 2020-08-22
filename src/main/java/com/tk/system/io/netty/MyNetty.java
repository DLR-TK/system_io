package com.tk.system.io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.net.InetSocketAddress;

public class MyNetty {

    @Test
    public void myBytebuf() {

//        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(8, 20);
        //pool
//        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        print(byteBuf);

        byteBuf.writeBytes(new byte[]{1,2,3,4});
        print(byteBuf);
//
//        byteBuf.readBytes(4);
//        print(byteBuf);

        byte[] bytes = new byte[4];
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        print(byteBuf);
    }

    private void print(ByteBuf byteBuf) {
        System.out.println("byteBuf.isReadable()  : " + byteBuf.isReadable());
        System.out.println("byteBuf.readerIndex()  : " + byteBuf.readerIndex());
        System.out.println("byteBuf.readableBytes()  : " + byteBuf.readableBytes());
        System.out.println("byteBuf.isWritable()  : " + byteBuf.isWritable());
        System.out.println("byteBuf.writerIndex()  : " + byteBuf.writerIndex());
        System.out.println("byteBuf.writableBytes()  : " + byteBuf.writableBytes());
        System.out.println("byteBuf.capacity()  : " + byteBuf.capacity());
        System.out.println("byteBuf.maxCapacity()  : " + byteBuf.maxCapacity());
        System.out.println("byteBuf.isDirect()  : " + byteBuf.isDirect());
        System.out.println("=====================================================");
    }

    @Test
    public void clientMode() throws Exception {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioSocketChannel client = new NioSocketChannel();
        thread.register(client);//epoll_ctl(5,add,3)

        //响应式
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new InHandle());

        //异步
        ChannelFuture connect = client.connect(new InetSocketAddress("192.168.150.1", 9999));
        ChannelFuture sync = connect.sync();

        ByteBuf byteBuf = Unpooled.copiedBuffer("hello tk".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();

        sync.channel().closeFuture().sync();
        System.out.println("client closed...");
    }

    @Test
    public void nettyClient() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();

        ChannelFuture connect = bootstrap.group(group)
                .channel(NioSocketChannel.class)
//                .service(new ChannelInit())
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new InHandle());
                    }
                })
                .connect(new InetSocketAddress("192.168.150.1", 9999));

        Channel client = connect.sync().channel();

        ByteBuf byteBuf = Unpooled.copiedBuffer("hello tk".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();

        client.closeFuture().sync();
    }

    @Test
    public void serverMode() throws Exception {
        NioServerSocketChannel server = new NioServerSocketChannel();
        NioEventLoopGroup thread = new NioEventLoopGroup(1);

        ChannelPipeline pipeline = server.pipeline();
        pipeline.addLast(new AcceptHandle(thread, new ChannelInit()));
        thread.register(server);
        ChannelFuture bind = server.bind(new InetSocketAddress("192.168.150.1", 9999));

        bind.sync().channel().closeFuture().sync();
    }

    @Test
    public void nettyServer() throws InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture bind = bootstrap.group(group, group)
                .channel(NioServerSocketChannel.class)
//                .service(new AcceptHandle(group, new ChannelInit()))
//                .childHandler(new ChannelInit())
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new InHandle());
                    }
                })
                .bind(new InetSocketAddress("192.168.150.1", 9999));

        bind.sync().channel().closeFuture().sync();
    }

}

class AcceptHandle extends ChannelInboundHandlerAdapter {

    private final EventLoopGroup selector;
    private final ChannelHandler handler;

    public AcceptHandle(EventLoopGroup thread, ChannelHandler channelInit) {
        this.selector = thread;
        this.handler = channelInit;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server register...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        SocketChannel client = (SocketChannel) msg;
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(handler);
        selector.register(client); //注意顺序
    }
}

/**
 *  创建单例
 */
@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new InHandle());
        ctx.pipeline().remove(this);
//        System.out.println(ctx.pipeline().equals(ctx.channel().pipeline())); //true
    }
}

/**
 * 用户自己实现
 */
class InHandle extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
//        CharSequence str = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
        CharSequence str = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(str);
        ctx.writeAndFlush(buf);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client  register...");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("active...");
    }
}