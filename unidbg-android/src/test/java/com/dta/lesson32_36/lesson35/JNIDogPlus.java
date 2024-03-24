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
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class JNIDogPlus extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public JNIDogPlus() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson35/DogPlus.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary("dogplus", true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        JNIDogPlus jniDogPlus = new JNIDogPlus();
        jniDogPlus.detectAccessibilityManager();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void detectAccessibilityManager() {
        DvmObject<?> dvmObject = vm.resolveClass("com.example.dogplus.MainActivity").newObject(null);
        dvmObject.callJniMethod(emulator, "detectAccessibilityManager()V");
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("android/app/ActivityThread->getApplication()Landroid/app/Application;")) {
            return vm.resolveClass("android/app/Application").newObject(null);
        }
        //获取已安装到系统的服务列表
        if (signature.equals("android/view/accessibility/AccessibilityManager->getInstalledAccessibilityServiceList()Ljava/util/List;")) {
            //return vm.resolveClass("java/util/List").newObject(null);

            DvmClass AccessibilityServiceInfo = vm.resolveClass("android.accessibilityservice.AccessibilityServiceInfo");
            List<DvmObject<?>> dvmObjects = new java.util.ArrayList<>();

            AccessibilityServiceInfo info1 = new AccessibilityServiceInfo("TalkBackService", "com.google.android.marvin.talkback", "TalkBack");
            AccessibilityServiceInfo info2 = new AccessibilityServiceInfo("SelectToSpeakService", "com.google.android.marvin.talkback", "随选朗读");
            DvmObject<?> dvmObject1 = AccessibilityServiceInfo.newObject(info1);
            DvmObject<?> dvmObject2 = AccessibilityServiceInfo.newObject(info2);
            dvmObjects.add(dvmObject1);
            dvmObjects.add(dvmObject2);

            return new ArrayListObject(vm, dvmObjects);

            //return ProxyDvmObject.createObject(vm, dvmObjects); //不可以，准确说是下一步需要重写父类方法。因为AbstractJni.java中对实现.size()、.get(index)等方法使用的是ArrayListObject。

            //DvmObject[] m = new DvmObject[]{dvmObject1, dvmObject2};
            //return new ArrayObject(m);                           //不可以，整体还是需要返回一个DvmObject对象(java/util/List)，而不是一个列表如：byte[], String[], Object[] 等
            //return ProxyDvmObject.createObject(vm, m);
        }

        if (signature.equals("android/accessibilityservice/AccessibilityServiceInfo->getResolveInfo()Landroid/content/pm/ResolveInfo;")) {
            return vm.resolveClass("android/content/pm/ResolveInfo").newObject(dvmObject.getValue());
        }

        if (signature.equals("android/content/pm/ServiceInfo->loadLabel(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;")) {
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return vm.resolveClass("java/lang/CharSequence").newObject(info.label);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        if (signature.equals("android/content/pm/ResolveInfo->serviceInfo:Landroid/content/pm/ServiceInfo;")) {
            return vm.resolveClass("android/content/pm/ServiceInfo").newObject(dvmObject.getValue());
        }

        if (signature.equals("android/content/pm/ServiceInfo->name:Ljava/lang/String;")) {
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return new StringObject(vm, info.name);
        }
        if (signature.equals("android/content/pm/ServiceInfo->packageName:Ljava/lang/String;")) {
            AccessibilityServiceInfo info = (AccessibilityServiceInfo) dvmObject.getValue();
            return new StringObject(vm, info.packageName);
        }
        if (signature.equals("android/accessibilityservice/AccessibilityServiceInfo->packageNames:[Ljava/lang/String;")) {
            return new ArrayObject();
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.err.println(">>>> lilac open:" + pathname);
        return null;
    }
}

