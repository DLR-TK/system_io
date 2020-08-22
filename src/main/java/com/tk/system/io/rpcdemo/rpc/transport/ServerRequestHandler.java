package com.tk.system.io.rpcdemo.rpc.transport;

import com.tk.system.io.rpcdemo.rpc.Dispatcher;
import com.tk.system.io.rpcdemo.rpc.protocol.MyContent;
import com.tk.system.io.rpcdemo.rpc.protocol.MyHeader;
import com.tk.system.io.rpcdemo.utils.PackageMsg;
import com.tk.system.io.rpcdemo.utils.SerDerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

/**
 * 服务端处理器
 */
public class ServerRequestHandler extends ChannelInboundHandlerAdapter {
    private Dispatcher dispatcher;

    public ServerRequestHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

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

            //调用实现方法
            String name = pm.getContent().getName();
            String methodName = pm.getContent().getMethodName();
            Class<?>[] parameterType = pm.getContent().getParameterType();
            Object[] args = pm.getContent().getArgs();

            Object o = dispatcher.get(name);
            Class<?> clazz = o.getClass();

            Object res = null;
            try {
                Method method = clazz.getMethod(methodName, parameterType);
                res = method.invoke(o, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (res == null) {
                return;
            }

            MyContent content = new MyContent();
            content.setRes(res);
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