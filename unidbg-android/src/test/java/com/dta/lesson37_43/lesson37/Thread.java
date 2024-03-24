package com.dta.lesson37_43.lesson37;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AbstractEmulator;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.FunctionCallListener;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.AndroidSyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
                .build();
        Memory memory = emulator.getMemory();
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
