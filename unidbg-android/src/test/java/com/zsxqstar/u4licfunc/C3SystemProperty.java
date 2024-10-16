package com.zsxqstar.u4licfunc;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.SystemPropertyHook;
import com.github.unidbg.linux.android.SystemPropertyProvider;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;


public class C3SystemProperty extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass MainActivity;
    private final VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u4libf_3_app-debug\\app-debug.apk";

    public C3SystemProperty() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);

        // 处理完毕
        //DalvikModule dm = vm.loadLibrary("getprop", true);
        //MainActivity = vm.resolveClass("com.example.getprop.MainActivity");
        //dm.callJNI_OnLoad(emulator);

        // hook system property get
        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator);
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String key) {
                System.out.println(">>> " + key);
                switch (key) {
                    case "ro.build.id": {
                        return "get id";
                    }
                    case "ro.build.version.sdk": {
                        return "get sdk";
                    }
                    //省略其他...
                }
                return null;
            }
        });
        //这一步，不添加监听器拦截就不会生效，绝不能漏。
        memory.addHookListener(systemPropertyHook);

        // 处理完毕
        DalvikModule dm = vm.loadLibrary("getprop", true);
        MainActivity = vm.resolveClass("com.example.getprop.MainActivity");
        dm.callJNI_OnLoad(emulator);

        //emulator.traceCode();
    }

    public String call() {
        return MainActivity.newObject(null).callJniMethodObject(emulator, "stringFromJNI").getValue().toString();
    }

    public static void main(String[] args) {
        C3SystemProperty getProp = new C3SystemProperty();
        getProp.call();
    }
}
