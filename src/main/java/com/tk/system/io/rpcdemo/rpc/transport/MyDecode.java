package com.tk.system.io.rpcdemo.rpc.transport;

import com.tk.system.io.rpcdemo.rpc.protocol.MyContent;
import com.tk.system.io.rpcdemo.rpc.protocol.MyHeader;
import com.tk.system.io.rpcdemo.utils.PackageMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

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

public class MyDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

//        System.out.println("channel start : " + buf.readableBytes());
        while (buf.readableBytes() >= 115) { //int 102    long 106
            //读取信息头
            byte[] bytes = new byte[115];
            buf.getBytes(buf.readerIndex(), bytes); //不会移动指针
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader header = (MyHeader) oin.readObject();

//            System.out.println("server RequestID : " + header.getRequestID());
//            System.out.println("dataLength : " + dataLength);

            //获取信息体
            if (buf.readableBytes() >= (header.getDataLength() + 115)) {
                //移动指针
                buf.readBytes(115);
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