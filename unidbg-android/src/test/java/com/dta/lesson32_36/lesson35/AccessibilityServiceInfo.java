package com.dta.lesson32_36.lesson35;

public class AccessibilityServiceInfo {
    public String[] packageNames;
    public String name;
    public String packageName;
    public String label;

    public AccessibilityServiceInfo(String name, String packageName, String label) {
        this.name = name;
        this.packageName = packageName;
        this.label = label;
    }
}
