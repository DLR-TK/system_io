package com.tk.system.io.rpcdemo.utils;


import com.tk.system.io.rpcdemo.rpc.protocol.MyContent;
import com.tk.system.io.rpcdemo.rpc.protocol.MyHeader;

public class PackageMsg {

    private MyHeader header;
    private MyContent content;

    public PackageMsg(MyHeader header, MyContent content) {
        this.header = header;
        this.content = content;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }
}
