package com.zsxqstar.u3syscall;

import com.github.unidbg.Emulator;
import com.github.unidbg.linux.ARM64SyscallHandler;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;

/*
 *
 * 我们的样例是 64 位的，所以这里继承自ARM64SyscallHandler，否则应该继承ARM32SyscallHandler
 *
 * */
public class DemoARM64SyscallHandler extends ARM64SyscallHandler {
    private static final Log log = LogFactory.getLog(DemoARM64SyscallHandler.class);
    public DemoARM64SyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    //处理Unidbg尚未模拟实现的系统调用
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        if (NR == 165) {
            getrusage(emulator);
            return true;

        }
        return super.handleUnknownSyscall(emulator, NR);
    }


    //处理系统调用getrusage
    private void getrusage(Emulator<?> emulator) {
        System.out.println("====> 补系统调用getrusage!");
        if(log.isDebugEnabled()){
            System.out.println("补系统调用getrusage!");
        }

        //SysInfo32 sysInfo32 = new SysInfo32(info);
        //sysInfo32.pack();

        //读取寄存器中的值，然后构造一个指针返回
        System.out.println("设置返回值!");
        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);

        Pointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
        byte[] rusageContent = hexStringToByteArray("00000000000000009f4a0b00000000000000000000000000c5e10100000000009052010000000000000000000000000000000000000000000000000000000000255e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d02000000000000d300000000000000");
        for (int i = 0; i < rusageContent.length; i++) {
            rusage.setByte(i, rusageContent[i]);
        }
    }


    /*//处理系统调用getrusage(备选方案)
     如果时间充裕，也可以一个个 long 去填充，看起来更清楚一些。
     00 00 00 00 00 00 00 00 9f 4a 0b 00 00 00 00 00  .........J......
     00 00 00 00 00 00 00 00 c5 e1 01 00 00 00 00 00  ................
     90 52 01 00 00 00 00 00 00 00 00 00 00 00 00 00  .R..............
     00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................
     25 5e 00 00 00 00 00 00 00 00 00 00 00 00 00 00  %^..............
     00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................
     00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................
     00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................
     0d 02 00 00 00 00 00 00 d3 00 00 00 00 00 00 00  ................
      */
    private void getrusage2(Emulator<?> emulator) {
        //emulator.getContext(); //获取寄存器

        //拿到一个指针，指向内存地址，通过该指针可操作内存
        Pointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
        rusage.setLong(0, 0);
        rusage.setLong(8, 0xB4A9FL);
        rusage.setLong(16, 0);
        rusage.setLong(24, 0x1E1C5L);
        rusage.setLong(32, 0x15290L);
        // 继续往下
    }


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
