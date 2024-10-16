package com.zsxqstar.u2fileaccess;

import com.github.unidbg.Emulator;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.unix.UnixEmulator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


/*
 *
 * 文件访问基本处理，它的知识由三部分构成:
 *   Unidbg对应的实现和映射
 *   Linux/Android文件系统的相关知识
 *   对业务的经验和知识
 *
 *
 * 对文件访问有 success、failed、fallback 三种处理
        ● success 表示文件顺利访问，参数是 NewFileIO。
        ● failed 表示文件访问失败，参数是 erron 错误码。
        ● fallback 表示回退、降级，只有在其他处理器无法处理对该文件的访问时，才由 fallback 指定的文件 IO 来处理
 *
 * */
public class BiliIOResolver implements IOResolver<AndroidFileIO> {
    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        //参数二: 文件路径
        //参数三: 操作文件标志

        System.out.println(">>>> lilac open:" + pathname);

        /*
        *
        * TODO 文件访问(一)
        *
        * */
        /*
        [一]
        [二]
        [三]：文件访问基本处理
        Unidbg对应的实现和映射：像下面这样补它
            对业务的经验：boot_id 常用于构建设备指纹或单纯的信息采集，需要补它
            Linux/Android文件系统的知识：boot_id 文件在开机时生成，在设备关机前不会改变内容。我们将这个文件从真机上push出来
        */
        switch (pathname) {
            case "/proc/sys/kernel/random/boot_id": {
                //return FileResult.fallback()

                //1处理文件
                //1.1 SimpleFileIO 是最常使用的 IO 类型，它代表普通文件
                //return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/dewu/files/cpu/boot_id"), pathname));

                //1.2 读者可能会想一个问题，boot_id 在一定程度上用于标识设备，那么在生产环境中，我们需要对它做随机化以躲避风控。使用 SimpleFileIO 就不太好处理这个问题，这时候可以用 ByteArrayFileIO。
                //副作用：没办法做文件做写入操作，ByteArrayFileIO 像浮萍漂泊，无根无依。
                return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

                //2处理目录
                //2.1 如果样本访问的是一个目录呢？这时候要用 DirectoryFileIO，File 传入文件夹即可。
                // 但对于文件夹访问而言，使用虚拟文件系统是更好的选择。首先看前文《创建模拟器和加载模块》的 2-4 小节，设置好虚拟目录的根目录。然后将待处理的文件按照层级放到对应位置。
                //return FileResult.<AndroidFileIO>success(new DirectoryFileIO(oflags, pathname, new File("unidbg-android/src/test/resources/meituan/data")));
            }

            //处理文件和目录之常见案例
            //比如 补apk，在任何情况下，都不应该对访问apk忽略不管，因为它往往用于资源读取或签名校验，不补风险很大。
            case "/data/app/com.jingdong.app.mall-jJGgNO9r6uKMLj1ytVwSRw==/base.apk": {
                return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/jd/jd.apk"), pathname));
            }
            //补CPU信息
            // https://bbs.pediy.com/thread-229579-1.htm
            case "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq": {
                return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, "1766400".getBytes()));
            }
            //补电池信息
            case "/sys/class/power_supply/battery/temp": {
                return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/meituan/files/battery/temp"), pathname));
            }
            case "/sys/class/power_supply/battery/voltage_now": {
                return FileResult.<AndroidFileIO>success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/meituan/files/battery/voltage_now"), pathname));
            }

            /* TODO 重点
            * 接下来说说 failed，它表示文件访问失败，使用场景其实也不多。因为只要我们什么都不补，AndroidResolver 里没有处理这个文件，
            * 虚拟文件系统中也不包含它，那就自动访问失败 + 设置错误码。
            * 但有个情况比较例外——如果文件访问 /data，【判断是否有写权限，进而确认设备环境是否Root的话】，我们需要手动处理。
            * */
            case "/data": {
                return FileResult.failed(UnixEmulator.EACCES);
            }


            /* [四]：环境检测
             * 当样本做环境检测相关的文件访问时，主要检测这些文件是否存在，以及是否有权限
             *  ● Root检测（检测 su、Magisk、Riru，检测市面上的 Root 工具）
                ● 模拟器检测（检测 Qemu，检测各家模拟器，比如夜神、雷电、Mumu 等模拟器的文件特征、驱动特征等）
                ● 危险应用检测（各类多开助手、按键精灵、接码平台等）
                ● 云手机检测 （以各种云手机产品为主）
                ● Hook框架（以 Xposed、Substrate、Frida 为主）
                ● 脱壳机（以 Fart、DexHunter、Youpk 三者为主）
             * 我们选择什么都不补就行，因为不管是自定义还是默认的文件处理器，以及虚拟文件系统里，都不会有这样内容，这正合我们的意。就像下样，什么都不补就行了。
             * */
        }

        /*
        *
        *
        * TODO 文件访问(二)  proc 伪文件系统
        *
        * */

        /*
         2.1 cmdline，
         /proc/self(pid)/cmdline 用于查看自身进程名，常用于信息收集或检测运行环境是否有异。
         */
        //查看当前进程名
        //采用emulator.getPid()而非硬编码
        //对于进程名这个需求，没必要从真机拷贝出对应文件，直接 cat 查看，然后用 ByteArrayFileIO 处理就很方便，结尾记得加上\0，这是 cmdline 的格式规范，不加的话在解析时存在出错的可能性。
        if (("/proc/" + emulator.getPid() + "/cmdline").equals(pathname) || ("/proc/self/cmdline").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "process-name\0".getBytes()));
        }

        /*
         2.2 status
         /proc/self(pid)/status 最主要用来检测 tracepid 状态，因此偷懒的话可以像下面这样干
         if (("proc/" + emulator.getPid() + "/status").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, ("TracerPid: 0").getBytes()));
        }
        */
        //上面那么做并非毫无风险，样本也可能会校验和关注其他字段，所以像下面这样补更规范一些。
        if (pathname.equals("proc/" + emulator.getPid() + "/status")) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname,
                    ("Name:   ip.android.view\n" +
                            "Umask:  0077\n" +
                            "State:  S (sleeping)\n" +
                            "Tgid:   " + emulator.getPid() + "\n" +
                            "Ngid:   0\n" +
                            "Pid:    " + emulator.getPid() + "\n" +
                            "PPid:   6119\n" +
                            "TracerPid:      0\n" +
                            "Uid:    10494   10494   10494   10494\n" +
                            "Gid:    10494   10494   10494   10494\n" +
                            "FDSize: 512\n" +
                            "Groups: 3002 3003 9997 20494 50494 99909997\n" +
                            "VmPeak:  2543892 kB\n" +
                            "VmSize:  2466524 kB\n" +
                            "VmLck:         0 kB\n" +
                            "VmPin:         0 kB\n" +
                            "VmHWM:    475128 kB\n" +
                            "VmRSS:    415548 kB\n" +
                            "RssAnon:          144072 kB\n" +
                            "RssFile:          267216 kB\n" +
                            "RssShmem:           4260 kB\n" +
                            "VmData:  1488008 kB\n" +
                            "VmStk:      8192 kB\n" +
                            "VmExe:        20 kB\n" +
                            "VmLib:    239368 kB\n" +
                            "VmPTE:      2360 kB\n" +
                            "VmPMD:        16 kB\n" +
                            "VmSwap:    13708 kB\n" +
                            "Threads:        122\n" +
                            "SigQ:   0/21555\n" +
                            "SigPnd: 0000000000000000\n" +
                            "ShdPnd: 0000000000000000\n" +
                            "SigBlk: 0000000080001204\n" +
                            "SigIgn: 0000000000000001\n" +
                            "SigCgt: 0000000e400096fc\n" +
                            "CapInh: 0000000000000000\n" +
                            "CapPrm: 0000000000000000\n" +
                            "CapEff: 0000000000000000\n" +
                            "CapBnd: 0000000000000000\n" +
                            "CapAmb: 0000000000000000\n" +
                            "Seccomp:        2\n" +
                            "Speculation_Store_Bypass:       unknown\n" +
                            "Cpus_allowed:   07\n" +
                            "Cpus_allowed_list:      0-2\n" +
                            "Mems_allowed:   1\n" +
                            "Mems_allowed_list:      0\n" +
                            "voluntary_ctxt_switches:        17290\n" +
                            "nonvoluntary_ctxt_switches:     10433").getBytes(StandardCharsets.UTF_8)));
        }


        /*
          2.3 net
          /proc/net 下的文件一律不要补，有两个理由。
            一是 Google 禁止普通进程访问该目录，这一规定对 Android 10 以及更高的版本均有效，所以我们伪装成高版本，多一事不如少一事，少补几个文件而且无副作用，岂不美哉。
            二是这个目录主要用于检测环境，比如 IDA/Frida Server 的端口检测。将真机的对应文件一股脑 拷贝过来，还可能把风险信息和检测点带过来。
            其中最常见的是下面这几个：
                /proc/net/arp
                /proc/net/tcp
                /proc/net/unix
        */


        /*
          2.4 信息获取
          遇到下面这些文件时，建议正常补，并且了解每个文件的用途，在生产环境上部分需要随机化。
            /proc/meminfo
            /proc/version
            /proc/cpuinfo
            /proc/stat
            /proc/asound/cardX/id
            /proc/self/exe
        */



        /*
            2.5 maps
            /proc/self(pid)/maps 是补 proc 文件访问中最重要的一项，请读者良好处理每一次 maps 访问。
            因为 maps 访问频率高以及地位特殊，Unidbg 中对它做了专门的处理——如果在自定义 IOResolver 中没有得到处理，会在最后的虚拟文件系统这一块进行如下处理
            // src/main/java/com/github/unidbg/file/linux/LinuxFileSystem.java

            MapsFileIO 是 Unidbg 所设计的 fakemaps，它反应了 Unidbg 内存环境中的模块信息，即在语义和格式都等价于真实maps，如下就是libmtguard.so对应的fakemaps。
            客观上讲，这个maps确实能处理一些问题。比如样本访问 maps，检测其中是否有frida/xposed/substrate/magisk/riru等风险模块，
            真实maps会暴露这些信息，而Unidbg fakeMaps很干净、简洁，所以不会有任何问题。

            在内容上，fakeMaps是真实maps的子集，后者包含了许多前者不具备的信息。如果样本试图获取小框外的信息，就会出问题。
            在理论上，我建议遇到对 maps 的访问，就对这部分逻辑做算法分析，完全确定其访问意图，然后反向构造合适的 maps 予以返回。
            在实践上，可能没法总这么干，因为太费事了，那么建议优先使用真实 maps，如果出现内存异常（这意味着样本在基于真实 maps 做内存访问），就使用 fakemaps。
        */

        return null;
    }
}
