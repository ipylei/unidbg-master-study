package com.kanxue.chap02;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.ModuleListener;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.listener.TraceWriteListener;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

//import static com.dta.lesson2.AesKeyFinder.readFuncFromIDA;

public class T2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Memory memory;
    private final Module module;

    private final Map<Long, byte[]> patchMap = new HashMap<>();

    static {
        //Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG);
    }

    public T2() throws IOException {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                //.setProcessName()
                //.setRootDir()
                //.setRootDir(new File("target/rootfs/default"))
                //.addBackendFactory(new DynarmicFactory(true))
                .build();

        vm = emulator.createDalvikVM();
        vm.setJni(this);

        vm.setVerbose(true);
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        //memory.disableCallInitFunction();
        //vm.addNotFoundClass("xxxxx");

        //【*】patch过掉在init、init_array中编写的反调试逻辑
        memory.addModuleListener(new ModuleListener() {
            @Override
            public void onLoaded(Emulator<?> emulator, Module module) {
                System.out.println("so: " + module.toString() + "loaded!");
                if (module.name.equals("libnative-lib.so")) {
                    System.out.println("start patch libnative-lib.so");
                    long baseaddr = module.base;
                    byte[] nop = {0x00, (byte) 0xbf, 0x00, (byte) 0xbf};
                    emulator.getBackend().mem_write(baseaddr + 0xc15c, nop);
                    emulator.getBackend().mem_write(baseaddr + 0xd316, nop);

                    emulator.getBackend().mem_write(baseaddr + 0xccb4, nop);
                    emulator.getBackend().mem_write(baseaddr + 0xd322, nop);

                    emulator.getBackend().mem_write(baseaddr + 0xd4dc, nop);
                }
            }
        });


        traceAndPatchStart();
        DalvikModule dalvikModule = vm.loadLibrary(new File("E:\\Learning相应资料\\Learn_Spider相应资料\\网课-远\\P2\\第2章 NDK开发详解\\课件\\4月\\test2_libroysue.so"), true);
        module = dalvikModule.getModule();
        vm.callJNI_OnLoad(emulator, module);
        System.out.println("load so file success!");
        traceAndPatchEnd();
    }

    public void traceAndPatchStart() throws IOException {
        //这里的起/止地点是需要自己选择的，以及trace时机也需要自己选择
        emulator.traceWrite(module.base, module.base + module.size, new TraceWriteListener() {
            @Override
            public boolean onWrite(Emulator<?> emulator, long address, int size, long value) {
                System.out.println(Long.toHexString(address) + "----" + size + "---" + value);
                byte[] bytes = long2Bytes(value, size);
                patchMap.put(address, bytes);
                return false;
            }
        });
    }

    public void traceAndPatchEnd() throws IOException {
        //traceWrite()完成后，可以进行patch了
        String soPath = "unidbg-android/src/test/resources/example_binaries/test/obftest/libcrack.so";
        byte[] fileContent = readFile(new File(soPath));
        patchMap.forEach((addr, bytes) -> {
            if (addr >= module.base && addr <= module.base + module.size) {
                long offset = addr - module.base - 0x1000;
                for (int i = 0; i < bytes.length; i++) {
                    fileContent[(int) (offset + i)] = bytes[i];
                }
            }
        });

        //最后写入文件
        try (FileOutputStream out = new FileOutputStream(new File(soPath + ".fix"))) {
            out.write(fileContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] long2Bytes(long num, int size) {
        byte[] byteNum = new byte[size];
        for (int ix = 0; ix < byteNum.length; ++ix) {
            int offset = size * 8 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }

    public static byte[] readFile(File file) throws IOException {
        long len = file.length();
        byte[] bytes = new byte[(int) len];
        try (FileInputStream in = new FileInputStream(file)) {
            in.read(bytes);
        }
        return bytes;
    }


    public String Sign(String str) throws IOException {
        //trace();

        DvmClass MainActivity = vm.resolveClass("com/roysue/easyso1/MainActivity");
        StringObject param1 = new StringObject(vm, str);
        DvmObject<?> retval = MainActivity.callStaticJniMethodObject(emulator, "Sign(Ljava/lang/String;)Ljava/lang/String;", param1);
        String ret = (String) retval.getValue();
        System.out.println(">>>> " + ret);
        return null;
    }


    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        T2 t1 = new T2();
        t1.Sign("45678");
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        if (signature.equals("android/os/Build->FINGERPRINT:Ljava/lang/String;")) {
            return new StringObject(vm, "123456789");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }
}


/*
patch过掉在init、init_array中编写的反调试逻辑
字符串加密
*/