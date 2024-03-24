package com.github.unidbg.memory;

import com.github.unidbg.Emulator;
import com.github.unidbg.spi.LibraryFile;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MemRegion implements Comparable<MemRegion> {

    public final long virtualAddress;
    public final long begin;
    public final long end;
    public final int perms;
    private final LibraryFile libraryFile;
    public final long offset;

    public static MemRegion create(long begin, int size, int perms, final String name) {
        return new MemRegion(begin, begin, begin + size, perms, new LibraryFile() {
            @Override
            public String getName() {
                return name;
            }
            @Override
            public String getMapRegionName() {
                return name;
            }
            @Override
            public LibraryFile resolveLibrary(Emulator<?> emulator, String soName) {
                throw new UnsupportedOperationException();
            }
            @Override
            public ByteBuffer mapBuffer() {
                throw new UnsupportedOperationException();
            }
            @Override
            public String getPath() {
                return name;
            }
            @Override
            public long getFileSize() {
                throw new UnsupportedOperationException();
            }
        }, 0);
    }

    public MemRegion(long virtualAddress, long begin, long end, int perms, LibraryFile libraryFile, long offset) {
        this.virtualAddress = virtualAddress;  //在内存中的起始地址
        this.begin = begin;                    //对齐后的起始页地址
        this.end = end;                        //对齐后的结束页地址
        this.perms = perms;                    //操作权限
        this.libraryFile = libraryFile;        //若该内存块存放了so文件，则对应文件名
        this.offset = offset;                  //在so文件中的虚拟地址，即段偏移
    }

    public String getName() {
        return libraryFile.getMapRegionName();
    }

    public byte[] readLibrary() throws IOException {
        ByteBuffer buffer = libraryFile.mapBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }

    @Override
    public int compareTo(MemRegion o) {
        return Long.compare(begin, o.begin);
    }
}
