package com.dta.lesson32_36.lesson33;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Base64;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class JNIDogLite extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    private final DvmObject<?> object;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public JNIDogLite() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson33/DogLite.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary("doglite", true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);

        object = vm.resolveClass("com.example.doglite.MainActivity").newObject(null);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        JNIDogLite jniDogLite = new JNIDogLite();
        //jniDogLite.detectFile();
        //jniDogLite.detectFileNew();
        //jniDogLite.SysInfo();
        //jniDogLite.getAppFilesDir();
        jniDogLite.base64result();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    private void detectFile() {
        object.callJniMethod(emulator, "detectFile()V");
    }

    private void detectFileNew() {
        object.callJniMethod(emulator, "detectFileNew()V");
    }

    private void SysInfo() {
        object.callJniMethod(emulator, "SysInfo()V");
    }

    private void getAppFilesDir() {
        object.callJniMethod(emulator, "getAppFilesDir()V");
    }

    private void base64result() {
        object.callJniMethod(emulator, "base64result(Ljava/lang/String;)V", "12345");
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if (signature.equals("java/io/File-><init>(Ljava/lang/String;)V")) {
            String arg = (String) vaList.getObjectArg(0).getValue();
            System.out.println(">>>> 构造函数调用 File init， arg " + arg);
            File argFile;
            if (arg.equals("/sys/class/power_supply/battery/voltage_now")) {
                argFile = new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson33/voltage_now");
            } else {
                argFile = new File(arg);
            }
            return ProxyDvmObject.createObject(vm, argFile);
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("java/io/File->exists()Z")) {
            File file;
            Object obj = dvmObject.getValue();
            if (obj instanceof UniFile) {
                file = ((UniFile) obj).holder;
            } else {
                file = (File) obj;
            }
            return file.exists();
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        if (signature.equals("java/io/File->allocObject")) {
            return vm.resolveClass("java/io/File").newObject(new UniFile());
        }
        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        //这里调用构造方法。。。
        if (signature.equals("java/io/File-><init>(Ljava/lang/String;)V")) {
            String arg = (String) vaList.getObjectArg(0).getValue();
            System.out.println(">>>> 主动调用 File init， arg " + arg);

            File argFile;
            if (arg.equals("/sys/class/power_supply/battery/voltage_now")) {
                argFile = new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson33/voltage_now");

            } else {
                argFile = new File(arg);
            }

            //单独处理
            Object obj = dvmObject.getValue();
            if (obj instanceof UniFile) {
                ((UniFile) obj).setHolder(argFile);
                return;
            } else {
                //emulator.set();
                System.err.println("未处理，交给父类去处理");
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")) {
            //return vm.resolveClass("android/app/Application", vm.resolveClass("android/content/ContextWrapper", vm.resolveClass("android/content/Context"))).newObject(signature);
            return vm.resolveClass("android/app/Application").newObject(null);
        }
        if(signature.equals("java/io/File->getAbsolutePath()Ljava/lang/String;")){
            File file = (File) dvmObject.getValue();
            System.out.println("----" + file.getName());
            String fileName = file.getName();
            if(fileName.equals("getExternalStorageDirectory")){
                return new StringObject(vm, "/sdcard1");
            }
            else if(fileName.equals("getStorageDirectory")){
                return new StringObject(vm, "/sdcard2");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        //9774d56d682e549c
        if (signature.equals("android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;")) {
            String arg2 = vaList.getObjectArg(1).toString();
            System.out.println("~~~~~~" + arg2);
            return new StringObject(vm, "9774d56d682e549c");
        }
        if (signature.equals("android/os/Environment->getExternalStorageDirectory()Ljava/io/File;")) {
            return ProxyDvmObject.createObject(vm, new File("getExternalStorageDirectory"));
        }
        if (signature.equals("android/os/Environment->getStorageDirectory()Ljava/io/File;")) {
            return ProxyDvmObject.createObject(vm, new File("getStorageDirectory"));
        }

        if (signature.equals("android/util/Base64->encodeToString([BI)Ljava/lang/String;")) {
            byte[] bytes = (byte[]) vaList.getObjectArg(0).getValue();
            int intArg = vaList.getIntArg(1);
            System.out.println(">>> int arg " + intArg);
            String ret = Base64.getEncoder().encodeToString(bytes);
            System.out.println(">>> ret " + ret);
            StringObject stringObject = new StringObject(vm, ret);
            return stringObject;

        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.err.println(">>>> lilac open:" + pathname);
        return null;
    }
}

