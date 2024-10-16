package com.zsxqstar.u1base;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.whale.IWhale;
import com.github.unidbg.hook.whale.Whale;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import unicorn.ArmConst;

import java.io.File;

public class C2Weibo extends AbstractJni {

    private final AndroidEmulator emulator;
    private final DvmClass WeiboSecurityUtils;
    private final VM vm;

    //public String apkPath = "unidbg-android/src/test/resources/weibo/sinaInternational.apk";
    //public String soPath = "unidbg-android/src/test/resources/bilibili/liboasiscore.so";

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\q2\\sinaInternational.apk";

    public C2Weibo() {
        //创建模拟器对象
        emulator = AndroidEmulatorBuilder
                .for32Bit() //指定是arm64，还是arm32架构
                //设置指令执行引擎，为了让程序感觉自己在操作系统里，fallbackUnicorn，这是一个布尔型参数，它表示如果这个后端创建失败时如何处理——报错还是回退到 Unicorn Backend。
                //如果不添加 BackendFactory，默认使用 Unicorn Backend
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.weico.international") //设置进程名
                //用于设置虚拟文件系统的根目录，在语义上它对应于Andorid的根目录。当读者认为目标SO可能会做文件访问与读写操作时，就应该设置根目录，程序对文件的读写会落在这个目录里。
                //如果不加以设置，Unidbg 会默认在本机临时目录下创建根目录，这会在将项目迁移到其他电脑上时带来不便。
                //所以我们一般会主动设置根目录，并设置为target/rootfs这个相对路径，使得潜在的文件依赖位于在当前Unidbg项目里，方便打包处理和迁移。
                //本例中没有设置根目录，算是偷懒了。
                //.setRootDir(new File("target/rootfs"))
                .build();

        // 开启线程调度器，个人建议在模拟执行相对复杂的样本时，就打开多线程；如果样本难度一般，就不必打开。
        //emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        // 设置执行多少条指令切换一次线程
        //emulator.getBackend().registerEmuCountHook(10000);

        //创建Memory对象
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        //创建虚拟机对象
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);

        //加载so文件
        DalvikModule dm = vm.loadLibrary("utility", true);
        //DalvikModule dm = vm.loadLibrary("mtguard", true); //测试
        //DalvikModule dm = vm.loadLibrary(soPath, false);

        // patch free 方法1
        emulator.attach().addBreakPoint(dm.getModule().findSymbolByName("free").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                Arm32RegisterContext registerContext = emulator.getContext();
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLR());
                return true;
            }
        });


        //返回Java类操作对象，然后就可以调用Native方法了
        WeiboSecurityUtils = vm.resolveClass("com/sina/weibo/security/WeiboSecurityUtils"); //声明一个类
        //执行so文件的加载和初始化工作
        dm.callJNI_OnLoad(emulator);

    }

    // patch free 方法2
    public void patchFree() {
        IWhale whale = Whale.getInstance(emulator);
        Symbol free = emulator.getMemory().findModule("libc.so").findSymbolByName("free");
        whale.inlineHookFunction(free, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                System.out.println("WInlineHookFunction free=" + emulator.getContext().getPointerArg(0));
                return HookStatus.LR(emulator, 0);
            }
        });
    }


    //public native String calculateS(Context context, String str, String str2);
    public String callS() {
        DvmObject<?> context = vm.resolveClass("android/app/Application", //类名
                //父类名
                vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))
        ).newObject(null);
        String arg2 = "hello world";
        String arg3 = "123456";
        return WeiboSecurityUtils.newObject(null).callJniMethodObject(emulator, "calculateS", context, arg2, arg3).getValue().toString();
        //return WeiboSecurityUtils.newObject(null).callJniMethodObject(emulator, "calculateS(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", context, arg2, arg3).getValue().toString();
        //return null;
    }


    public static void main(String[] args) {
        C2Weibo wb = new C2Weibo();
        //wb.patchFree();

        String result = wb.callS();
        System.out.println("call s result:" + result);
    }


    //补环境
    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/content/ContextWrapper->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }


}
