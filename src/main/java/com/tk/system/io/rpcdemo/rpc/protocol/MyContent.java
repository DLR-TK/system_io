package com.tk.system.io.rpcdemo.rpc.protocol;

import java.io.Serializable;

/**
 * 信息体（内容）
 */
public class MyContent implements Serializable {

    private String name;
    private String methodName;
    private Class<?>[] parameterType;
    private Object[] args;
    private Object res;

    public Object getRes() {
        return res;
    }

    public void setRes(Object res) {
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
