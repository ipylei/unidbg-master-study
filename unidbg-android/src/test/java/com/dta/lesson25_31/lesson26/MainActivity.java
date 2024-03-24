package com.dta.lesson25_31.lesson26;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import unicorn.ArmConst;

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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson26/libnative-lib.so"), true);
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
        mainActivity.check();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    private void check() {
        //emulator.attach().addBreakPoint(module, 0xB1A);
        emulator.attach().addBreakPoint(module, 0xB1A, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                UnidbgPointer pointer = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                String reg0 = pointer.getString(0);
                System.out.println("当前寄存器的值");
                System.out.println(reg0);
                return true;
            }
        });

        DvmObject<?> dvmObject = vm.resolveClass("com/r0ysue/crackme/MainActivity").newObject(null);
        String username = "123456";
        String code = "ZR1/eeOmwoMK1kN8qCqeUw==";
        boolean ret = dvmObject.callJniMethodBoolean(emulator, "check([B[B)Z", username.getBytes(), code.getBytes());
        System.out.println("=========================");
        System.out.println(ret);
    }

    //private void patch() {
    //    long patchAddr = module.base + 0xB1A;
    //    Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb);
    //    KeystoneEncoded assemble = keystone.assemble(
    //            "mov r11, r5\n" +
    //                    "nop\n" +
    //                    "nop\n" +
    //                    "nop\n" +
    //                    "nop\n" +
    //                    "nop");
    //    byte[] machineCode = assemble.getMachineCode();
    //    UnidbgPointer.pointer(emulator, patchAddr).write(machineCode);
    //}


    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        return null;
    }
}

