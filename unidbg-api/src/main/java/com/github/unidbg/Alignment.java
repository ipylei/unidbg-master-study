package com.github.unidbg;

public class Alignment {

    public final long address;
    public final long size;
    public long begin;
    public long dataSize;

    public Alignment(long address, long size) {
        this.address = address;            //页对齐后，所占页的起始地址
        this.size = size;                  //页对齐后，占几页的大小
    }
}
