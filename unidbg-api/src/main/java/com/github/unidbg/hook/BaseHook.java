package com.github.unidbg.hook;

import com.github.unidbg.Emulator;
import com.github.unidbg.Family;
import com.github.unidbg.Module;
import com.github.unidbg.arm.Arm64Hook;
import com.github.unidbg.arm.ArmHook;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.context.EditableArm32RegisterContext;
import com.github.unidbg.arm.context.EditableArm64RegisterContext;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.spi.LibraryFile;
import com.sun.jna.Pointer;

import java.net.URL;
import java.util.Stack;

public abstract class BaseHook implements IHook {

    protected final Emulator<?> emulator;
    protected final Module module;

    public BaseHook(Emulator<?> emulator, String libName) {
        this.emulator = emulator;
        this.module = emulator.getMemory().load(resolveLibrary(libName)); //加载库：类似于android linker
    }

    //Pointer new_func = createReplacePointer(callback, old_func, enablePostCall);
    //参数1：callback  注册的回调函数
    //参数2：backup    一个函数指针，原型与被hook的函数地址一致
    //参数3：enablePostCall  是否对return处生效
    //申请一块内存，往该地址处填入一些内容(svc #xxx)，然后返回一个指针
    protected Pointer createReplacePointer(final ReplaceCallback callback, final Pointer backup, boolean enablePostCall) {
        SvcMemory svcMemory = emulator.getSvcMemory();
        return svcMemory.registerSvc(emulator.is64Bit() ? new Arm64Hook(enablePostCall) {
            private final Stack<Object> context = new Stack<>();
            @Override
            protected HookStatus hook(Emulator<?> emulator) {
                return callback.onCall(emulator, new Arm64HookContext(context, emulator.<EditableArm64RegisterContext>getContext()), backup.getLong(0));
            }
            @Override
            public void handlePostCallback(Emulator<?> emulator) {
                super.handlePostCallback(emulator);
                EditableArm64RegisterContext registerContext = emulator.getContext();
                callback.postCall(emulator, new Arm64HookContext(context, registerContext));
            }
        } : new ArmHook(enablePostCall) {
            private final Stack<Object> context = new Stack<>();

            //before
            @Override
            protected HookStatus hook(Emulator<?> emulator) {
                return callback.onCall(emulator, new Arm32HookContext(context, emulator.<EditableArm32RegisterContext>getContext()), backup.getInt(0) & 0xffffffffL);
            }
            //after
            @Override
            public void handlePostCallback(Emulator<?> emulator) {
                super.handlePostCallback(emulator);
                EditableArm32RegisterContext registerContext = emulator.getContext();
                callback.postCall(emulator, new Arm32HookContext(context, registerContext));
            }
        });
    }

    protected LibraryFile resolveLibrary(String libName) {
        Family family = emulator.getFamily();
        String lib = libName + family.getLibraryExtension();
        URL url = BaseHook.class.getResource(family.getLibraryPath() + lib);
        if (url == null) {
            throw new IllegalStateException("resolve library failed: " + lib);
        }

        return emulator.createURLibraryFile(url, lib);
    }

    @Override
    public Module getModule() {
        return module;
    }

}
