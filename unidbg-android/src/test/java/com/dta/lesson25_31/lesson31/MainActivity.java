package com.dta.lesson25_31.lesson31;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        DalvikModule dalvikModule = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson25_31/lesson31/libcheck.so"), true);
        module = dalvikModule.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        mainActivity.sub_85E0();
        System.out.println("load the vm " + (System.currentTimeMillis() - start) + "ms");
    }


    private void sub_85E0() {
        //emulator.traceCode();

        List<Object> args = new ArrayList<>();
        UnidbgPointer ptr_arg0 = UnidbgPointer.pointer(emulator, module.base + 0xF1B0);
        String md5 = "f8c49056e4ccf9a11e090eaf471f418d";
        UnidbgPointer ptr_arg2 = memory.malloc(md5.length(), true).getPointer();
        ptr_arg2.setString(0, md5);

        args.add(ptr_arg0.toIntPeer());
        args.add(622);
        args.add(ptr_arg2.toIntPeer());

        Number number = module.callFunction(emulator, 0x85E0 + 1, args.toArray());
        System.out.println("====> sub_85E0 return => " + number.intValue());

        sub_code(number.intValue());
        //sub_shellCode(number.intValue());
    }

    private void sub_code(int addr) {

        String input = "qqqqqqq";
        MemoryBlock malloc = memory.malloc(input.length(), true);
        UnidbgPointer ptr_input = malloc.getPointer();
        //ptr_input.setString(0, input);


        //UnidbgPointer ptr_pipe = memory.allocateStack(8);    //在栈上开辟一块空间
        UnidbgPointer ptr_pipe = memory.malloc(8, true).getPointer();
        ptr_pipe.setInt(0, 0);
        ptr_pipe.setInt(4, 1);

        //UnidbgPointer ptr_v9 = memory.allocateStack(8);      //在栈上开辟一块空间
        UnidbgPointer ptr_v9 = memory.malloc(8, true).getPointer();
        ptr_v9.setPointer(0, ptr_input);
        ptr_v9.setPointer(4, ptr_pipe);

        List<Object> args = new ArrayList<>();
        args.add(ptr_v9.toIntPeer());
        //Number number = module.callFunction(emulator, addr - module.base + 1, args.toArray());
        Number number = module.callFunction(emulator, addr - module.base + 1, ptr_v9.toIntPeer());
        System.out.println("====> sub_code return => " + number.intValue());
    }

    private void sub_shellCode(long addr) {
        List<Object> args = new ArrayList<>();

        String input = "qqqqqqq";
        MemoryBlock malloc = memory.malloc(input.length(), true);
        UnidbgPointer ptr_input = malloc.getPointer();
        ptr_input.setString(0, input);

        UnidbgPointer ptr_v9 = memory.allocateStack(8);
        //ptr_v9.setPointer(0, ptr_input);


        UnidbgPointer ptr_pipe = memory.allocateStack(8);
        ptr_pipe.setInt(0, 0);
        ptr_pipe.setInt(4, 1);

        ptr_v9.setPointer(0, ptr_input);
        ptr_v9.setPointer(4, ptr_pipe);

        args.add(ptr_v9.toIntPeer());
        Number number = module.callFunction(emulator, addr - module.base + 1, args.toArray());
        System.out.println("shellcode result => " + number.intValue());
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if (signature.equals("com/a/sample/loopcrypto/Decode->a([BI)Ljava/lang/String;")) {
            byte[] bArr = (byte[]) varArg.getObjectArg(0).getValue();
            int i = varArg.getIntArg(1);
            return new StringObject(vm, Decode.a(bArr, i));
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        System.out.println(">>>> lilac open:" + pathname);
        return null;
    }
}

