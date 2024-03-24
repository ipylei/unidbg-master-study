package com.dta.lesson5;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookEntryInfo;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.hookzz.WrapCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmClass;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import com.sun.jna.Pointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneMode;
import unicorn.Unicorn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity() {
        emulator = AndroidEmulatorBuilder
                //.for32Bit()
                .for64Bit()
                //.setRootDir(new File("target/rootfs/default"))
                .setProcessName("")
                .addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();

        //new JniGraphics(emulator, vm).register(memory);
        //memory.disableCallInitFunction();

        //memory.disableCallInitFunction();
        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson5/libnative-lib.so"), true);
        module = dalvikModule.getModule();
        vm.callJNI_OnLoad(emulator, module);
        //或者
        //dalvikModule.callJNI_OnLoad(emulator);

        //emulator.traceCode();
    }

    public void callAdd() {
        //emulator.attach().addBreakPoint(module, 0x9490);

        DvmObject obj = ProxyDvmObject.createObject(vm, this);
        int result = obj.callJniMethodInt(emulator, "add(II)I", 100, 200);
        System.out.println("[symble] Call the so add function result is ==> " + result);

        //DvmObject obj2 = vm.resolveClass("com/dta/lesson5/MainActivity").newObject(null);
        //int result2 = obj2.callJniMethodInt(emulator, "add(II)I", 1, 2);
        //System.out.println("[symble] Call the so add function result is ==> " + result2);
    }

    public void hook() {
        HookZz hook = HookZz.getInstance(emulator);
        //hook.replace(module.base + 0x3DC + 1, new ReplaceCallback() {

        hook.replace(module.base + 0xF18C, new ReplaceCallback() {
            //hook.replace(module.base + 0xF28C, new ReplaceCallback() {  //随便乱填地址是不行的
            //hook.replace(module.findSymbolByName("Java_com_dta_lesson5_MainActivity_add"), new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("3================>");
                System.out.println(String.format("R2: %d, R3: %d", context.getIntArg(2), context.getIntArg(3)));
                //emulator.getBackend().reg_write(Unicorn.UC_ARM_REG_R3,5);

                //System.out.println(emulator.getContext() == context);  //false
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                //emulator.getBackend().reg_write(Unicorn.UC_ARM_REG_R0,10);
                super.postCall(emulator, context);
                System.out.println("4================>");
            }
        }, true);

        //hook.wrap(module.base + 0xF18C, new WrapCallback<RegisterContext>() {
        //    @Override
        //    public void preCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
        //        System.out.println("1================>");
        //    }
        //
        //    @Override
        //    public void postCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
        //        System.out.println("2================>");
        //        super.postCall(emulator, ctx, info);
        //    }
        //});
    }

    public void patch() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x3E8);
        byte[] code = new byte[]{(byte) 0xd0, 0x1a};
        pointer.write(code);
    }

    public void patch2() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x3E8);
        Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb);
        String s = "subs r0, r2, r3";
        byte[] machineCode = keystone.assemble(s).getMachineCode();
        //byte[] code = ;
        pointer.write(machineCode);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");

        mainActivity.hook();
        //mainActivity.patch(); //直接使用机器码
        //mainActivity.patch2();  //使用汇编代码=>机器码

        mainActivity.callAdd();
    }

}
