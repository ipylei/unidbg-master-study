package com.dta.lesson25_31.lesson28;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;


public class MainActivityLesson extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivityLesson(){
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson28/second.apk"));
        vm.setVerbose(true);
        vm.setJni(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson28/libJNIEncrypt.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator,module);
    }

    static {
        Logger.getLogger(AndroidElfLoader.class).setLevel(Level.INFO);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivityLesson mainActivity = new MainActivityLesson();
        System.out.println("load the vm "+( System.currentTimeMillis() - start )+ "ms");
        mainActivity.debuger();
        mainActivity.doRawData();

    }

    private void debuger() {
        emulator.attach().addBreakPoint(module,0x19f4);
    }

    private void doRawData() {
        DvmClass Encryto = vm.resolveClass("com/tencent/testvuln/c/Encryto");
        DvmObject<?> obj = vm.resolveClass("android/content/Context").newObject(null);
        String input = "123";
        DvmObject<?> dvmObject = Encryto.callStaticJniMethodObject(emulator, "doRawData(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",obj,input);
        System.out.println("result ==> " + dvmObject.getValue());
    }
}
