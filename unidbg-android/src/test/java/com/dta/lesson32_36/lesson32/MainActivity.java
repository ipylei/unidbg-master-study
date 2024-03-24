package com.dta.lesson32_36.lesson32;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class MainActivity extends AbstractJni implements IOResolver<AndroidFileIO> {
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
        vm.setJni(this);
        //vm.addNotFoundClass();

        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson32_36/lesson32/libdogpro.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        mainActivity.getHash();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }

    private void getHash() {
        DvmObject<?> dvmObject = vm.resolveClass("com.example.dogpro.MainActivity").newObject(null);
        String input = "/data/app/com.example.dogpro-pnF2J3-qBi8ei74vXTNXmQ==/base.apk";
        DvmObject<?> result = dvmObject.callJniMethodObject(emulator, "getHash(Ljava/lang/String;)Ljava/lang/String;", input);
        System.out.println("getHash return => " + result.getValue());
    }


    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "java/util/zip/ZipFile-><init>(Ljava/lang/String;)V": {
                String arg = vaList.getObjectArg(0).getValue().toString();
                System.out.println("===> arg " + arg);
                try {
                    if (arg.equals("/data/app/com.example.dogpro-pnF2J3-qBi8ei74vXTNXmQ==/base.apk")) {
                        ZipFile zipFile = new ZipFile("unidbg-android/src/test/java/com/dta/lesson32_36/lesson32/app-debug.apk");
                        return ProxyDvmObject.createObject(vm, zipFile);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("未处理的情况 arg=> " + arg);

            }
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/util/zip/ZipFile->entries()Ljava/util/Enumeration;": {
                ZipFile zipFile = (ZipFile) dvmObject.getValue();
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                List<DvmObject<?>> list = new ArrayList<>();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    list.add(ProxyDvmObject.createObject(vm, zipEntry));
                }
                return new com.github.unidbg.linux.android.dvm.Enumeration(vm, list);
            }

            case "java/util/zip/ZipEntry->getName()Ljava/lang/String;": {
                ZipEntry zipEntry = (ZipEntry) dvmObject.getValue();
                return new StringObject(vm, zipEntry.getName());
            }

            case "java/util/zip/ZipFile->getInputStream(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;": {
                ZipFile zipFile = (ZipFile) dvmObject.getValue();
                ZipEntry zipEntry = (ZipEntry) vaList.getObjectArg(0).getValue();
                try {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    return ProxyDvmObject.createObject(vm, inputStream);
                    //return vm.resolveClass("java/io/InputStream").newObject(inputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "java/security/MessageDigest->digest()[B": {
                MessageDigest md = (MessageDigest) dvmObject.getValue();
                byte[] digest = md.digest();
                return new ByteArray(vm, digest);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }


    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/String->endsWith(Ljava/lang/String;)Z": {
                String string = (String) dvmObject.getValue();
                String arg = (String) vaList.getObjectArg(0).getValue();
                System.out.println("====> obj => " + string);
                System.out.println("====> arg => " + arg);
                return string.endsWith(arg);
            }

        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/util/zip/ZipFile$ZipFileInputStream->read([B)I":
            case "java/io/InputStream->read([B)I": {
                InputStream inputStream = (InputStream) dvmObject.getValue();
                byte[] bytes = (byte[]) vaList.getObjectArg(0).getValue();
                try {
                    int count = inputStream.read(bytes);
                    return count;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/security/MessageDigest->update([B)V": {
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                byte[] bytes = (byte[]) vaList.getObjectArg(0).getValue();
                messageDigest.update(bytes);
                return;
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
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

