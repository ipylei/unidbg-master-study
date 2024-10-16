package com.kanxue.chap02;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.listener.TraceWriteListener;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DecryptSo {
    public static Map<Long, byte[]> modifyMap = new HashMap<>();
    private static AndroidEmulator createARMEmulator() {
        return AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.sun.jna")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
    }
    public static void main(String[] args) throws IOException {
        String sopath="unidbg-android/src/test/resources/example_binaries/test/obftest/obf.so";
        sopath="unidbg-android/src/test/resources/example_binaries/test/obftest/libcrypt.so";
        sopath="unidbg-android/src/test/resources/example_binaries/test/obftest/libcrack.so";
        AndroidEmulator emulator = createARMEmulator();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);
        VM vm = emulator.createDalvikVM();
        vm.setVerbose(true);


       // emulator.traceCode();
/*
        emulator.getBackend().hook_add_new(new WriteHook() {
            @Override
            public void hook(Backend backend, long address, int size, long value, Object user) {
                System.out.println(Long.toHexString(address)+"---"+size+"---"+value);
            }

            @Override
            public void onAttach(UnHook unHook) {
                System.out.println("attach");
            }

            @Override
            public void detach() {
                System.out.println("detach");
            }
        },0,Long.MAX_VALUE,null);
*/

        emulator.traceWrite(0, Long.MAX_VALUE, new TraceWriteListener() {
            @Override
            public boolean onWrite(Emulator<?> emulator, long address, int size, long value) {
                System.out.println(Long.toHexString(address) + "---" + size + "---" + value);
                byte[] bytes = long2Bytes(value, size);
                modifyMap.put(address, bytes);
                return false;
            }
        });
        DalvikModule dm = vm.loadLibrary(new File(sopath), true);

        long start = dm.getModule().base;
        long end = start + dm.getModule().size;

        byte[] content = readFile(new File(sopath));
        modifyMap.forEach((aLong, bytes) -> {
            if (aLong >= start && aLong <= end) {
                long offset = aLong - start - 0x1000;
                for (int i = 0; i < bytes.length; i++) {
                    content[(int) (offset + i)] = bytes[i];
                }
            }

        });
        try (FileOutputStream out = new FileOutputStream(new File(sopath+".fix"))) {
            out.write(content);
        }
    }
    public static byte[] long2Bytes(long num, int size) {
        byte[] byteNum = new byte[size];
        for (int ix = 0; ix < byteNum.length; ++ix) {
            int offset = size * 8 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }
    public static byte[] readFile(File file) throws IOException {
        long len = file.length();
        byte[] bytes = new byte[(int)len];
        try (FileInputStream in = new FileInputStream(file)) {
            in.read(bytes);
        }
        return bytes;
    }
}
