package com.zsxqstar.u4licfunc;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class C4GetSensor extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass MainActivity;
    private final VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u4libf_4_app-debug\\app-debug.apk";

    public C4GetSensor() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);

        // 使用 libandroid.so 的虚拟模块
        new AndroidModule(emulator, vm).register(memory);

        DalvikModule dm = vm.loadLibrary("getsensorinfo", true);

        MainActivity = vm.resolveClass("com.example.getsensorinfo.MainActivity");
        dm.callJNI_OnLoad(emulator);

        //emulator.traceCode();
    }

    public void call(){
        MainActivity.newObject(null).callJniMethod(emulator, "getsensorinfo");
    }

    public static void main(String[] args) {
        Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
        C4GetSensor sensor = new C4GetSensor();
        sensor.call();
    }
}