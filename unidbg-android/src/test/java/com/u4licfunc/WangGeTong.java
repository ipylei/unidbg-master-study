package com.u4licfunc;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class WangGeTong extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmObject BlackBox;
    private final VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u4_5_wanggetong\\wanggetong.apk";
    public WangGeTong() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("msec", true);
        BlackBox = vm.resolveClass("com.autohome.mainlib.common.util.BlackBox").newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    public static void main(String[] args) {
        WangGeTong wgt = new WangGeTong();
    }
}
