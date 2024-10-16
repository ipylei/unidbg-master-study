package com.zsxqstar.u1base;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class C3ZuiYou extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NetCrypto;
    private final VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\q3\\right573.apk";

    public C3ZuiYou() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("cn.xiaochuankeji.tieba")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        //vm.setJni(new zuiYouJNI(emulator));
        vm.setVerbose(true);

        // 使用 libandroid.so 虚拟模块
        new AndroidModule(emulator, vm).register(memory);

        //加载so
        DalvikModule dm = vm.loadLibrary("net_crypto", true);

        //声明类
        NetCrypto = vm.resolveClass("com.izuiyou.network.NetCrypto");

        dm.callJNI_OnLoad(emulator);
    }

    //public static native String sign(String str, byte[] bArr);
    public String callSign() {
        String arg1 = "hello world";
        byte[] arg2 = "V I 50".getBytes(StandardCharsets.UTF_8);
        String ret = NetCrypto.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;[B)Ljava/lang/String;", arg1, arg2).getValue().toString();
        //NetCrypto.callStaticJniMethodBoolean()
        //NetCrypto.callStaticJniMethodObject()
        return ret;
    }

    public static void main(String[] args) {
        C3ZuiYou nw = new C3ZuiYou();
        String result = nw.callSign();
        System.out.println("call s result:" + result);
    }

    //补环境
    //@Override
    //public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
    //    switch (signature) {
    //        case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;": {
    //            //DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
    //
    //            //这里我们使用另一个方式构造，为什么要这么做，这是一个小坑，放后文讨论，请读者先忍耐一下。
    //            DvmObject<?> context = vm.resolveClass("cn/xiaochuankeji/tieba/AppController").newObject(null);
    //
    //            //基本使用(九)答疑：
    //            //getAppContext的返回值类型是Context，它是一个抽象类，返回的是抽象类的子类对象。
    //            //在一般情况里我们不需要讲究，但这里它获取了返回的Context的类名，因此不得不确认样本到底返回了Context的什么子类对象，否则类名就对不上了。
    //            return context;
    //        }
    //
    //    }
    //    return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    //}

    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            //AbstractJni已有的，直接复制过来即可
            case "cn/xiaochuankeji/tieba/AppController->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "cn/xiaochuankeji/tieba/AppController->getPackageName()Ljava/lang/String;": {
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
                break;
            }
            case "cn/xiaochuankeji/tieba/AppController->getClass()Ljava/lang/Class;": {
                return dvmObject.getObjectType();
            }

            //TODO AbstractJni中没有的，则需要自己补了
            //这在 AbstractJNI 里可没有，像下面这样处理，为什么这么做同样放到后面讨论。
            //TODO 这里是一个Class实例与class一一对应，也就是一个class对象加入到内存中了!
            case "java/lang/Class->getSimpleName()Ljava/lang/String;": {
                String className = ((DvmClass) dvmObject).getClassName();
                String[] name = className.split("/");
                return new StringObject(vm, name[name.length - 1]);
            }

            //case "cn/xiaochuankeji/tieba/AppController->getFilesDir()Ljava/io/File;":{
            //    return vm.resolveClass("java/io/File").newObject(null);
            //}

            case "java/io/File->getAbsolutePath()Ljava/lang/String;": {
                //TODO dvmObject.getValue() instanceof String ============> true
                return new StringObject(vm, dvmObject.getValue().toString());
            }

            //TODO 方法签名重复了
            case "cn/xiaochuankeji/tieba/AppController->getFilesDir()Ljava/io/File;": {
                //return vm.resolveClass("java/io/File").newObject("ipylei");
                return vm.resolveClass("java/io/File").newObject("/data/data/cn.xiaochuankeji.tieba/files");
            }

        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z": {
                return false;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Process->myPid()I":{
                return emulator.getPid();
            }
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }
}