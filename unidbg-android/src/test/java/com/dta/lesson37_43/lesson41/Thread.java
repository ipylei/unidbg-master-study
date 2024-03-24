package com.dta.lesson37_43.lesson41;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class Thread {
    public static void main(String[] args) throws IOException {
        Thread test = new Thread();
        test.test();
        test.destroy();
    }

    private void destroy() {
        IOUtils.close(emulator);
    }

    private final AndroidEmulator emulator;
    private final Module module;

    private Thread() {
        final File executable = new File("unidbg-android/src/test/java/com/dta/lesson37_43/lesson37/thread_39");
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(false))
                .build();
        Memory memory = emulator.getMemory();

        //emulator.getBackend().registerEmuCountHook(100); // 设置执行多少条指令切换一次线程
        emulator.getSyscallHandler().setVerbose(false);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        AndroidResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        module = emulator.loadLibrary(executable, true);
    }

    private void test() {
        int code = module.callEntry(emulator);
        System.err.println("exit code: " + code + ", backend=" + emulator.getBackend());
    }
}
