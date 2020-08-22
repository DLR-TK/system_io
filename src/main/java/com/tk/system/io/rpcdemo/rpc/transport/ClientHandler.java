package com.tk.system.io.rpcdemo.rpc.transport;

import com.tk.system.io.rpcdemo.rpc.ResponseMappingCallback;
import com.tk.system.io.rpcdemo.utils.PackageMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 客户端响应处理函数
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        PackageMsg pm = (PackageMsg) msg;
        ResponseMappingCallback.runCallback(pm);
    }
}
