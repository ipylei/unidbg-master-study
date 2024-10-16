package com.github.unidbg.linux;

import com.github.unidbg.Alignment;
import com.github.unidbg.Emulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.ARM;
import com.github.unidbg.arm.ARMEmulator;
import com.github.unidbg.file.FileIO;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.file.linux.IOConstants;
import com.github.unidbg.hook.HookListener;
import com.github.unidbg.linux.android.ElfLibraryFile;
import com.github.unidbg.linux.thread.PThreadInternal;
import com.github.unidbg.memory.MemRegion;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryAllocBlock;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.memory.MemoryBlockImpl;
import com.github.unidbg.memory.MemoryMap;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.spi.AbstractLoader;
import com.github.unidbg.spi.InitFunction;
import com.github.unidbg.spi.LibraryFile;
import com.github.unidbg.spi.Loader;
import com.github.unidbg.thread.Task;
import com.github.unidbg.unix.IO;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.github.unidbg.virtualmodule.VirtualSymbol;
import com.sun.jna.Pointer;
import net.fornwall.jelf.ArmExIdx;
import net.fornwall.jelf.ElfDynamicStructure;
import net.fornwall.jelf.ElfException;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfRelocation;
import net.fornwall.jelf.ElfSection;
import net.fornwall.jelf.ElfSegment;
import net.fornwall.jelf.ElfSymbol;
import net.fornwall.jelf.GnuEhFrameHeader;
import net.fornwall.jelf.MemoizedObject;
import net.fornwall.jelf.PtLoadData;
import net.fornwall.jelf.SymbolLocator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;
import unicorn.Unicorn;
import unicorn.UnicornConst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AndroidElfLoader extends AbstractLoader<AndroidFileIO> implements Memory, Loader {

    private static final Log log = LogFactory.getLog(AndroidElfLoader.class);

    private Symbol malloc, free;

    public AndroidElfLoader(Emulator<AndroidFileIO> emulator, UnixSyscallHandler<AndroidFileIO> syscallHandler) {
        super(emulator, syscallHandler);

        // init stack，初始化SP(堆栈寄存器)
        stackSize = STACK_SIZE_OF_PAGE * emulator.getPageAlign();

        //将栈空间mem_map映射，因为在Backend(Unicorn)中，所有需要用的内存都需要先进行映射才能够进行使用，
        //大小就是STACK_SIZE_OF_PAGE * emulator.getPageAlign()，可读可写权限
        backend.mem_map(STACK_BASE - stackSize, stackSize, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);

        //设置SP(堆栈寄存器)
        setStackPoint(STACK_BASE);

        // 初始化TLS(线程局部存储相关)，在libc一些系统库中是有线程局部变量的，如errno等。这里就做了相关的协处理器的初始化操作
        this.environ = initializeTLS(new String[]{
                "ANDROID_DATA=/data",
                "ANDROID_ROOT=/system",
                "PATH=/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin",
                "NO_ADDR_COMPAT_LAYOUT_FIXUP=1"
        });
        this.setErrno(0);
    }

    //设置系统解析类库，如：.setLibraryResolver(new AndroidResolver(23));
    @Override
    public void setLibraryResolver(LibraryResolver libraryResolver) {
        super.setLibraryResolver(libraryResolver);

        /*
         * 注意打开顺序很重要
         */
        syscallHandler.open(emulator, IO.STDIN, IOConstants.O_RDONLY);
        syscallHandler.open(emulator, IO.STDOUT, IOConstants.O_WRONLY);
        syscallHandler.open(emulator, IO.STDERR, IOConstants.O_WRONLY);
    }

    @Override
    protected LibraryFile createLibraryFile(File file) {
        return new ElfLibraryFile(file, emulator.is64Bit());
    }

    private UnidbgPointer initializeTLS(String[] envs) {
        final Pointer thread = allocateStack(0x400); // reserve space for pthread_internal_t
        PThreadInternal pThread = PThreadInternal.create(emulator, thread);
        pThread.tid = emulator.getPid();
        pThread.pack();

        final Pointer __stack_chk_guard = allocateStack(emulator.getPointerSize());

        final Pointer programName = writeStackString(emulator.getProcessName());

        final Pointer programNamePointer = allocateStack(emulator.getPointerSize());
        assert programNamePointer != null;
        programNamePointer.setPointer(0, programName);

        final Pointer auxv = allocateStack(0x100);
        assert auxv != null;
        final int AT_RANDOM = 25; // AT_RANDOM is a pointer to 16 bytes of randomness on the stack.
        auxv.setPointer(0, UnidbgPointer.pointer(emulator, AT_RANDOM));
        auxv.setPointer(emulator.getPointerSize(), __stack_chk_guard);
        final int AT_PAGESZ = 6;
        auxv.setPointer(emulator.getPointerSize() * 2L, UnidbgPointer.pointer(emulator, AT_PAGESZ));
        auxv.setPointer(emulator.getPointerSize() * 3L, UnidbgPointer.pointer(emulator, emulator.getPageAlign()));

        List<String> envList = new ArrayList<>();
        for (String env : envs) {
            int index = env.indexOf('=');
            if (index != -1) {
                envList.add(env);
            }
        }
        final Pointer environ = allocateStack(emulator.getPointerSize() * (envList.size() + 1));
        assert environ != null;
        Pointer pointer = environ;
        for (String env : envList) {
            Pointer envPointer = writeStackString(env);
            pointer.setPointer(0, envPointer);
            pointer = pointer.share(emulator.getPointerSize());
        }
        pointer.setPointer(0, null);

        final UnidbgPointer argv = allocateStack(0x100);
        assert argv != null;
        argv.setPointer(emulator.getPointerSize(), programNamePointer);
        argv.setPointer(2L * emulator.getPointerSize(), environ);
        argv.setPointer(3L * emulator.getPointerSize(), auxv);

        final UnidbgPointer tls = allocateStack(0x80 * 4); // tls size
        assert tls != null;
        tls.setPointer(emulator.getPointerSize(), thread);
        this.errno = tls.share(emulator.getPointerSize() * 2L);
        tls.setPointer(emulator.getPointerSize() * 3L, argv);

        if (emulator.is32Bit()) {
            backend.reg_write(ArmConst.UC_ARM_REG_C13_C0_3, tls.peer);
        } else {
            backend.reg_write(Arm64Const.UC_ARM64_REG_TPIDR_EL0, tls.peer);
        }

        long sp = getStackPoint();
        sp &= (~(emulator.is64Bit() ? 15 : 7));
        setStackPoint(sp);

        if (log.isDebugEnabled()) {
            log.debug("initializeTLS tls=" + tls + ", argv=" + argv + ", auxv=" + auxv + ", thread=" + thread + ", environ=" + environ + ", sp=0x" + Long.toHexString(getStackPoint()));
        }
        return argv.share(2L * emulator.getPointerSize(), 0);
    }

    //存放所有已加载的模块
    private final Map<String, LinuxModule> modules = new LinkedHashMap<>();

    protected final LinuxModule loadInternal(LibraryFile libraryFile, boolean forceCallInit) {
        try {
            //接着调用了loadInternal(重载)方法，继续加载流程
            LinuxModule module = loadInternal(libraryFile);
            //处理符号(关于重定位)
            resolveSymbols(!forceCallInit);
            if (callInitFunction || forceCallInit) {

                //调用初始化函数
                for (LinuxModule m : modules.values().toArray(new LinuxModule[0])) {
                    // 1. 模块是我们自己加载的目标模块 且 设置 forceCallInit参数为true
                    // 2. 模块本身有一个forceCallInit参数，默认为false
                    boolean forceCall = (forceCallInit && m == module) || m.isForceCallInit();

                    if (callInitFunction) {
                        m.callInitFunction(emulator, forceCall);
                    } else if (forceCall) {
                        m.callInitFunction(emulator, true);
                    }

                    // 移除该模块下的所有初始化函数
                    m.initFunctionList.clear();

                }
            }
            //添加引用计数
            module.addReferenceCount();
            return module;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void resolveSymbols(boolean showWarning) throws IOException {
        Collection<LinuxModule> linuxModules = modules.values();
        for (LinuxModule m : linuxModules) {
            //遍历还未重定位的符号
            for (Iterator<ModuleSymbol> iterator = m.getUnresolvedSymbol().iterator(); iterator.hasNext(); ) {
                ModuleSymbol moduleSymbol = iterator.next();
                ModuleSymbol resolved = moduleSymbol.resolve(new HashSet<Module>(linuxModules), true, hookListeners, emulator.getSvcMemory());
                if (resolved != null) {
                    log.debug("[" + moduleSymbol.soName + "]" + moduleSymbol.symbol.getName() + " symbol resolved to " + resolved.toSoName);
                    resolved.relocation(emulator, m);
                    iterator.remove();
                } else if (showWarning) {
                    log.info("[" + moduleSymbol.soName + "]symbol " + moduleSymbol.symbol + " is missing relocationAddr=" + moduleSymbol.relocationAddr + ", offset=0x" + Long.toHexString(moduleSymbol.offset));
                }
            }
        }
    }

    @Override
    public Module dlopen(String filename, boolean calInit) {
        LinuxModule loaded = modules.get(FilenameUtils.getName(filename));
        if (loaded != null) {
            loaded.addReferenceCount();
            return loaded;
        }

        for (Module module : getLoadedModules()) {
            for (MemRegion memRegion : module.getRegions()) {
                if (filename.equals(memRegion.getName())) {
                    module.addReferenceCount();
                    return module;
                }
            }
        }

        LibraryFile file = libraryResolver == null ? null : libraryResolver.resolveLibrary(emulator, filename);
        if (file == null) {
            return null;
        }

        if (calInit) {
            return loadInternal(file, false);
        }

        try {
            LinuxModule module = loadInternal(file);
            resolveSymbols(false);
            if (!callInitFunction) { // No need call init array
                for (LinuxModule m : modules.values()) {
                    m.initFunctionList.clear();
                }
            }
            module.addReferenceCount();
            return module;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * dlopen调用init_array会崩溃
     */
    @Override
    public Module dlopen(String filename) {
        return dlopen(filename, true);
    }

    private final UnidbgPointer environ;

    private static final int RTLD_DEFAULT = -1;

    @Override
    public Symbol dlsym(long handle, String symbolName) {
        if ("environ".equals(symbolName)) {
            return new VirtualSymbol(symbolName, null, environ.toUIntPeer());
        }
        Module sm = null;
        Symbol ret = null;
        for (LinuxModule module : modules.values()) {
            if (module.base == handle) { // virtual module may have same base address
                Symbol symbol = module.findSymbolByName(symbolName, false);
                if (symbol != null) {
                    ret = symbol;
                    sm = module;
                    break;
                }
            }
        }
        if (ret == null && ((int) handle == RTLD_DEFAULT || handle == 0L)) {
            for (Module module : modules.values()) {
                Symbol symbol = module.findSymbolByName(symbolName, false);
                if (symbol != null) {
                    ret = symbol;
                    sm = module;
                    break;
                }
            }
        }
        for (HookListener listener : hookListeners) {
            long hook = listener.hook(emulator.getSvcMemory(), sm == null ? null : sm.name, symbolName, ret == null ? 0L : ret.getAddress());
            if (hook != 0) {
                return new VirtualSymbol(symbolName, null, hook);
            }
        }
        return ret;
    }

    @Override
    public boolean dlclose(long handle) {
        for (Iterator<Map.Entry<String, LinuxModule>> iterator = modules.entrySet().iterator(); iterator.hasNext(); ) {
            LinuxModule module = iterator.next().getValue();
            if (module.base == handle) {
                if (module.decrementReferenceCount() <= 0) {
                    module.unload(backend);
                    iterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    private LinuxModule loadInternal(LibraryFile libraryFile) throws IOException {
        // 将我们的So文件让ElfFile类去解析，这个ElfFile是jelf库经过凯神改装过的，可以帮助解析Elf文件
        final ElfFile elfFile = ElfFile.fromBuffer(libraryFile.mapBuffer());

        if (emulator.is32Bit() && elfFile.objectSize != ElfFile.CLASS_32) {
            throw new ElfException("Must be 32-bit");
        }
        if (emulator.is64Bit() && elfFile.objectSize != ElfFile.CLASS_64) {
            throw new ElfException("Must be 64-bit");
        }

        if (elfFile.encoding != ElfFile.DATA_LSB) {
            throw new ElfException("Must be LSB");
        }

        if (emulator.is32Bit() && elfFile.arch != ElfFile.ARCH_ARM) {
            throw new ElfException("Must be ARM arch.");
        }

        if (emulator.is64Bit() && elfFile.arch != ElfFile.ARCH_AARCH64) {
            throw new ElfException("Must be ARM64 arch.");
        }

        long start = System.currentTimeMillis();

        // 获取当前So的最大虚拟地址
        long bound_high = 0;
        // 页对齐align参数
        long align = 0;

        for (int i = 0; i < elfFile.num_ph; i++) {
            ElfSegment ph = elfFile.getProgramHeader(i);
            // 遍历所有mem_size>0的PT_LOAD段
            if (ph.type == ElfSegment.PT_LOAD && ph.mem_size > 0) {
                long high = ph.virtual_address + ph.mem_size;

                if (bound_high < high) {
                    bound_high = high;
                }
                if (ph.alignment > align) {
                    align = ph.alignment;
                }
            }
        }

        ElfDynamicStructure dynamicStructure = null;

        //页单位。从获取到的So指定的alignment和默认的PageAlign取一个最大值，一般拿到的就是4K大小(4096)
        final long baseAlign = Math.max(emulator.getPageAlign(), align);
        //手动获取(多个PT_LOAD)起始页的首地址，根据baseAlign来计算该So的加载地址。初始地址0x40000000L
        final long load_base = ((mmapBaseAddress - 1) / baseAlign + 1) * baseAlign;


        //页对齐后的大小(多个PT_LOAD段所占空间大小)
        //这个就相当于Linker在计算load_size，但Unidbg中将所有So的最小虚拟地址默认为0
        //这里有改进空间对吧，因为Linker中为了防止内存浪费，出现了一个load_bias_字段
        //但是出于目的不用，Unidbg的目的是让二进制文件跑起来
        long load_virtual_address = 0;
        long size = ARM.align(0, bound_high, baseAlign).size;

        //重新设置基址
        //设置加载下个So的mmapBaseAddress
        setMMapBaseAddress(load_base + size);

        //MemRegion存储了哪块内存对应了哪个So文件
        //regions存放加载的Segment
        final List<MemRegion> regions = new ArrayList<>(5);
        MemoizedObject<ArmExIdx> armExIdx = null;
        MemoizedObject<GnuEhFrameHeader> ehFrameHeader = null;
        Alignment lastAlignment = null;

        //再次遍历所有段
        for (int i = 0; i < elfFile.num_ph; i++) {
            ElfSegment ph = elfFile.getProgramHeader(i);
            switch (ph.type) {
                case ElfSegment.PT_LOAD:
                    // 获取该段在内存中对应的操作权限，如该段未指定，设置满权限(一般不会出现这种情况)
                    int prot = get_segment_protection(ph.flags);
                    if (prot == UnicornConst.UC_PROT_NONE) {
                        prot = UnicornConst.UC_PROT_ALL;
                    }
                    // 该段在内存中的起始地址
                    final long begin = load_base + ph.virtual_address;
                    if (load_virtual_address == 0) {
                        load_virtual_address = begin;
                    }
                    // 计算该段(Segment)在内存中的位置和大小
                    Alignment check = ARM.align(begin, ph.mem_size, Math.max(emulator.getPageAlign(), ph.alignment));

                    // 获取上一个内存块
                    final int regionSize = regions.size();
                    MemRegion last = regionSize == 0 ? null : regions.get(regionSize - 1);
                    MemRegion overall = null;
                    if (last != null && check.address >= last.begin && check.address < last.end) {
                        overall = last;
                    }
                    // 处理重叠段，应该为特殊情况，正常都会走下面else分支
                    if (overall != null) {
                        long overallSize = overall.end - check.address;
                        int perms = overall.perms | prot;
                        if (mMapListener != null) {
                            perms = mMapListener.onProtect(check.address, overallSize, perms);
                        }
                        backend.mem_protect(check.address, overallSize, perms);
                        if (ph.mem_size > overallSize) {
                            Alignment alignment = this.mem_map(begin + overallSize, ph.mem_size - overallSize, prot, libraryFile.getName(), Math.max(emulator.getPageAlign(), ph.alignment));
                            regions.add(new MemRegion(begin, alignment.address, alignment.address + alignment.size, prot, libraryFile, ph.virtual_address));
                            if (lastAlignment != null && lastAlignment.begin + lastAlignment.dataSize > begin) {
                                throw new UnsupportedOperationException();
                            }
                            lastAlignment = alignment;
                            lastAlignment.begin = begin;
                        }
                    } else {
                        // 开辟内存，将该PT_LOAD段指示的内存大小进行映射
                        // 【*】this.mem_map方法中调用了：memoryMap.put()
                        Alignment alignment = this.mem_map(begin, ph.mem_size, prot, libraryFile.getName(), Math.max(emulator.getPageAlign(), ph.alignment));
                        // 添加一块MemRegion
                        regions.add(new MemRegion(begin, alignment.address, alignment.address + alignment.size, prot, libraryFile, ph.virtual_address));
                        if (lastAlignment != null) {
                            //上一个段的末尾
                            long base = lastAlignment.address + lastAlignment.size;
                            long off = alignment.address - base;
                            if (off < 0) {
                                throw new IllegalStateException();
                            }
                            if (off > 0) {
                                // 处理该段(Segment)与上一个段(Segment)之间的空隙，置0
                                backend.mem_map(base, off, UnicornConst.UC_PROT_NONE);
                                if (mMapListener != null) {
                                    mMapListener.onMap(base, off, UnicornConst.UC_PROT_NONE);
                                }
                                if (memoryMap.put(base, new MemoryMap(base, (int) off, UnicornConst.UC_PROT_NONE)) != null) {
                                    log.warn("mem_map replace exists memory map base=" + Long.toHexString(base));
                                }
                            }
                        }
                        lastAlignment = alignment;
                        lastAlignment.begin = begin;
                    }

                    // 将该段对应的数据写入进已经开辟好的内存
                    PtLoadData loadData = ph.getPtLoadData();
                    loadData.writeTo(pointer(begin));
                    if (lastAlignment != null) {
                        lastAlignment.dataSize = loadData.getDataSize();
                    }
                    break;
                case ElfSegment.PT_DYNAMIC:
                    dynamicStructure = ph.getDynamicStructure();
                    break;
                case ElfSegment.PT_INTERP:
                    // INTERP段指定了解释器位置，在So中没用
                    if (log.isDebugEnabled()) {
                        log.debug("[" + libraryFile.getName() + "]interp=" + ph.getInterpreter());
                    }
                    break;
                case ElfSegment.PT_GNU_EH_FRAME:
                    // 没分析过，未知TODO
                    ehFrameHeader = ph.getEhFrameHeader();
                    break;
                case ElfSegment.PT_ARM_EXIDX:
                    // 异常相关的段
                    armExIdx = ph.getARMExIdxData();
                    break;
                default:
                    if (log.isDebugEnabled()) {
                        log.debug("[" + libraryFile.getName() + "]segment type=0x" + Integer.toHexString(ph.type) + ", offset=0x" + Long.toHexString(ph.offset));
                    }
                    break;
            }
        }

        // 此时，该So中的有用的段信息已经处理完毕
        // 该加载到内存的已经加载到内存
        // 该置空的内存也已置空
        // 动态段、异常段、PT_GNU_EH_FRAME段的信息已经保存下来，继续看接下来的处理

        // 动态段是必须有的
        if (dynamicStructure == null) {
            throw new IllegalStateException("dynamicStructure is empty.");
        }
        // 此SoName是动态段中的tag为SO_NAME指定的内容，而且Unidbg中的Log也是基于这个SoName打印的
        // 如果该内容为空，才会使用文件名。这也就是有的同学会问为什么我加载的是libxxxxx.so，而日志输出libyyyyyy.so呢
        final String soName = dynamicStructure.getSOName(libraryFile.getName());

        // 处理依赖，对依赖库进行加载
        Map<String, Module> neededLibraries = new HashMap<>();
        for (String neededLibrary : dynamicStructure.getNeededLibraries()) {
            if (log.isDebugEnabled()) {
                log.debug(soName + " need dependency " + neededLibrary);
            }

            // modules字段保存了所有已经加载过的库，这里就是在寻找是否该So已经被加载过
            LinuxModule loaded = modules.get(neededLibrary);
            if (loaded != null) {
                loaded.addReferenceCount();
                neededLibraries.put(FilenameUtils.getBaseName(loaded.name), loaded);
                continue;
            }
            // 如果依赖还没有被加载过，就开始寻找这个依赖文件在哪，先在当前So的路径下找
            LibraryFile neededLibraryFile = libraryFile.resolveLibrary(emulator, neededLibrary);
            // 如果当前路径下没有找到，就去找library解析器去找
            // 即，如果依赖so没有找到的情况，那么去/android/sdk[n]/lib(32|64)/ 目录下去找
            // 即，memory.setLibraryResolver(new AndroidResolver(23)); 的作用生效
            if (libraryResolver != null && neededLibraryFile == null) {
                neededLibraryFile = libraryResolver.resolveLibrary(emulator, neededLibrary);
            }

            // So找到啦，就会在这里加载
            if (neededLibraryFile != null) {
                // 加载依赖
                LinuxModule needed = loadInternal(neededLibraryFile);
                needed.addReferenceCount();
                neededLibraries.put(FilenameUtils.getBaseName(needed.name), needed);
            } else {
                log.info(soName + " load dependency " + neededLibrary + " failed");
            }
        }

        // 符号处理
        // 下面这个循环会处理未解决(符号为0特殊情况)的重定位，进行二次重定位，极少数能成功，如果确定没用可以注释掉
        for (LinuxModule module : modules.values()) {
            for (Iterator<ModuleSymbol> iterator = module.getUnresolvedSymbol().iterator(); iterator.hasNext(); ) {
                ModuleSymbol moduleSymbol = iterator.next();
                ModuleSymbol resolved = moduleSymbol.resolve(module.getNeededLibraries(), false, hookListeners, emulator.getSvcMemory());
                if (resolved != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("[" + moduleSymbol.soName + "]" + moduleSymbol.symbol.getName() + " symbol resolved to " + resolved.toSoName);
                    }
                    resolved.relocation(emulator, module);
                    iterator.remove();
                }
            }
        }


        //重定位
        List<ModuleSymbol> list = new ArrayList<>();
        List<ModuleSymbol> resolvedSymbols = new ArrayList<>();
        //遍历重定位表
        for (MemoizedObject<ElfRelocation> object : dynamicStructure.getRelocations()) {
            ElfRelocation relocation = object.getValue();
            // 拿到重定位类型
            final int type = relocation.type();
            if (type == 0) {
                log.warn("Unhandled relocation type " + type);
                continue;
            }
            // 拿到重定位项指定的符号信息
            ElfSymbol symbol = relocation.sym() == 0 ? null : relocation.symbol();
            long sym_value = symbol != null ? symbol.value : 0;


            // 计算需要重定位的位置(重定位入口)
            Pointer relocationAddr = UnidbgPointer.pointer(emulator, load_base + relocation.offset());
            assert relocationAddr != null;

            Log log = LogFactory.getLog("com.github.unidbg.linux." + soName);
            if (log.isDebugEnabled()) {
                log.debug("symbol=" + symbol + ", type=" + type + ", relocationAddr=" + relocationAddr + ", offset=0x" + Long.toHexString(relocation.offset()) + ", addend=" + relocation.addend() + ", sym=" + relocation.sym() + ", android=" + relocation.isAndroid());
            }

            ModuleSymbol moduleSymbol;
            // 根据重定位类型进行不同的处理，下面包含了32位/64位下的重定位处理
            switch (type) {
                case ARMEmulator.R_ARM_ABS32: {
                    int offset = relocationAddr.getInt(0);
                    moduleSymbol = resolveSymbol(load_base, symbol, relocationAddr, soName, neededLibraries.values(), offset);
                    if (moduleSymbol == null) {
                        // 不能当即处理的，添加到list，后面再处
                        list.add(new ModuleSymbol(soName, load_base, symbol, relocationAddr, null, offset));
                    } else {
                        resolvedSymbols.add(moduleSymbol);
                    }
                    break;
                }
                case ARMEmulator.R_AARCH64_ABS64: {
                    long offset = relocationAddr.getLong(0) + relocation.addend();
                    moduleSymbol = resolveSymbol(load_base, symbol, relocationAddr, soName, neededLibraries.values(), offset);
                    if (moduleSymbol == null) {
                        list.add(new ModuleSymbol(soName, load_base, symbol, relocationAddr, null, offset));
                    } else {
                        resolvedSymbols.add(moduleSymbol);
                    }
                    break;
                }
                case ARMEmulator.R_ARM_RELATIVE: {
                    int offset = relocationAddr.getInt(0);
                    if (sym_value == 0) {
                        relocationAddr.setInt(0, (int) load_base + offset);
                    } else {
                        throw new IllegalStateException("sym_value=0x" + Long.toHexString(sym_value));
                    }
                    break;
                }
                case ARMEmulator.R_AARCH64_RELATIVE:
                    if (sym_value == 0) {
                        relocationAddr.setLong(0, load_base + relocation.addend());
                    } else {
                        throw new IllegalStateException("sym_value=0x" + Long.toHexString(sym_value));
                    }
                    break;
                case ARMEmulator.R_ARM_GLOB_DAT:
                case ARMEmulator.R_ARM_JUMP_SLOT:
                    moduleSymbol = resolveSymbol(load_base, symbol, relocationAddr, soName, neededLibraries.values(), 0);
                    if (moduleSymbol == null) {
                        list.add(new ModuleSymbol(soName, load_base, symbol, relocationAddr, null, 0));
                    } else {
                        resolvedSymbols.add(moduleSymbol);
                    }
                    break;
                case ARMEmulator.R_AARCH64_GLOB_DAT:
                case ARMEmulator.R_AARCH64_JUMP_SLOT:
                    moduleSymbol = resolveSymbol(load_base, symbol, relocationAddr, soName, neededLibraries.values(), relocation.addend());
                    if (moduleSymbol == null) {
                        list.add(new ModuleSymbol(soName, load_base, symbol, relocationAddr, null, relocation.addend()));
                    } else {
                        resolvedSymbols.add(moduleSymbol);
                    }
                    break;
                case ARMEmulator.R_ARM_COPY:
                    throw new IllegalStateException("R_ARM_COPY relocations are not supported");
                case ARMEmulator.R_AARCH64_COPY:
                    throw new IllegalStateException("R_AARCH64_COPY relocations are not supported");
                case ARMEmulator.R_AARCH64_ABS32:
                case ARMEmulator.R_AARCH64_ABS16:
                case ARMEmulator.R_AARCH64_PREL64:
                case ARMEmulator.R_AARCH64_PREL32:
                case ARMEmulator.R_AARCH64_PREL16:
                case ARMEmulator.R_AARCH64_IRELATIVE:
                case ARMEmulator.R_AARCH64_TLS_TPREL64:
                case ARMEmulator.R_AARCH64_TLS_DTPREL32:
                case ARMEmulator.R_ARM_IRELATIVE:
                case ARMEmulator.R_ARM_REL32:
                default:
                    log.warn("[" + soName + "]Unhandled relocation type " + type + ", symbol=" + symbol + ", relocationAddr=" + relocationAddr + ", offset=0x" + Long.toHexString(relocation.offset()) + ", addend=" + relocation.addend() + ", android=" + relocation.isAndroid());
                    break;
            }
        }

        //重定位完成后，开始执行初始化函数
        List<InitFunction> initFunctionList = new ArrayList<>();
        int preInitArraySize = dynamicStructure.getPreInitArraySize();
        boolean executable = elfFile.file_type == ElfFile.FT_EXEC || preInitArraySize > 0;

        // 处理可执行文件相关，我们分析So的，忽略就可以
        if (executable) {
            int count = preInitArraySize / emulator.getPointerSize();
            if (count > 0) {
                UnidbgPointer pointer = UnidbgPointer.pointer(emulator, load_base + dynamicStructure.getPreInitArrayOffset());
                if (pointer == null) {
                    throw new IllegalStateException("DT_PREINIT_ARRAY is null");
                }
                for (int i = 0; i < count; i++) {
                    UnidbgPointer ptr = pointer.share((long) i * emulator.getPointerSize(), 0);
                    initFunctionList.add(new AbsoluteInitFunction(load_base, soName, ptr));
                }
            }
        }
        //处理So的初始化函数
        //下面的处理内容在新版有修复，我们之前Linker的文章也讲过，他们的  顺序不应该是平级的，需要Init函数先执行
        if (elfFile.file_type == ElfFile.FT_DYN) { // not executable
            int init = dynamicStructure.getInit();
            if (init != 0) {
                //处理init
                initFunctionList.add(new LinuxInitFunction(load_base, soName, init));
            }

            int initArraySize = dynamicStructure.getInitArraySize();
            int count = initArraySize / emulator.getPointerSize();
            if (count > 0) {
                UnidbgPointer pointer = UnidbgPointer.pointer(emulator, load_base + dynamicStructure.getInitArrayOffset());
                if (pointer == null) {
                    throw new IllegalStateException("DT_INIT_ARRAY is null");
                }
                //处理init_array
                for (int i = 0; i < count; i++) {
                    UnidbgPointer ptr = pointer.share((long) i * emulator.getPointerSize(), 0);
                    initFunctionList.add(new AbsoluteInitFunction(load_base, soName, ptr));
                }
            }
        }

        // 至此，依赖So加载了，重定位可以处理的也处理了(不能处理的还会有二次处理)
        // 初始化函数也被添加到列表中了，但是还没有调用(注意)
        SymbolLocator dynsym = dynamicStructure.getSymbolStructure();
        if (dynsym == null) {
            throw new IllegalStateException("dynsym is null");
        }
        ElfSection symbolTableSection = null;
        try {
            symbolTableSection = elfFile.getSymbolTableSection();
        } catch (Throwable ignored) {
        }
        if (load_virtual_address == 0) {
            throw new IllegalStateException("load_virtual_address");
        }

        // 将加载好的So封装为LinuxModule对象
        LinuxModule module = new LinuxModule(load_virtual_address, load_base, size, soName, dynsym, list, initFunctionList, neededLibraries, regions,
                armExIdx, ehFrameHeader, symbolTableSection, elfFile, dynamicStructure, libraryFile);
        for (ModuleSymbol symbol : resolvedSymbols) {
            //TODO 真正重定位的地方
            symbol.relocation(emulator, module);
        }
        if (executable) {
            for (LinuxModule linuxModule : modules.values()) {
                for (Map.Entry<String, ModuleSymbol> entry : linuxModule.resolvedSymbols.entrySet()) {
                    ElfSymbol symbol = module.getELFSymbolByName(entry.getKey());
                    if (symbol != null && !symbol.isUndef()) {
                        entry.getValue().relocation(emulator, module, symbol);
                    }
                }
                linuxModule.resolvedSymbols.clear();
            }
        }
        if ("libc.so".equals(soName)) { // libc
            malloc = module.findSymbolByName("malloc", false);
            free = module.findSymbolByName("free", false);
        }

        // 放入已加载的So列表中
        modules.put(soName, module);
        if (maxSoName == null || soName.length() > maxSoName.length()) {
            maxSoName = soName;
        }
        if (bound_high > maxSizeOfSo) {
            maxSizeOfSo = bound_high;
        }
        // 设置可执行Elf的入口点
        module.setEntryPoint(elfFile.entry_point);
        log.debug("Load library " + soName + " offset=" + (System.currentTimeMillis() - start) + "ms" + ", entry_point=0x" + Long.toHexString(elfFile.entry_point));
        // 通知监听器，So已加载完毕
        notifyModuleLoaded(module);
        return module;
    }

    //加载虚拟模块
    @Override
    public Module loadVirtualModule(String name, Map<String, UnidbgPointer> symbols) {
        LinuxModule module = LinuxModule.createVirtualModule(name, symbols, emulator);
        modules.put(name, module);
        if (maxSoName == null || name.length() > maxSoName.length()) {
            maxSoName = name;
        }
        return module;
    }

    private String maxSoName;
    private long maxSizeOfSo;

    private ModuleSymbol resolveSymbol(long load_base, ElfSymbol symbol, Pointer relocationAddr, String soName, Collection<Module> neededLibraries, long offset) throws IOException {
        if (symbol == null) {
            return new ModuleSymbol(soName, load_base, null, relocationAddr, soName, offset);
        }

        if (!symbol.isUndef()) {
            for (HookListener listener : hookListeners) {
                long hook = listener.hook(emulator.getSvcMemory(), soName, symbol.getName(), load_base + symbol.value + offset);
                if (hook > 0) {
                    return new ModuleSymbol(soName, ModuleSymbol.WEAK_BASE, symbol, relocationAddr, soName, hook);
                }
            }
            return new ModuleSymbol(soName, load_base, symbol, relocationAddr, soName, offset);
        }

        return new ModuleSymbol(soName, load_base, symbol, relocationAddr, null, offset).resolve(neededLibraries, false, hookListeners, emulator.getSvcMemory());
    }

    private int get_segment_protection(int flags) {
        int prot = Unicorn.UC_PROT_NONE;
        if ((flags & ElfSegment.PF_R) != 0) prot |= Unicorn.UC_PROT_READ;
        if ((flags & ElfSegment.PF_W) != 0) prot |= Unicorn.UC_PROT_WRITE;
        if ((flags & ElfSegment.PF_X) != 0) prot |= Unicorn.UC_PROT_EXEC;
        return prot;
    }

    @Override
    public MemoryBlock malloc(int length, boolean runtime) {
        if (runtime) {
            return MemoryBlockImpl.alloc(this, length);
        } else {
            //调用lib.c中的方法
            return MemoryAllocBlock.malloc(emulator, malloc, free, length);
        }
    }

    private static final long HEAP_BASE = 0x8048000;
    private long brk;

    @Override
    public int brk(long address) {
        if (address == 0) {
            this.brk = HEAP_BASE;
            return (int) this.brk;
        }

        if (address % emulator.getPageAlign() != 0) {
            throw new UnsupportedOperationException();
        }

        if (address > brk) {
            backend.mem_map(brk, address - brk, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);
            if (mMapListener != null) {
                mMapListener.onMap(brk, address - brk, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);
            }
            this.brk = address;
        } else if (address < brk) {
            backend.mem_unmap(address, brk - address);
            if (mMapListener != null) {
                mMapListener.onUnmap(address, brk - address);
            }
            this.brk = address;
        }

        return (int) this.brk;
    }

    private static final int MAP_FAILED = -1;
    public static final int MAP_FIXED = 0x10;
    public static final int MAP_ANONYMOUS = 0x20;

    @Override
    public long mmap2(long start, int length, int prot, int flags, int fd, int offset) {
        int aligned = (int) ARM.alignSize(length, emulator.getPageAlign());

        boolean isAnonymous = ((flags & MAP_ANONYMOUS) != 0) || (start == 0 && fd <= 0 && offset == 0);
        if ((flags & MAP_FIXED) != 0 && isAnonymous) {
            if (log.isDebugEnabled()) {
                log.debug("mmap2 MAP_FIXED start=0x" + Long.toHexString(start) + ", length=" + length + ", prot=" + prot);
            }

            munmap(start, length);
            backend.mem_map(start, aligned, prot);
            if (mMapListener != null) {
                mMapListener.onMap(start, aligned, prot);
            }
            if (memoryMap.put(start, new MemoryMap(start, aligned, prot)) != null) {
                log.warn("mmap2 replace exists memory map: start=" + Long.toHexString(start));
            }
            return start;
        }
        if (isAnonymous) {
            long addr = allocateMapAddress(0, aligned);
            if (log.isDebugEnabled()) {
                log.debug("mmap2 addr=0x" + Long.toHexString(addr) + ", mmapBaseAddress=0x" + Long.toHexString(mmapBaseAddress) + ", start=" + start + ", fd=" + fd + ", offset=" + offset + ", aligned=" + aligned + ", LR=" + emulator.getContext().getLRPointer());
            }
            backend.mem_map(addr, aligned, prot);
            if (mMapListener != null) {
                mMapListener.onMap(start, aligned, prot);
            }
            if (memoryMap.put(addr, new MemoryMap(addr, aligned, prot)) != null) {
                log.warn("mmap2 replace exists memory map addr=" + Long.toHexString(addr));
            }
            return addr;
        }
        try {
            FileIO file;
            if (start == 0 && fd > 0 && (file = syscallHandler.getFileIO(fd)) != null) {
                long addr = allocateMapAddress(0, aligned);
                if (log.isDebugEnabled()) {
                    log.debug("mmap2 addr=0x" + Long.toHexString(addr) + ", mmapBaseAddress=0x" + Long.toHexString(mmapBaseAddress));
                }
                long ret = file.mmap2(emulator, addr, aligned, prot, offset, length);
                if (mMapListener != null) {
                    mMapListener.onMap(addr, aligned, prot);
                }
                if (memoryMap.put(addr, new MemoryMap(addr, aligned, prot)) != null) {
                    log.warn("mmap2 replace exists memory map addr=0x" + Long.toHexString(addr));
                }
                return ret;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            FileIO file;
            if (fd > 0 && (file = syscallHandler.getFileIO(fd)) != null) {
                if ((start & (emulator.getPageAlign() - 1)) != 0) {
                    return MAP_FAILED;
                }
                long end = start + length;
                for (Map.Entry<Long, MemoryMap> entry : memoryMap.entrySet()) {
                    MemoryMap map = entry.getValue();
                    if (Math.max(start, entry.getKey()) <= Math.min(map.base + map.size, end)) {
                        return MAP_FAILED;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("mmap2 start=0x" + Long.toHexString(start) + ", mmapBaseAddress=0x" + Long.toHexString(mmapBaseAddress) + ", flags=0x" + Integer.toHexString(flags) + ", length=0x" + Integer.toHexString(length));
                }
                long ret = file.mmap2(emulator, start, aligned, prot, offset, length);
                if (mMapListener != null) {
                    mMapListener.onMap(start, aligned, prot);
                }
                if (memoryMap.put(start, new MemoryMap(start, aligned, prot)) != null) {
                    log.warn("mmap2 replace exists memory map start=0x" + Long.toHexString(start));
                }
                return ret;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        emulator.attach().debug();
        throw new AbstractMethodError("mmap2 start=0x" + Long.toHexString(start) + ", length=" + length + ", prot=0x" + Integer.toHexString(prot) + ", flags=0x" + Integer.toHexString(flags) + ", fd=" + fd + ", offset=" + offset);
    }

    private Pointer errno;

    private int lastErrno;

    @Override
    public int getLastErrno() {
        return lastErrno;
    }

    @Override
    public void setErrno(int errno) {
        this.lastErrno = errno;
        Task task = emulator.get(Task.TASK_KEY);
        if (task != null && task.setErrno(emulator, errno)) {
            return;
        }
        this.errno.setInt(0, errno);
    }

    @Override
    public String getMaxLengthLibraryName() {
        return maxSoName;
    }

    @Override
    public long getMaxSizeOfLibrary() {
        return maxSizeOfSo;
    }

    @Override
    public Collection<Module> getLoadedModules() {
        return new ArrayList<Module>(modules.values());
    }
}
