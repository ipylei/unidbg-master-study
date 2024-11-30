package com.zsxqstar.u3syscall;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.ARM64SyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class C4KuaiShou extends AbstractJni implements IOResolver {

    private AndroidEmulator emulator;
    private DvmClass Watermelon;
    private VM vm;

    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\u3syscall_4_快手\\快手_10.3.40.25268.apk";

    public String sourceDir = "unidbg-android/src/test/resources/projects/kuaishou";
    public String rootDir = sourceDir + "/rootfs";

    public C4KuaiShou() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setRootDir(new File(rootDir))
                .setProcessName("com.smile.gifmaker")
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        vm.setVerbose(true);

        //处理文件访问
        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dm = vm.loadLibrary("ksse", true);
        Watermelon = vm.resolveClass("com.kuaishou.dfp.envdetect.jni.Watermelon");
        dm.callJNI_OnLoad(emulator);
    }


    public String call() {
        return Watermelon.newObject(null).callJniMethodObject(emulator, "jniCommand(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1114128, null, null, null).getValue().toString();
    }


    public static void main(String[] args) {
        Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.DEBUG); //开启模块日志

        C4KuaiShou ks = new C4KuaiShou();
        System.out.println("ret:"+ks.call());
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open =======>:" + pathname);
        return null;
    }
}
