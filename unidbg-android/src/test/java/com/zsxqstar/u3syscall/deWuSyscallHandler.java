package com.zsxqstar.u3syscall;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.context.EditableArm32RegisterContext;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.DumpFileIO;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.ArmConst;

import java.util.concurrent.ThreadLocalRandom;

public class deWuSyscallHandler extends ARM32SyscallHandler {

    private static final Log log = LogFactory.getLog(deWuSyscallHandler.class);


    public deWuSyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }


    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        switch (NR) {
            case 190:
                vfork(emulator);
                return true;
            case 114:
                wait4(emulator);
                return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }

    //处理系统调用wait4
    private void wait4(Emulator<?> emulator) {
        return;
    }

    //处理系统调用vfork
    private void vfork(Emulator<?> emulator) {
        EditableArm32RegisterContext context = emulator.getContext();
        int pid = emulator.getPid();
        int childPid = pid + ThreadLocalRandom.current().nextInt(256);
        int r0 = childPid;
        if (log.isDebugEnabled()) {
            log.debug(pid + " vfork pid=" + r0);
        }
        context.setR0(r0);
    }

    //TODO Hook Popen之后，接下来在系统调用端做处理，重写pipe2，根据当前的command返回从ADB shell获取到的值，以及出现popen内部会使用的vfork 和wait4系统调用。
    @Override
    protected int pipe2(Emulator<?> emulator) {
        EditableArm32RegisterContext context = (EditableArm32RegisterContext) emulator.getContext();
        Pointer pipefd = context.getPointerArg(0);

        int write = getMinFd();
        this.fdMap.put(write, new DumpFileIO(write));
        int read = getMinFd();

        String command = emulator.get("command");
        System.out.println("fuck cmd<<<<<<<<: " + command);
        // stdout中写入popen command 应该返回的结果
        String stdout = "\n";
        if (command.equals("stat /data")) {
            stdout = "  File: /data\n" +
                    "  Size: 4096     Blocks: 16      IO Blocks: 512 directory\n" +
                    "Device: 10305h/66309d    Inode: 2        Links: 53\n" +
                    "Access: (0771/drwxrwx--x)       Uid: ( 1000/  system)   Gid: ( 1000/  system)\n" +
                    "Access: 2022-04-22 16:08:42.656423789 +0800\n" +
                    "Modify: 1970-02-05 00:02:38.459999996 +0800\n" +
                    "Change: 1971-07-05 09:54:50.369999990 +0800";
        }
        this.fdMap.put(read, new ByteArrayFileIO(0, "pipe2_read_side", stdout.getBytes()));

        pipefd.setInt(0, read);
        pipefd.setInt(4, write);
        context.setR0(0);
        return 0;
    }


    //处理系统调用clock_gettime, 不用在方法handleUnknownSyscall中调用，因为Unidbg自己会调用，只是缺少对应的值才报错的
    //避免直接在源码里修改
    @Override
    protected int clock_gettime(Backend backend, Emulator<?> emulator) {
        int clk_id = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer tp = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
        if (clk_id == 2) {
            //这里还可以随机
            tp.setInt(0, 0);
            tp.setInt(4, 1);
            return 0;
        }
        return super.clock_gettime(backend, emulator);
    }


    //处理系统调用ioctl
    @Override
    protected int ioctl(Emulator<?> emulator) {
        System.out.println("拦截住父类方法!!!!");

        Backend backend = emulator.getBackend();
        //int fd = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        //long argp = backend.reg_read(ArmConst.UC_ARM_REG_R2).intValue() & 0xffffffffL;
        long request = backend.reg_read(ArmConst.UC_ARM_REG_R1).intValue() & 0xffffffffL;

        if (request == 0x8927) {
            return -1; //修改源码，将抛出异常改为直接返回 -1，即函数调用失败之意。
        }
        return super.ioctl(emulator);
    }
}
