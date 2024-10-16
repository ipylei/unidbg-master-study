package com.androidsdk;

public class AccessibilityServiceInfo {
    // 指定您希望服务处理的无障碍事件所属的应用的软件包名称。如果省略此参数，则无障碍服务会被视为可用于处理任何应用的无障碍事件。
    // 即当前无障碍服务作用于哪些app
    public String[] packageNames;
    // 当前无障碍服务的名称
    public String name;
    // 当前无障碍服务所属App的标签名
    public CharSequence label;
    // 当前无障碍服务所属App的包名
    public String packageName;

    public AccessibilityServiceInfo(String packageName, String name, CharSequence label, String[] packageNames){
        this.packageName = packageName;
        this.name = name;
        this.label = label;
        this.packageNames = packageNames;
    }
}