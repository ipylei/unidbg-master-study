package com.dta.lesson32_36.lesson33;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class JNIBOSS extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public JNIBOSS() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson33/boss_last.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary("yzwg", true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        JNIBOSS jniBOSS = new JNIBOSS();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        //fix1
        if (signature.equals("com/twl/signer/YZWG->gContext:Landroid/content/Context;")) {
            return vm.resolveClass("android/content/Context").newObject(null);
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        //fix2
        if (signature.equals("android/content/pm/PackageManager->getPackagesForUid(I)[Ljava/lang/String;")) {
            int intArg = varArg.getIntArg(0);
            //System.out.println("===>" + intArg);
            String[] args = new String[]{vm.getPackageName()};
            return ProxyDvmObject.createObject(vm, args);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        //fix3
        if(signature.equals("java/lang/String->hashCode()I")){
            String obj = (String) dvmObject.getValue();
            return obj.hashCode();
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.err.println(">>>> lilac open:" + pathname);
        return null;
    }
}

