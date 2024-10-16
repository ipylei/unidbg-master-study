package com.dta.lesson32_36.lesson35;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class L36JNIMethodID extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;


    private DvmClass Context;
    private DvmClass ContextWrapper;
    private DvmClass Application;
    private DvmClass MainActivity;
    private DvmObject<?> obj;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public L36JNIMethodID() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson35/MethodID.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary("getpackagename", true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);


        Context = vm.resolveClass("android/content/Context");
        ContextWrapper = vm.resolveClass("android/content/ContextWrapper", Context);
        Application = vm.resolveClass("android/app/Application", ContextWrapper);
        MainActivity = vm.resolveClass("com/example/getpackagename/MainActivity", Application);

        //Context = vm.resolveClass("android/content/Context");
        //ContextWrapper = vm.resolveClass("android/content/ContextWrapper");
        //Application = vm.resolveClass("android/app/Application");
        //MainActivity = vm.resolveClass("com/example/getpackagename/MainActivity", Application);
        obj = MainActivity.newObject(null);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        L36JNIMethodID jniDogPlus = new L36JNIMethodID();
        jniDogPlus.getpackagename();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void getpackagename() {
        DvmObject<?> dvmObject = vm.resolveClass("com.example.getpackagename.MainActivity").newObject(null);
        //dvmObject.callJniMethod(emulator, "getAppName()V");
        obj.callJniMethod(emulator, "getAppName()V");
        //module.callFunction(emulator,0x108B);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")) {
            //DvmClass Context = vm.resolveClass("android/content/Context");
            //DvmClass ContextWrapper = vm.resolveClass("android/content/ContextWrapper",Context);
            //DvmClass MainActivity = vm.resolveClass("com/example/getpackagename/MainActivity", ContextWrapper);
            //return vm.resolveClass("android/app/Application", MainActivity).newObject(null);
            //ProxyDvmObject.createObject()
            return Application.newObject(null);
        }
        if (signature.equals("com/example/getpackagename/MainActivity->getPackageName()Ljava/lang/String;")) {
            return new StringObject(vm, vm.getPackageName());
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    //@Override
    //public boolean acceptMethod(DvmClass dvmClass, String signature, boolean isStatic) {
    //    //System.out.println("=====>" + signature);
    //    if (signature.equals("com/example/getpackagename/MainActivity->getPackageName()Ljava/lang/String;")) {
    //        //ContextWrapper.getSuperclass();
    //        //添加到指定的methodMap
    //        int hash = signature.hashCode();
    //        try {
    //            Field filed = DvmClass.class.getDeclaredField("methodMap");
    //            filed.setAccessible(true);
    //            Map<Integer, DvmMethod> methodMap = (Map<Integer, DvmMethod>) filed.get(ContextWrapper);
    //            methodMap.put(hash, new DvmMethod(dvmClass, "getPackageName", "()Ljava/lang/String;", false));
    //            //return false;
    //        } catch (IllegalAccessException | NoSuchFieldException e) {
    //            throw new RuntimeException(e);
    //        }
    //
    //    }
    //    return super.acceptMethod(dvmClass, signature, isStatic);
    //}

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.err.println(">>>> lilac open:" + pathname);
        return null;
    }
}

