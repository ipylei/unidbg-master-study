package com.dta.lesson25_31.lesson28;

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
import com.github.unidbg.pointer.UnidbgPointer;

import java.io.File;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class MainActivity2 extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    public MainActivity2() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        //vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson28/second.apk"));
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(true);
        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson28/libJNIEncrypt.so"), false);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
        //Logger.getLogger(AndroidElfLoader.class).setLevel(Level.DEBUG);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity2 mainActivity2 = new MainActivity2();
        //mainActivity.encode();
        mainActivity2.encode2();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void encode() {
        DvmClass dvmClass = vm.resolveClass("com.tencent.testvuln.c.Encryto");
        DvmObject<?> dvmObject = vm.resolveClass("com.tencent.testvuln.SecondActivity").newObject(null);
        String arg = "123";
        DvmObject<?> dvmObject1 = dvmClass.callStaticJniMethodObject(emulator, "encode(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", dvmObject, arg);
        String ret = (String) dvmObject1.getValue();
        System.out.println("===>" + ret);
    }


    private void encode2() {
        //emulator.traceCode();
        emulator.attach().addBreakPoint(module.base + 0x3948);

        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x3948);
        byte[] code = new byte[]{0x01, 0x20, 0x00, (byte) 0xBF};
        pointer.write(code);

        //emulator.getBackend().mem_write(module.base + 0x3948, code);


        DvmClass Encryto = vm.resolveClass("com.tencent.testvuln.c.Encryto");
        String arg = "123";
        DvmObject<?> dvmObject1 = Encryto.callStaticJniMethodObject(emulator, "doRawData(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", 0, arg);
        String ret = (String) dvmObject1.getValue();
        //System.out.println("===>" + ret);

        //DvmClass Encryto = vm.resolveClass("com/tencent/testvuln/c/Encryto");
        //DvmObject<?> obj = vm.resolveClass("android/content/Context").newObject(null);
        //String input = "123";
        //DvmObject<?> dvmObject = Encryto.callStaticJniMethodObject(emulator, "doRawData(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", 0, input);
        //System.out.println("doRawdata result ==> " + dvmObject.getValue());
    }


    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "com/tencent/testvuln/SecondActivity->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "com/tencent/testvuln/SecondActivity->getPackageName()Ljava/lang/String;": {
                //return new StringObject(vm, "com.tencent.testvuln");
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
                break;
            }

        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        return null;
    }
}

