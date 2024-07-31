package com.github.unidbg.arm;

import com.github.unidbg.Emulator;
import com.github.unidbg.Svc;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.ArmConst;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public abstract class ArmHook extends ArmSvc {

    private static final Log log = LogFactory.getLog(ArmHook.class);

    private final boolean enablePostCall;

    protected ArmHook() {
        this(false);
    }

    protected ArmHook(boolean enablePostCall) {
        this.enablePostCall = enablePostCall;
    }

    public ArmHook(String name, boolean enablePostCall) {
        super(name);
        this.enablePostCall = enablePostCall;
    }

    @Override
    public final UnidbgPointer onRegister(SvcMemory svcMemory, int svcNumber) {
        byte[] code;
        if (enablePostCall) {
            try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
                KeystoneEncoded encoded = keystone.assemble(Arrays.asList(
                        "svc #0x" + Integer.toHexString(svcNumber),  //被handle()捕获到，执行java层: ArmHook(或子类)::hook() -> callback.onCall()

                        //执行完java层: 再下面的hook()函数中，执行完ArmHook::hook()后，会在SP寄存器中存放jump函数的地址
                        //从SP寄存器弹出值存放至R7寄存器，所以R7寄存器中存放的是jump函数地址
                        "pop {r7}",

                        //对比r7的值是否是0
                        "cmp r7, #0",
                        "bxeq lr",  //如果是则直接跳转到lr寄存器所指向的地址，即直接return
                        "blx r7",   //否则，跳转到r7所指向的地址

                        "mov r7, #0",

                        //使用r5、r4这两个寄存器来存放特殊值，下面会被svc #0捕获到
                        //if (swi == 0 && NR == 0 && (backend.reg_read(ArmConst.UC_ARM_REG_R5).intValue()) == Svc.POST_CALLBACK_SYSCALL_NUMBER) { // postCallback
                        //int number = backend.reg_read(ArmConst.UC_ARM_REG_R4).intValue();
                        "mov r5, #0x" + Integer.toHexString(Svc.POST_CALLBACK_SYSCALL_NUMBER),
                        "mov r4, #0x" + Integer.toHexString(svcNumber),

                        "svc #0",   //借助svc #0来 执行ArmHook(或子类)::handlePostCallback() -> callback::postCall()
                        "bx lr"     //返回
                ));
                code = encoded.getMachineCode();
            }
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // svc #0xsvcNumber
            buffer.putInt(ArmSvc.assembleSvc(svcNumber));
            //等价于
            //ldr pc, [sp]             ;读取栈顶(sp寄存器)的值赋值给pc寄存器
            //add sp, sp, #4           ;回收栈空间
            buffer.putInt(0xe49df004); // pop {pc}: manipulated stack in handle
            code = buffer.array();
        }
        String name = getName();
        UnidbgPointer pointer = svcMemory.allocate(code.length, name == null ? "ArmHook" : name);
        pointer.write(0, code, 0, code.length);
        if (log.isDebugEnabled()) {
            log.debug("ARM hook: pointer=" + pointer);
        }
        return pointer;
    }

    @Override
    public void handlePostCallback(Emulator<?> emulator) {
        super.handlePostCallback(emulator);

        if (regContext == null) {
            throw new IllegalStateException();
        } else {
            //恢复寄存器的内容
            regContext.restore();
        }
    }

    private RegContext regContext;

    @Override
    public final long handle(Emulator<?> emulator) {
        Backend backend = emulator.getBackend();
        if (enablePostCall) {
            //保存寄存器的内容
            regContext = RegContext.backupContext(emulator, ArmConst.UC_ARM_REG_R4,
                                                            ArmConst.UC_ARM_REG_R5,
                                                            ArmConst.UC_ARM_REG_R6,
                                                            ArmConst.UC_ARM_REG_R7,
                                                            ArmConst.UC_ARM_REG_LR);
        }

        //读取sp寄存器的值，然后视为指针
        UnidbgPointer sp = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
        try {
            HookStatus status = hook(emulator);         //执行：ArmHook(或子类)::hook() --> callback.onCall()

            //继续向下执行，pc寄存器设置为目标函数地址
            //HookStatus::RET(Emulator<?> emulator, long pc)
            if (status.forward || !enablePostCall) {
                sp = sp.share(-4, 0);          //栈向下增长，所以是开辟空间
                sp.setInt(0, (int) status.jump);  //重定向的地址
            }

            //直接设置返回值，pc寄存器设置为0
            //HookStatus::LR(Emulator<?> emulator, long returnValue)
            else {
                sp = sp.share(-4, 0);         //栈向下增长，所以是开辟空间
                sp.setInt(0, 0);
            }

            return status.returnValue;  //直接取值: r0寄存器，or 调用LR()直接设置的返回值
        } finally {
            backend.reg_write(ArmConst.UC_ARM_REG_SP, sp.peer);
        }
    }

    protected abstract HookStatus hook(Emulator<?> emulator);

}
