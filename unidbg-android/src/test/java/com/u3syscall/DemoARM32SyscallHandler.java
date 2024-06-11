package com.u3syscall;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.struct.SysInfo32;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.Arm64Const;

public class DemoARM32SyscallHandler extends ARM32SyscallHandler {
    private static final Log log = LogFactory.getLog(DemoARM32SyscallHandler.class);

    public DemoARM32SyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        if (NR == 165) {
            getrusage(emulator);
            return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }


    //【*】补getrusage：方法1-形式1
    // Cyberchef
    //https://gchq.github.io/CyberChef/#recipe=From_Hexdump()To_Hex('None',0)
    private void getrusage(Emulator<?> emulator) {
        Pointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
        byte[] rusageContent = hexStringToByteArray("00000000000000009f4a0b00000000000000000000000000c5e10100000000009052010000000000000000000000000000000000000000000000000000000000255e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d02000000000000d300000000000000");
        for (int i = 0; i < rusageContent.length; i++) {
            rusage.setByte(i, rusageContent[i]);
        }
    }

    //【*】补getrusage：方法1-形式2， 一个个填充
    //private void getrusage(Emulator<?> emulator) {
    //    Pointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
    //    rusage.setLong(0, 0);
    //    rusage.setLong(8, 0xB4A9FL);
    //    rusage.setLong(16, 0);
    //    rusage.setLong(24, 0x1E1C5L);
    //    rusage.setLong(32, 0x15290L);
    //    // 继续往下
    //}

    //【*】补getrusage：方法2
    //RUsage64
    /*
    private long getrusage(Emulator<DarwinFileIO> emulator) {
        RegisterContext context = emulator.getContext();
        int who = context.getIntArg(0);
        Pointer r_usage = context.getPointerArg(1);
        RUsage64 usage64 = new RUsage64(r_usage);
        usage64.unpack();
        if (log.isDebugEnabled()) {
            log.debug("getrusage who=" + who + ", r_usage=" + r_usage + ", usage64=" + usage64);
        }
        usage64.fillDefault();
        usage64.pack();
        return 0;
    }*/

    //【*】补sysinfo：继承实现，虽然在父类中是私有方法
    private int sysinfo(Emulator<?> emulator) {
        Arm32RegisterContext context = emulator.getContext();
        Pointer info = context.getR0Pointer();
        if (log.isDebugEnabled()) {
            log.debug("sysinfo info=" + info);
        }
        SysInfo32 sysInfo32 = new SysInfo32(info);
        // 自定义
        sysInfo32.uptime = 1234;
        sysInfo32.bufferRam = 0x125600;
        sysInfo32.pack();
        return 0;
    }


    //【*】补uname：继承实现
    /*protected int uname(Emulator<?> emulator) {
        return 0;
    }*/

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
