package com.kanxue.chap02;


import capstone.api.Instruction;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.ModuleListener;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.*;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.listener.TraceCodeListener;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class T1 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public T1() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM();
        vm.setJni(this);

        vm.setVerbose(true);
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        //memory.disableCallInitFunction();
        //vm.addNotFoundClass("xxxxx");

        memory.addModuleListener(new ModuleListener() {
            @Override
            public void onLoaded(Emulator<?> emulator, Module module) {
                System.out.println("so: " + module.toString() + "loaded!");
                if (module.name.equals("libnative-lib.so")) {
                    System.out.println("start patch libnative-lib.so");
                    long baseaddr = module.base;
                    byte[] nop = {0x00, (byte) 0xbf, 0x00, (byte) 0xbf};
                    emulator.getBackend().mem_write(baseaddr + 0xc15c, nop);
                    emulator.getBackend().mem_write(baseaddr + 0xd316, nop);

                    emulator.getBackend().mem_write(baseaddr + 0xccb4, nop);
                    emulator.getBackend().mem_write(baseaddr + 0xd322, nop);

                    emulator.getBackend().mem_write(baseaddr + 0xd4dc, nop);
                }
            }
        });


        /*
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
        */

        DalvikModule dalvikModule = vm.loadLibrary(new File("E:\\Learning相应资料\\Learn_Spider相应资料\\网课-远\\P2\\第2章 NDK开发详解\\课件\\4月\\test2_libroysue.so"), true);
        //DalvikModule dalvikModule = vm.loadLibrary(new File("D:\\Learning\\Learn_Spider\\unidbg-master-study\\unidbg-android\\src\\test\\java\\com\\dta\\lesson23\\libtest-lib.so"), true);
        //DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson23/libtest-lib.so"), true);

        module = dalvikModule.getModule();
        vm.callJNI_OnLoad(emulator, module);
        System.out.println("load so file success!");

        //主动执行
        //module.callEntry();
        //module.callFunction();
    }

    public void trace() throws FileNotFoundException {
        //emulator.traceCode();
        //emulator.traceCode(module.base, module.base + module.size);

        // 保存的path
        String traceFile = "trace6.log";
        PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile));
        emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
        emulator.traceRead(module.base, module.base + module.size).setRedirect(traceStream);
        emulator.traceWrite(module.base, module.base + module.size).setRedirect(traceStream);

        //System.out.println(module.base);
        //System.out.println(module.size);
        //System.out.println(module.base + module.size);

        //emulator.traceCode(module.base, module.base + module.size, new TraceCodeListener() {
        //    @Override
        //    public void onInstruction(Emulator<?> emulator, long address, Instruction insn) {
        //        System.out.println("=================");
        //        //String soFileName = "test2_libroysue.so";
        //        //String currentSoFileName = emulator.getMemory().findModuleByAddress(address).name;
        //        //
        //        //if (currentSoFileName.equals(soFileName)) {
        //        //    // 打印 libnative-lib.so 的日志
        //        //    System.out.printf("Tracing: %s at address: 0x%08x\n", currentSoFileName, address);
        //        //}
        //    }
        //});
    }

    private void consoleDebugger() {
        //emulator.attach().addBreakPoint(module.base + 0x20ad);
        emulator.attach().addBreakPoint(module, 0x3C9E4 + 1);

        //emulator.attach().addBreakPoint(module.base + 0x20ad, new BreakPointCallback() {
        //    @Override
        //    public boolean onHit(Emulator<?> emulator, long address) {
        //        //这里可以进行一些操作：如寄存器值修改
        //        return false;  //return false会断住，return true则不会断住
        //    }
        //});

        //emulator.getMemory().pointer().setInt();
    }

    public void hook() {
        //MemoryBlock malloc = memory.malloc(16, true);
        //malloc.free();
        //UnidbgPointer pointer = memory.allocateStack(10);

        HookZz hookZz = HookZz.getInstance(emulator);
        //MemoryBlock blockin = emulator.getMemory().malloc(16,true);

       /* hookZz.wrap(module.base + 0x3C9E4 + 1, new WrapCallback<HookZzArm32RegisterContextImpl>() {
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContextImpl ctx, HookEntryInfo info) {
                UnidbgPointer arg0 = ctx.getPointerArg(0);
                Inspector.inspect(arg0.getByteArray(0, 128), "OnEnter_arg0");
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContextImpl ctx, HookEntryInfo info) {
                UnidbgPointer pointerArg = ctx.getPointerArg(0);
                Inspector.inspect(pointerArg.getByteArray(0, 128), "OnLeave_arg0");
                super.postCall(emulator, ctx, info);
            }
        });*/

        /*hookZz.replace(module.base + 0x3C9E4 + 1, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                UnidbgPointer arg0 = context.getPointerArg(0);

                Inspector.inspect(arg0.getByteArray(0, 128), "OnEnter_arg0");

                return super.onCall(emulator, context, originFunction);     //常规执行，可以先修改参数
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                UnidbgPointer pointerArg = context.getPointerArg(0);
                Inspector.inspect(pointerArg.getByteArray(0, 128), "OnLeave_arg0");

                super.postCall(emulator, context);
            }
        },true);
        */

        IxHook ixHook = XHookImpl.getInstance(emulator);
        ixHook.register("libroysue.so", "ll11l1l1ll", new ReplaceCallback() {
            //ixHook.register("test2_libroysue.so", "ll11l1l1ll", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                UnidbgPointer arg0 = context.getPointerArg(0);
                Inspector.inspect(arg0.getByteArray(0, 128), "OnEnter_arg0");
                return super.onCall(emulator, context, originFunction);
                //return HookStatus.LR(emulator, -2);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                UnidbgPointer pointerArg = context.getPointerArg(0);
                Inspector.inspect(pointerArg.getByteArray(0, 128), "OnLeave_arg0");
                super.postCall(emulator, context);
            }
        }, true);
        ixHook.refresh();
    }

    public String Sign(String str) throws FileNotFoundException {
        trace();

        DvmClass MainActivity = vm.resolveClass("com/roysue/easyso1/MainActivity");
        StringObject param1 = new StringObject(vm, str);
        DvmObject<?> retval = MainActivity.callStaticJniMethodObject(emulator, "Sign(Ljava/lang/String;)Ljava/lang/String;", param1);
        String ret = (String) retval.getValue();
        System.out.println(">>>> " + ret);
        return null;
    }


    public static void main(String[] args) throws FileNotFoundException {
        long start = System.currentTimeMillis();
        T1 t1 = new T1();
        t1.hook();
        //t1.consoleDebugger();
        t1.Sign("45678");

        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        if (signature.equals("android/os/Build->FINGERPRINT:Ljava/lang/String;")) {
            return new StringObject(vm, "123456789");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }
}

