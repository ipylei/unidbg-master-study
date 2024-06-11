package com.u1base;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class C4Bilibili extends AbstractJni implements IOResolver {
    //public class C4Bilibili extends AbstractJni  {
    private final AndroidEmulator emulator;
    private final DvmClass LibBili;
    private final VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\q4\\bilibili.apk";

    public C4Bilibili() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("tv.danmaku.bili")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        //vm.setJni(new zuiYouJNI(emulator));
        vm.setVerbose(true);

        //添加线程
        emulator.getBackend().registerEmuCountHook(10000); // 设置执行多少条指令切换一次线程
        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        //文件访问相关
        emulator.getSyscallHandler().addIOResolver(this);

        // 使用 libandroid.so 虚拟模块 (该项目不需要)
        //new AndroidModule(emulator, vm).register(memory);

        //加载so
        DalvikModule dm = vm.loadLibrary("bili", true);

        //声明类
        LibBili = vm.resolveClass("com.bilibili.nativelibrary.LibBili");

        dm.callJNI_OnLoad(emulator);
    }


    //RegisterNative(com/bilibili/nativelibrary/LibBili, s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;, RX@0x40001c97[libbili.so]0x1c97)
    //static native SignedQuery s(SortedMap<String, String> sortedMap);
    public String callS() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("build", "6180500");
        map.put("mobi_app", "android");
        map.put("channel", "shenma069");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("s_locale", "zh_CN");
        //String ret = LibBili.callStaticJniMethodObject(emulator, "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;", map).getValue().toString();

        DvmObject<?> mapObject = ProxyDvmObject.createObject(vm, map);
        String ret = LibBili.callStaticJniMethodObject(emulator, "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;", mapObject).getValue().toString();
        return ret;
    }

    public static void main(String[] args) {
        C4Bilibili p4Bilibili = new C4Bilibili();
        String result = p4Bilibili.callS();
        System.out.println("call s result:" + result);
    }


    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/Map->isEmpty()Z": {
                //向上转型
                Map map = (Map) dvmObject.getValue();
                return map.isEmpty();
            }
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;": {
                //获取JNI对象
                Map map = (Map) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue(); // 不要忘了getvalue
                //Ljava/lang/Object
                return ProxyDvmObject.createObject(vm, map.get(key));
            }
            case "java/util/Map->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;": {
                //获取JNI对象
                Map map = (Map) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue(); // 不要忘了getvalue
                Object value = varArg.getObjectArg(1).getValue();
                //Ljava/lang/Object
                return ProxyDvmObject.createObject(vm, map.put(key, value));
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            //TODO 借用静态方法，获取其返回值。这样直接拷贝会省很多时间，不用后续一个个补SignedQuery的方法
            case "com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;": {
                Map map = (Map) varArg.getObjectArg(0).getValue();
                //Ljava/lang/String
                return new StringObject(vm, SignedQuery.r(map));
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }


    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V": {
                String arg1 = varArg.getObjectArg(0).getValue().toString();
                String arg2 = varArg.getObjectArg(1).getValue().toString();
                //下面也可以!
                //String arg1 = (String) varArg.getObjectArg(0).getValue();
                //String arg2 = (String) varArg.getObjectArg(1).getValue();

                //return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(null);
                //方法1 可以
                return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(new SignedQuery(arg1, arg2));
                //TODO 方法2 也可以哦，但该类没有包名也许会有潜在问题
                //return ProxyDvmObject.createObject(vm, new SignedQuery(arg1, arg2));
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }


    //@Override
    //public FileResult resolve(Emulator emulator, String pathname, int oflags) {
    //    System.out.println("lilac open:" + pathname);
    //    return null;
    //}

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        if (pathname.equals("/proc/self/cmdline")) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
        }
        return null;
    }
}
