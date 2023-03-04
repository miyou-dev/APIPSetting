package com.miyou.apIpSetting;

import android.net.LinkAddress;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.util.HashSet;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MainHook implements IXposedHookLoadPackage {

    private static final String className = "android.net.ip.IpServer";
    private static final String methodName = "requestIpv4Address";

    private static final String WIFI_HOST_ADDRESS = "10.9.9.1";
    private static final int WIFI_HOST_SUBNET_MASK = 24;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        final String packageName = param.packageName;
        HashSet<String> pkgNameSet = new HashSet<>();
        pkgNameSet.add("com.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.android.networkstack.tethering");
        pkgNameSet.add("com.google.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.google.android.networkstack.tethering");
        pkgNameSet.add("android");
        if (!pkgNameSet.contains(packageName)) {
            return;
        }
        Constructor<?> constructor = LinkAddress.class.getDeclaredConstructor(String.class);
        final Object mLinkAddress = constructor.newInstance(WIFI_HOST_ADDRESS + "/" + WIFI_HOST_SUBNET_MASK);
        XposedBridge.log("AP IP Setting " + WIFI_HOST_ADDRESS + "/" + WIFI_HOST_SUBNET_MASK);
        XposedBridge.log("AP IP Setting Android API " + Build.VERSION.SDK_INT);

        final ClassLoader classLoader = param.classLoader;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Android 11
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, getXc_methodHook(mLinkAddress));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, boolean.class, getXc_methodHook(mLinkAddress));
        }
    }

    private XC_MethodHook getXc_methodHook(Object mLinkAddress) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (StackUtils.isCallingFrom(className, "configureIPv4")) {
                    param.setResult(mLinkAddress);
                }
            }
        };
    }
}