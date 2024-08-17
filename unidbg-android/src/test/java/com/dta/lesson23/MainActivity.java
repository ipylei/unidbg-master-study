package com.dta.lesson23;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookEntryInfo;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.hookzz.HookZzArm32RegisterContextImpl;
import com.github.unidbg.hook.hookzz.WrapCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import unicorn.ArmConst;

import java.io.File;
import java.util.List;

import static com.dta.lesson23.AesKeyFinder.readFuncFromIDA;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class MainActivity {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson23/libtest-lib.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }

    public void callAes() {
        //emulator.traceCode();
        DvmObject obj2 = ProxyDvmObject.createObject(vm, this);
        DvmObject obj = vm.resolveClass("com/dta/lesson2/MainActivity").newObject(null);
        obj.callJniMethod(emulator, "aes(II)V", 2, 3);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();

        mainActivity.hookZz();
        //mainActivity.consoleDebugger();
        //mainActivity.keyFinder();

        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
        mainActivity.callAes();

        //Dobby
        //HookZz
    }


    private void hookZz() {
        HookZz hookZz = HookZz.getInstance(emulator);
        //hookZz.enable_arm_arm64_b_branch();

        //【*】相当于Frida中的Intercept.attach
       /* hookZz.wrap(module.base + 0x20ad, new WrapCallback<HookZzArm32RegisterContextImpl>() {
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContextImpl ctx, HookEntryInfo info) {
                UnidbgPointer arg0 = ctx.getPointerArg(0);
                UnidbgPointer arg1 = ctx.getPointerArg(1);
                System.out.println("0x20ad_OnEnter: arg0=>" + arg0.getString(0));
                System.out.println("0x20ad_OnEnter: arg1=>" + arg1.getInt(0));
                Inspector.inspect(arg1.getByteArray(0, 200), "0x20ad_OnEnter_arg1");
                ctx.push(arg1); //类似于frida Intercept.attach中的this.xxx=xxx;
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContextImpl ctx, HookEntryInfo info) {
                UnidbgPointer arg1 = ctx.pop();
                Inspector.inspect(arg1.getByteArray(0, 200), "0x20ad_OnLeave_arg1");
                super.postCall(emulator, ctx, info);
            }
        });*/

        //【*】相当于Frida中的Intercept.replace
        hookZz.replace(module.base + 0x20ad, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                //emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0,1);
                System.out.println("hello world before -----------------------");
                //TODO 直接跳转到末尾，(此时LR寄存器为返回地址)

                UnidbgPointer arg0 = context.getPointerArg(0);
                UnidbgPointer arg1 = context.getPointerArg(1);
                Inspector.inspect(arg0.getByteArray(0, 200), "arg0");
                Inspector.inspect(arg1.getByteArray(0, 200), "arg1");
                context.push(arg1);
                context.push(arg0);

                return HookStatus.LR(emulator, 2);
                //return super.onCall(emulator, context, context.getLR());
                //return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                System.out.println("hello world after -----------------------");

                UnidbgPointer arg0 = context.pop();
                UnidbgPointer arg1 = context.pop();
                Inspector.inspect(arg0.getByteArray(0, 200), "arg00");
                Inspector.inspect(arg1.getByteArray(0, 200), "arg11");
                super.postCall(emulator, context);
            }
        },true);

/*        Dobby dobby = Dobby.getInstance(emulator);
        dobby.replace(module.base + 0x20ad, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                //HookStatus.RET(emulator,originFunction);
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                super.postCall(emulator, context);
            }
        },true);*/

/*        IxHook ixHook = XHookImpl.getInstance(emulator);
        ixHook.register("libtest-lib.so", "_Z17aes_key_expansionPhS_", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                super.postCall(emulator, context);
            }
        });
        ixHook.refresh();*/

    }

    private void consoleDebugger() {
        //emulator.attach().addBreakPoint(module.base + 0x20ad);

        emulator.attach().addBreakPoint(module.base + 0x20ad, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                //这里可以进行一些操作：如寄存器值修改
                return false;  //return false会断住，return true则不会断住
            }
        });
    }

    private void keyFinder() {
        List<String> funclist = readFuncFromIDA("unidbg-android/src/test/java/com/dta/lesson2/libtest-lib_functionlist_1636779320.txt");
        AesKeyFinder aesKeyFinder = new AesKeyFinder(emulator);
        aesKeyFinder.searchEveryFunction(module.base, funclist);
    }
}
