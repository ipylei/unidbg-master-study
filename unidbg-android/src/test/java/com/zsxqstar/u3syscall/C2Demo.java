package com.zsxqstar.u3syscall;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidARM64Emulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class C2Demo extends AbstractJni {
    private AndroidEmulator emulator;
    private VM vm;
    private DvmClass MainActivity;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u3syscall_2\\demo.apk";

    public C2Demo() {
        //emulator = AndroidEmulatorBuilder
        //        .for64Bit() //返回一个实例了!!!
        //        .addBackendFactory(new Unicorn2Factory(true))
        //        .setProcessName("com.example.demo")
        //        .build();

        // 创建模拟器实例，使用匿名类的方式实现继承和重新build方法！
        //【匿名类1】，参数is64Bit等价于.for64Bit()
        AndroidEmulatorBuilder builder = new AndroidEmulatorBuilder(true) {
            //重写build方法，返回一个AndroidEmulator实例
            @Override
            public AndroidEmulator build() {
                //【匿名类2】
                return new AndroidARM64Emulator(processName, rootDir, backendFactories) {
                    @Override
                    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
                        //使用我们自己的补系统调用类
                        return new DemoARM64SyscallHandler(svcMemory);
                    }
                };
            }
        };
        emulator = builder
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        //如果是 ARM32，那么关键代码应该改成下面这样。
        //参数is64Bit等价于.for23Bit()
        /*AndroidEmulatorBuilder builder2 = new AndroidEmulatorBuilder(false){
            @Override
            public AndroidEmulator build() {
                return new AndroidARMEmulator(processName,rootDir,backendFactories) {
                    @Override
                    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
                        return new DemoARM64SyscallHandler(svcMemory);
                    }
                };
            }
        };*/


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

    public static void main(String[] args) {
        Logger.getLogger(DemoARM64SyscallHandler.class).setLevel(Level.DEBUG); //开启模块日志
        Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG); //开启模块日志
        //Logger.getLogger(AndroidSyscallHandler.class).setLevel(Level.DEBUG); //开启模块日志

        C2Demo demo = new C2Demo();
        System.out.println("ret: " + demo.call());
    }
}
