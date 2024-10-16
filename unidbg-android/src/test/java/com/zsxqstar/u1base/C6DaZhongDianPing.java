package com.zsxqstar.u1base;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class C6DaZhongDianPing extends AbstractJni implements IOResolver {

    private final AndroidEmulator emulator;

    private final VM vm;

    private final DvmClass SIUACollector;
    private final DvmObject<?> instanceSIUACollector;

    public FileInputStream fileInputStream;
    public InputStreamReader inputStreamReader;
    public BufferedReader bufferedReader;
    public File file1;
    public File file2;

    public SimpleDateFormat simpleDateFormat;
    public String apkPath = "D:\\Learning\\Learn_Spider\\unidbg-相关资料\\q6_dazhongdianping\\dazhongdianping.apk";

    public C6DaZhongDianPing() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                //.addBackendFactory(new DynarmicFactory(true))
                .setProcessName("")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setJni(this);
        //vm.setVerbose(true);

        // 使用 libandroid.so 的虚拟模块
        new AndroidModule(emulator, vm).register(memory);
        // 使用 libjnigraphics.so 的虚拟模块
        new JniGraphics(emulator, vm).register(memory);


        DalvikModule dm = vm.loadLibrary("mtguard", true);
        SIUACollector = vm.resolveClass("com/meituan/android/common/mtguard/NBridge$SIUACollector");
        //全是实例方法
        instanceSIUACollector = SIUACollector.newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    public String getEnvironmentInfo() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getEnvironmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getEnvironmentInfoExtra() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getEnvironmentInfoExtra()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getExternalEquipmentInfo() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getExternalEquipmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWEquipmentInfo() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getHWEquipmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWProperty() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getHWProperty()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getHWStatus() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getHWStatus()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getLocationInfo() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getLocationInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getPlatformInfo() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getPlatformInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getUserAction() {
        String result = instanceSIUACollector.callJniMethodObject(emulator, "getUserAction()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public static void main(String[] args) {
        //创建实例
        C6DaZhongDianPing daZhongDianPing = new C6DaZhongDianPing();

        //测试方法1：getEnvironmentInfo
        String ret1 = daZhongDianPing.getEnvironmentInfo();
        System.out.println("===> ret1");
        System.out.println(ret1);

        //测试方法2：getEnvironmentInfoExtra
        String ret2 = daZhongDianPing.getEnvironmentInfoExtra();
        System.out.println("===> ret2");
        System.out.println(ret2);

        //测试方法3：getExternalEquipmentInfo
        String ret3 = daZhongDianPing.getExternalEquipmentInfo();
        System.out.println("===> ret3");
        System.out.println(ret3);


        //测试方法4：getHWEquipmentInfo
        String ret4 = daZhongDianPing.getHWEquipmentInfo();
        System.out.println("===> ret4");
        System.out.println(ret4);

        //测试方法5：getHWProperty
        String ret5 = daZhongDianPing.getHWProperty();
        System.out.println("===> ret5");
        System.out.println(ret5);

        // 测试方法6：getHWStatus
        String ret6 = daZhongDianPing.getHWStatus();
        System.out.println("===> ret6");
        System.out.println(ret6);

        String ret7 = daZhongDianPing.getLocationInfo();
        System.out.println("===> ret7");
        System.out.println(ret7);

        String ret8 = daZhongDianPing.getPlatformInfo();
        System.out.println("===> ret8");
        System.out.println(ret8);

        String ret9 = daZhongDianPing.getUserAction();
        System.out.println("===> ret9");
        System.out.println(ret9);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {

            case "java/lang/StringBuilder->allocObject": {
                //return dvmClass.newObject(new StringBuilder());
                return ProxyDvmObject.createObject(vm, new StringBuilder());
            }
            //这里直接用空的dvmObject占位，这里的 dvmClass 等价于vm.resolveClass("java/io/BufferedReader")。
            case "java/io/BufferedReader->allocObject": {
                return dvmClass.newObject(null);
            }
            case "java/io/InputStreamReader->allocObject": {
                return dvmClass.newObject(signature);
            }
            case "java/io/FileInputStream->allocObject": {
                return dvmClass.newObject(signature);
            }
            case "java/io/File->allocObject": {
                return dvmClass.newObject(signature);
            }
            case "java/text/SimpleDateFormat->allocObject": {
                //TODO 测试
                //return dvmClass.newObject(signature + "ipylei");
                //return dvmClass.newObject(666);
                //TODO 测试2
                //return ProxyDvmObject.createObject(vm, new SimpleDateFormat());
                return dvmClass.newObject(signature);
            }
            case "java/util/Date->allocObject": {
                //return dvmClass.newObject(signature);
                return ProxyDvmObject.createObject(vm, new Date());
            }
        }

        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/StringBuilder-><init>()V": {
                return;
            }
            case "java/io/FileInputStream-><init>(Ljava/lang/String;)V": {
                String name = vaList.getObjectArg(0).getValue().toString();
                if (name.equals("\\proc\\cpuinfo") || name.equals("/proc/cpuinfo")) {
                    name = "unidbg-android\\src\\test\\resources\\dazongdianping\\cpuinfo";
                }
                System.out.println("---> FileInputStream:" + name);
                try {
                    fileInputStream = new FileInputStream(name);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return;
            }
            //这里用到上面的fileInputStream
            case "java/io/InputStreamReader-><init>(Ljava/io/InputStream;)V": {
                inputStreamReader = new InputStreamReader(fileInputStream);
                return;
            }
            case "java/io/BufferedReader-><init>(Ljava/io/Reader;)V": {
                bufferedReader = new BufferedReader(inputStreamReader);
                return;
            }
            case "java/io/BufferedReader->close()V": {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->safeClose(Ljava/io/Closeable;)V": {
                return;
            }
            case "java/io/File-><init>(Ljava/lang/String;)V": {
                String path = vaList.getObjectArg(0).getValue().toString();
                System.out.println("--->filePath: " + path);
                if (path.equals("/sys/class/power_supply/battery/voltage_now")) {
                    file1 = new File("unidbg-android/src/test/resources/dianping/files/voltage_now");
                    return;
                } else if (path.equals("/sys/class/power_supply/battery/temp")) {
                    file2 = new File("unidbg-android/src/test/resources/dianping/files/voltage_now");
                    return;
                }
            }
            case "java/text/SimpleDateFormat-><init>(Ljava/lang/String;Ljava/util/Locale;)V": {
                String pattern = vaList.getObjectArg(0).getValue().toString();
                Locale locale = (Locale) vaList.getObjectArg(1).getValue();
                simpleDateFormat = new SimpleDateFormat(pattern, locale);
                return;
            }
            case "java/util/Date-><init>()V": {
                return;
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getEnvironmentInfo()Ljava/lang/String;": {
                //return new StringObject(vm, getEnvironmentInfo());

                //return new StringObject(vm, "0|0|0|-|0|");
                C6DaZhongDianPing obj = new C6DaZhongDianPing();
                return new StringObject(vm, obj.getEnvironmentInfo());
            }
            case "java/lang/StringBuilder->append(Ljava/lang/String;)Ljava/lang/StringBuilder;": {
                String str = vaList.getObjectArg(0).getValue().toString();
                //StringBuilder stringBuilder = (StringBuilder) dvmObject.getValue();
                //return ProxyDvmObject.createObject(vm, stringBuilder.append(str));
                return ProxyDvmObject.createObject(vm, ((StringBuilder) dvmObject.getValue()).append(str));
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isVPN()Ljava/lang/String;": {
                return new StringObject(vm, "0");
            }
            // 获取亮度信息，字符串范围 0-1
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->brightness(Landroid/content/Context;)Ljava/lang/String;": {
                return new StringObject(vm, "0.8");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->systemVolume(Landroid/content/Context;)Ljava/lang/String;": {
                return new StringObject(vm, "0");
            }
            case "java/lang/StringBuilder->toString()Ljava/lang/String;": {
                return new StringObject(vm, dvmObject.getValue().toString());
            }
            case "android/content/Context->getApplicationContext()Landroid/content/Context;": {
                return vm.resolveClass("android/content/Context").newObject(null);
            }
            //获取系统服务
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;": {
                StringObject serviceName = vaList.getObjectArg(0);
                assert serviceName != null;
                return new SystemService(vm, serviceName.getValue());
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->checkBuildAttribute(Ljava/lang/String;)Ljava/lang/String;": {
                String arg = vaList.getObjectArg(0).getValue().toString();
                System.out.println("checkBuildAttribute:" + arg);
                return new StringObject(vm, arg.isEmpty() || arg.equalsIgnoreCase("unknown") ? "-" : arg);
            }
            case "android/view/WindowManager->getDefaultDisplay()Landroid/view/Display;": {
                return vm.resolveClass("android/view/Display").newObject(null);
            }
            case "java/lang/StringBuilder->append(I)Ljava/lang/StringBuilder;": {
                StringBuilder sb = (StringBuilder) dvmObject.getValue();
                return ProxyDvmObject.createObject(vm, sb.append(vaList.getIntArg(0)));
            }
            case "java/lang/StringBuilder->append(C)Ljava/lang/StringBuilder;": {
                StringBuilder sb = (StringBuilder) dvmObject.getValue();
                return ProxyDvmObject.createObject(vm, sb.append((char) vaList.getIntArg(0)));
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getTotalInternalMemorySize()Ljava/lang/String;":
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getTotalExternalMemorySize()Ljava/lang/String;": {
                return new StringObject(vm, "110GB");
            }
            //获取运营商相关信息
            case "android/telephony/TelephonyManager->getSimOperator()Ljava/lang/String;": {
                return new StringObject(vm, "46001");
            }
            //检测当前的网络模式，可以是：wifi/2G/3G/4G
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getAccessSubType()Ljava/lang/String;": {
                return new StringObject(vm, "4G");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getCpuInfoType()Ljava/lang/String;": {
                return new StringObject(vm, "arm");
            }
            case "java/io/BufferedReader->readLine()Ljava/lang/String;": {
                String oneline;
                try {
                    oneline = bufferedReader.readLine();
                    if (oneline != null) {
                        return new StringObject(vm, oneline);
                    } else {
                        return null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //获取传感器类型
            case "android/hardware/SensorManager->getDefaultSensor(I)Landroid/hardware/Sensor;": {
                int type = vaList.getIntArg(0);
                System.out.println("----------->Sensor type:" + type);
                return vm.resolveClass("android/hardware/Sensor").newObject(type);
            }
            //获取传感器名字：根据前面的传感器类型返回对应的传感器名字
            case "android/hardware/Sensor->getName()Ljava/lang/String;": {
                int type = (int) dvmObject.getValue();
                System.out.println("----》Sensor getName:" + type);
                if (type == 1) {
                    return new StringObject(vm, "ICM20690");
                } else if (type == 9) {
                    return new StringObject(vm, "gravity  Non-wakeup");
                } else {
                    throw new UnsupportedOperationException(signature);
                }
            }
            //设备供应商
            case "android/hardware/Sensor->getVendor()Ljava/lang/String;": {
                int type = (int) dvmObject.getValue();
                System.out.println("Sensor getVendor:" + type);
                if (type == 1) {
                    return new StringObject(vm, "qualcomm");
                } else if (type == 9) {
                    return new StringObject(vm, "qualcomm");
                } else {
                    throw new UnsupportedOperationException(signature);
                }
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getSysProp(Ljava/lang/String;)Ljava/lang/String;": {
                String name = vaList.getObjectArg(0).getValue().toString();
                System.out.println("---getSysProp: " + name);
                if (name.equals("ro.product.cpu.abi")) {
                    return new StringObject(vm, "arm64-v8a");
                } else if (name.equals("ro.product.cpu.abi2")) {
                    return new StringObject(vm, "");
                } else if (name.equals("ro.build.product")) {
                    return new StringObject(vm, "polaris");
                } else if (name.equals("ro.build.description")) {
                    return new StringObject(vm, "polaris-user 10 QKQ1.190828.002 V12.0.2.0.QDGCNXM release-keys");
                } else if (name.equals("ro.secure")) {
                    return new StringObject(vm, "1");
                } else if (name.equals("ro.debuggable")) {
                    return new StringObject(vm, "0");
                } else if (name.equals("persist.sys.usb.config")) {
                    return new StringObject(vm, "");
                } else if (name.equals("sys.usb.config")) {
                    return new StringObject(vm, "");
                } else if (name.equals("sys.usb.state")) {
                    return new StringObject(vm, "");
                } else if (name.equals("gsm.version.baseband")) {
                    return new StringObject(vm, "4.0.c2.6-00335-0914_2350_3c8fca6,4.0.c2.6-00335-0914_2350_3c8fca6");
                } else if (name.equals("gsm.version.ril-impl")) {
                    return new StringObject(vm, "Qualcomm RIL 1.0");
                } else if (name.equals("gsm.sim.state")) {
                    return new StringObject(vm, "ABSENT,ABSENT");
                } else if (name.equals("gsm.sim.state.2")) {
                    return new StringObject(vm, "");
                } else if (name.equals("wifi.interface")) {
                    return new StringObject(vm, "wlan0");
                }
                throw new UnsupportedOperationException(signature);
            }
            case "android/content/Context->getResources()Landroid/content/res/Resources;": {
                return vm.resolveClass("android/content/res/Resources").newObject(null);
            }
            case "android/content/res/Resources->getConfiguration()Landroid/content/res/Configuration;": {
                return vm.resolveClass("android/content/res/Configuration").newObject(null);
            }
            case "android/net/wifi/WifiManager->getConnectionInfo()Landroid/net/wifi/WifiInfo;": {
                return vm.resolveClass("android/net/wifi/WifiInfo").newObject(signature);
            }
            case "android/net/wifi/WifiInfo->getSSID()Ljava/lang/String;": {
                return new StringObject(vm, "Redmi_7B25");
            }
            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;": {
                return new StringObject(vm, "");
            }
            case "java/text/SimpleDateFormat->format(Ljava/util/Date;)Ljava/lang/String;": {
                Date date = (Date) vaList.getObjectArg(0).getValue();
                return new StringObject(vm, simpleDateFormat.format(date));
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->appCache(Landroid/content/Context;)Ljava/lang/String;": {
                return new StringObject(vm, "149045277");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->availableSystem()Ljava/lang/String;": {
                return new StringObject(vm, "unknown");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->totalMemory()Ljava/lang/String;": {
                return new StringObject(vm, "5905514496");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getFirstLaunchTime(Landroid/content/Context;)Ljava/lang/String;": {
                return new StringObject(vm, "1665747253983");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getDataActivity(Landroid/content/Context;)Ljava/lang/String;": {
                return new StringObject(vm, "0");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->mContext:Landroid/content/Context;": {
                return vm.resolveClass("android/content/Context").newObject(null);
            }
            case "android/content/res/Configuration->locale:Ljava/util/Locale;": {
                return ProxyDvmObject.createObject(vm, Locale.getDefault());
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->batteryHelper:Lcom/meituan/android/common/dfingerprint/collection/utils/BatteryHelper;": {
                return vm.resolveClass("com/meituan/android/common/dfingerprint/collection/utils/BatteryHelper").newObject(null);
            }
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isAccessibilityEnable()Z": {
                return false;
            }
            //允许访问电话状态权限
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isPermissionGranted(Ljava/lang/String;Landroid/content/Context;)Z": {
                String permissionName = vaList.getObjectArg(0).getValue().toString();
                System.out.println("---> check permission:" + permissionName);

                if (permissionName.equals("android.permission.READ_PHONE_STATE")) {
                    return true;
                } else if (permissionName.equals("android.permission.ACCESS_WIFI_STATE")) {
                    return true;
                } else {
                    throw new UnsupportedOperationException(signature);
                }
            }
            case "android/content/pm/PackageManager->hasSystemFeature(Ljava/lang/String;)Z": {
                String name = vaList.getObjectArg(0).getValue().toString();
                System.out.println("---check hasSystemFeature: " + name);
                switch (name) {
                    // 检测加速传感器
                    case "android.hardware.sensor.accelerometer":
                        // 检测陀螺仪传感器
                    case "android.hardware.sensor.gyroscope":
                        // wifi
                    case "android.hardware.wifi":
                        // 蓝牙
                    case "android.hardware.bluetooth":
                        // 蓝牙低功耗
                    case "android.hardware.bluetooth_le":
                        // 电话
                    case "android.hardware.telephony":
                        // gps
                    case "android.hardware.location.gps":
                        // USB 配件API
                    case "android.hardware.usb.accessory":
                        // nfc
                    case "android.hardware.nfc": {
                        return true;
                    }
                }
            }
            case "java/io/File->exists()Z": {
                //System.out.println("---->|" + dvmObject.getValue().toString());
                //直接返回 true 也行，因为这里判断的就是我们刚处理的 file1 是否存在。
                return file1.exists();
            }
            case "java/lang/String->equalsIgnoreCase(Ljava/lang/String;)Z": {
                return dvmObject.getValue().toString().equalsIgnoreCase(vaList.getObjectArg(0).getValue().toString());
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getBatteryInfo()Z": {
                return true;
            }
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/String->valueOf(I)Ljava/lang/String;": {
                return new StringObject(vm, String.valueOf(vaList.getIntArg(0)));
            }
            case "java/lang/Integer->toString(I)Ljava/lang/String;": {
                return new StringObject(vm, Integer.toString(vaList.getIntArg(0)));
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->uiAutomatorClickCount()I": {
                return 0;
            }
            //获取屏幕的高
            case "android/view/Display->getHeight()I": {
                return 2160;
            }
            //获取屏幕的宽
            case "android/view/Display->getWidth()I": {
                return 1080;
            }
            case "java/lang/String->compareToIgnoreCase(Ljava/lang/String;)I": {
                String str = vaList.getObjectArg(0).getValue().toString();
                return dvmObject.getValue().toString().compareToIgnoreCase(str);
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->boolean2Integer(Z)I": {
                return vaList.getIntArg(0);
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->androidAppCnt(Landroid/content/Context;)I": {
                return 519;
            }
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            //这里在获取SDK的版本，我选择返回29，也就是 Android10 版本。
            case "android/os/Build$VERSION->SDK_INT:I": {
                return 29;
            }
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/text/TextUtils->isEmpty(Ljava/lang/CharSequence;)Z": {
                String str = vaList.getObjectArg(0).getValue().toString();
                return str == null || str.length() == 0;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);

    }


    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "android/os/Build->BOARD:Ljava/lang/String;": {
                return new StringObject(vm, "sdm845");
            }
            case "android/os/Build->MANUFACTURER:Ljava/lang/String;": {
                return new StringObject(vm, "Xiaomi");
            }
            case "android/os/Build->BRAND:Ljava/lang/String;": {
                return new StringObject(vm, "Xiaomi");
            }
            case "android/os/Build->MODEL:Ljava/lang/String;": {
                return new StringObject(vm, "MIX 2S");
            }
            case "android/os/Build->PRODUCT:Ljava/lang/String;": {
                return new StringObject(vm, "polaris");
            }
            case "android/os/Build->HARDWARE:Ljava/lang/String;": {
                return new StringObject(vm, "qcom");
            }
            case "android/os/Build->DEVICE:Ljava/lang/String;": {
                return new StringObject(vm, "polaris");
            }
            case "android/os/Build->HOST:Ljava/lang/String;": {
                return new StringObject(vm, "c3-miui-ota-bd134.bj");
            }
            case "android/os/Build->ID:Ljava/lang/String;": {
                return new StringObject(vm, "QKQ1.190828.002");
            }
            case "android/os/Build$VERSION->RELEASE:Ljava/lang/String;": {
                return new StringObject(vm, "10");
            }
            case "android/os/Build->TAGS:Ljava/lang/String;": {
                return new StringObject(vm, "release-keys");
            }
            case "android/os/Build->FINGERPRINT:Ljava/lang/String;": {
                return new StringObject(vm, "Xiaomi/polaris/polaris:10/QKQ1.190828.002/V12.0.2.0.QDGCNXM:user/release-keys");
            }
            case "android/os/Build->TYPE:Ljava/lang/String;": {
                return new StringObject(vm, "user");
            }
            case "android/os/Build$VERSION->SDK:Ljava/lang/String;": {
                return new StringObject(vm, "29");
            }
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            // level 电量剩余多少
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->level:I":
                return 60;
            // scale 电量精度
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->scale:I":
                return 100;
            // status 电池状态，未知、充电、放电、未充电、满电;
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->status:I":
                return 0;
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public boolean getBooleanField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->plugged:Z": {
                return false;
            }
        }
        return super.getBooleanField(vm, dvmObject, signature);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("lilac open:" + pathname);
        return null;
    }
}
