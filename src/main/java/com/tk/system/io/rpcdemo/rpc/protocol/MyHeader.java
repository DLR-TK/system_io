package com.tk.system.io.rpcdemo.rpc.protocol;

import java.io.Serializable;
import java.util.UUID;

/**
 * 信息头 （协议）
 * <p>
 * flag协议值
 * UUID：requestID
 * data length
 */
public class MyHeader implements Serializable {

    private int flag;
    private long requestID;
    private int dataLength;

    public static MyHeader createHeader(byte[] msgBody) {
        MyHeader header = new MyHeader();
        int flag = 0x14141414;
        int dataLength = msgBody.length;
        long requestID = Math.abs(UUID.randomUUID().getLeastSignificantBits());

        header.setFlag(flag);
        header.setRequestID(requestID);
        header.setDataLength(dataLength);
        return header;
    }

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

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }
}
