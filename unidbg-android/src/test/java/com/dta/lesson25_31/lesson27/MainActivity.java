package com.dta.lesson25_31.lesson27;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class MainActivity implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM();
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm.setVerbose(true);
        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson27/libcyberpeace.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
        //Logger.getLogger(AndroidElfLoader.class).setLevel(Level.DEBUG);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        mainActivity.CheckString();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void CheckString() {
        emulator.attach().addBreakPoint(module, 0x10B8);

        //vm.resolveClass("").newObject(null).getObjectType().callStaticJniMethodInt()
        DvmClass dvmClass = vm.resolveClass("com.testjava.jack.pingan2.cyberpeace");
        String input = "f72c5a36569418a20907b55be5bf95ad";
        int ret = dvmClass.callStaticJniMethodInt(emulator, "CheckString(Ljava/lang/String;)I", input);
        System.out.println(ret);

    }


    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        return null;
    }
}

