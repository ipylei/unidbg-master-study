package com.dta.lesson25_31.lesson30;

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
import java.util.Arrays;

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
        vm.setVerbose(true);

        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson30/libphcm.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        //mainActivity.encrypt();
        mainActivity.getFlag();

        //mainActivity.test();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void test() {
        String input = "ek`fz@q2^x/t^fn0mF^6/^rb`qanqntfg^E`hq|";
        byte[] bytes = input.getBytes();
        System.out.println(Arrays.toString(bytes));
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += 1;
        }
        System.out.println(Arrays.toString(bytes));
        System.out.println(new String(bytes));
    }

    private void getFlag() {
        //emulator.attach().addBreakPoint(module, 0x0EFE);


        DvmObject<?> dvmObject = vm.resolveClass("com.ph0en1x.android_crackme.MainActivity").newObject(null);
        DvmObject<?> ret = dvmObject.callJniMethodObject(emulator, "getFlag()Ljava/lang/String;");
        String retString = (String) ret.getValue();
        System.out.println(retString);
        System.out.println(retString.length());

        //memory.malloc()
        //MemoryBlock.free
        //UnidbgPointer
        //module.callFunction()
    }

    private void encrypt() {
        DvmObject<?> dvmObject = vm.resolveClass("com.ph0en1x.android_crackme.MainActivity").newObject(null);
        DvmObject<?> ret = dvmObject.callJniMethodObject(emulator, "encrypt(Ljava/lang/String;)Ljava/lang/String", "c1d2e3");
        System.out.println(ret.getValue());

    }


    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        return null;
    }
}

