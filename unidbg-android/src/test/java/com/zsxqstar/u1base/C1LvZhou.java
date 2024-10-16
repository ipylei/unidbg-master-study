package com.zsxqstar.u1base;


import com.github.unidbg.AbstractEmulator;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class C1LvZhou extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NativeApi;
    private final VM vm;

    //public String apkPath = "unidbg-android/src/test/resources/bilibili/lvzhou.apk";
    //public String soPath = "unidbg-android/src/test/resources/bilibili/liboasiscore.so";

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\q1\\lvzhou.apk";

    static {
        //针对于memory failed的处理
        Logger.getLogger(AbstractEmulator.class).setLevel(Level.DEBUG);
    }

    public C1LvZhou() {
        //创建模拟器对象
        emulator = AndroidEmulatorBuilder
                .for64Bit() //指定是arm64，还是arm32架构

                //设置指令执行引擎，为了让程序感觉自己在操作系统里，fallbackUnicorn，这是一个布尔型参数，它表示如果这个后端创建失败时如何处理——报错还是回退到 Unicorn Backend。
                //如果不添加 BackendFactory，默认使用 Unicorn Backend
                .addBackendFactory(new Unicorn2Factory(true))

                //需要强调的是，使用 Dynarmic 引擎时，不可使用基于 Unicorn的各种Hook，而应该用内置适配的 HookZz、xHook 等框架。
                //.addBackendFactory(new DynarmicFactory(true))

                //Dynarmic 和 Unicorn 一样，是模拟执行汇编指令的方案。但执行速度上比Unicorn快1-2个数量级。
                //因此如果用于RPC函数调用，建议用Dynarmic，可以获得接近于老Android设备的执行速度，再配合上Unidbg-boot-server，这样速度就上来了。
                //.addBackendFactory(new DynarmicFactory(true))

                .setProcessName("com.sina.oasis") //设置进程名

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
        //模拟器的内存操作接口
        Memory memory = emulator.getMemory();
        //设置系统类库(如libc.so等常见so)
        memory.setLibraryResolver(new AndroidResolver(23));

        //创建虚拟机对象，并指定apk文件
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);
        /*
         * `vm.setVerbose(true)` 是设置虚拟机的日志输出级别为详细模式，这意味着虚拟机会输出更多的调试信息和日志记录，
         * 这些信息可以帮助调试应用程序在虚拟机中的执行过程。在实际开发中，当应用程序出现问题时，开启该选项可以帮助开发人员定位调试问题。
         * 通常，在开发和调试过程中，打开该选项是非常有帮助的，但在生产环境中，应该禁止输出详细日志，以避免可能的性能问题。
         * */

        //加载so文件方式一(推荐)
        //参数一: 动态库或可执行ELF文件
        //参数二: 是否必须执行 init_proc、init_array 这些初始化函数
        DalvikModule dm = vm.loadLibrary("oasiscore", true);

        //加载so文件方式2
        //参数一: 动态库或可执行ELF文件
        //参数二: 是否必须执行 init_proc、init_array 这些初始化函数
        //DalvikModule dm = vm.loadLibrary(soPath, false);

        //返回Java类操作对象，然后就可以调用Native方法了
        NativeApi = vm.resolveClass("com/weibo/xvideo/NativeApi"); //声明一个类
        DvmClass NativeApi2 = vm.resolveClass("com/weibo/xvideo/leiziForverr"); //声明一个类
        //System.out.println("=====================");

        //执行so文件的加载和初始化工作
        dm.callJNI_OnLoad(emulator);
        //或者如下:
        //Module module = dm.getModule();
        //vm.callJNI_OnLoad(emulator, module);

        //ArrayListObject
    }


    //public final native String s(@NotNull byte[] bArr, boolean z);
    public String calls() {
        String arg1 = "aid=01A-khBWIm48A079Pz_DMW6PyZR8uyTumcCNm4e8awxyC2ANU.&cfrom=28B5295010&cuid=5999578300&noncestr=46274W9279Hr1X49A5X058z7ZVz024&platform=ANDROID&timestamp=1621437643609&ua=Xiaomi-MIX2S__oasis__3.5.8__Android__Android10&version=3.5.8&vid=1019013594003&wm=20004_90024";
        Boolean arg2 = false;
        //调用实例方法，需要先获取一个类，然后创建它的实例，最后才是发起调用
        //比如 Frida Call 代码就遵循这样的原则。Java.use获取类对象，$new()实例化，然后调用s方法。
        String ret = NativeApi.newObject(null).callJniMethodObject(emulator, "s([BZ)Ljava/lang/String;", arg1.getBytes(StandardCharsets.UTF_8), arg2).getValue().toString();
        //调用静态方法
        //String ret = NativeApi.callStaticJniMethod();
        return ret;
    }

    public void hook(){
        //HookZz hook = HookZz.getInstance(emulator);
        //hook.replace();
    }

    public static void main(String[] args) {
        C1LvZhou xv = new C1LvZhou();
        String result = xv.calls();
        System.out.println("call s result:" + result);
    }

}
