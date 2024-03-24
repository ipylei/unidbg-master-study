package com.dta.lesson25_31.lesson25;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class MainActivity implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson25/libmyjni.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        mainActivity.saveSN();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void saveSN() {
        //emulator.attach().addBreakPoint(module,0x1256);
        emulator.attach().addBreakPoint(module, 0x12be);

        DvmObject<?> dvmObject = vm.resolveClass("com/gdufs/xman/MyApp").newObject(null);
        //String sn = "EoPAoY62@ElRD";
        String sn = "222222";
        dvmObject.callJniMethod(emulator, "saveSN(Ljava/lang/String;)V", sn);

    }


    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        if ("/sdcard/reg.dat".equals(pathname)) {
            System.out.println(String.format("~~~~ pathname:%s, oflags:%s", pathname, oflags));
            File file = new File("unidbg-android/src/test/java/com/dta/lesson25/reg.dat");
            return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags, file, pathname));
        }
        return null;
    }
}

