package com.u4licfunc;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import unicorn.Arm64Const;

import java.io.File;


public class C2Demo extends AbstractJni {
    private AndroidEmulator emulator;
    private VM vm;
    private DvmClass MainActivity;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u3_2\\demo.apk";

    public C2Demo() {
        emulator = AndroidEmulatorBuilder
                .for64Bit() //返回一个实例了!!!
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.example.demo")
                .build();


        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);

        DalvikModule dm = vm.loadLibrary("demo", true);

        MainActivity = vm.resolveClass("com.example.demo.MainActivity");

        dm.callJNI_OnLoad(emulator);
    }

    public String call() {
        return MainActivity.newObject(null).callJniMethodObject(emulator, "stringFromJNI").getValue().toString();
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    //补库函数(使用XHook)
    public void hookGetRusage(){
        IxHook xHook = XHookImpl.getInstance(emulator);
        xHook.register("libdemo.so", "getrusage", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                Pointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
                byte[] rusageContent = hexStringToByteArray("00000000000000009f4a0b00000000000000000000000000c5e10100000000009052010000000000000000000000000000000000000000000000000000000000255e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d02000000000000d300000000000000");
                for(int i = 0;i< rusageContent.length;i++){
                    rusage.setByte(i, rusageContent[i]);
                }
                //给寄存器设置值
                return HookStatus.LR(emulator, 0);
            }
        });
        xHook.refresh();
    }

    public static void main(String[] args) {
        C2Demo demo = new C2Demo();
        demo.hookGetRusage();
        System.out.println("ret: " + demo.call());
    }
}
