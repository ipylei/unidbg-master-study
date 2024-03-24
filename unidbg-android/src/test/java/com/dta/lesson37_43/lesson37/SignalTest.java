package com.dta.lesson37_43.lesson37;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class SignalTest {


    private final AndroidEmulator emulator;
    private final Module module;

    private SignalTest() {
        final File executable = new File("unidbg-android/src/test/java/com/dta/lesson37_43/lesson37/signal_37");
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        emulator.getSyscallHandler().setVerbose(false);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        AndroidResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        module = emulator.loadLibrary(executable, true);
    }

    public static void main(String[] args) throws IOException {
        SignalTest test = new SignalTest();
        test.test();
        test.destroy();
    }


    private void test() {
        //emulator.emulateSignal(17);
        //emulator.traceCode();
        int code = module.callEntry(emulator);
        System.err.println("exit code: " + code + ", backend=" + emulator.getBackend());
    }

    private void destroy() {
        IOUtils.close(emulator);
    }
}
