package com.kanxue.chap02;


import capstone.api.Instruction;
import com.github.unidbg.*;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.*;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.listener.TraceCodeListener;
import com.github.unidbg.listener.TraceWriteListener;
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

    private TraceHook traceHook;

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

        //module.findSymbolByName()

        //T1.class.getResource();
        //T1.class.getClassLoader().getResource();
    }

    public void trace() throws FileNotFoundException {
        //emulator.traceCode();
        TraceHook traceHook = emulator.traceCode(module.base, module.base + module.size);
        //traceHook.stopTrace();

        //日志保存到本地
        //保存的path
        //String traceFile = "trace6.log";
        //PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile));
        //emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
        //emulator.traceRead(module.base, module.base + module.size).setRedirect(traceStream);
        //emulator.traceWrite(module.base, module.base + module.size).setRedirect(traceStream);

        //trace写内存事件
        //emulator.traceWrite(module.base, module.base + module.size, new TraceWriteListener() {
        //    @Override
        //    public boolean onWrite(Emulator<?> emulator, long address, int size, long value) {
        //        System.out.println(Long.toHexString(address) + "----" + size + "---" + value);
        //        return false;
        //    }
        //});
    }


    //一: 只关注目标函数在某个地址所发生的调用，而不关注于在别处的调用。那么首先转到汇编界面，确认调用点是 0xE53C。
    public void traceTargetFunc() {
        long callAddr = module.base + 0xE53C;
        emulator.attach().addBreakPoint(callAddr, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                traceHook = emulator.traceCode(module.base, module.base + module.size);
                return true;
            }
        });
        emulator.attach().addBreakPoint(callAddr + 4, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                traceHook.stopTrace();
                return true;
            }
        });
    }

    //二：关注调用digest的所有位置，或者说只要是调用它我们就关心
    public void traceTargetFunc2(){
        long callAddr = module.base + 0xd804;

        emulator.attach().addBreakPoint(callAddr, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();

                traceHook = emulator.traceCode(module.base, module.base+module.size);
                emulator.attach().addBreakPoint(registerContext.getLR(), new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        traceHook.stopTrace();
                        return true;
                    }
                });
                return true;
            }
        });
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

        hookZz.replace(module.base + 0x3C9E4 + 1, new ReplaceCallback() {
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
        }, true);

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

    public void patch() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x3E8);
        byte[] code = new byte[]{(byte) 0xd0, 0x1a};
        pointer.write(code);
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
        t1.trace();
        //t1.hook();
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

/*

hook
    > hook system property
patch
consoleDebugger
trace
*/