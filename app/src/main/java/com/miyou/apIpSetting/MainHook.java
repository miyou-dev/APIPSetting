package com.miyou.apIpSetting;

import android.net.LinkAddress;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MainHook implements IXposedHookLoadPackage {

    private static final String className = "android.net.ip.IpServer";

    private static final String WIFI_HOST_IFACE_ADDR = "10.9.9.1";
    private static final int WIFI_HOST_IFACE_PREFIX_LENGTH = 24;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        final String packageName = param.packageName;
        HashSet<String> pkgNameSet = new HashSet<>();
        pkgNameSet.add("com.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.android.networkstack.tethering");
        pkgNameSet.add("com.google.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.google.android.networkstack.tethering");
        pkgNameSet.add("android");
        if (!pkgNameSet.contains(packageName)) return;

        XposedBridge.log("IP 设置 Android API:" + Build.VERSION.SDK_INT + " IP:" + WIFI_HOST_IFACE_ADDR + "/" + WIFI_HOST_IFACE_PREFIX_LENGTH);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10
            // http://aospxref.com/android-10.0.0_r47/xref/frameworks/base/services/net/java/android/net/ip/IpServer.java#469
            XposedHelpers.findAndHookMethod(className, param.classLoader, "getRandomWifiIPv4Address", methodReplacement());
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Android 11
            // http://aospxref.com/android-11.0.0_r21/xref/frameworks/base/packages/Tethering/src/android/net/ip/IpServer.java#645
            XposedHelpers.findAndHookMethod(className, param.classLoader, "requestIpv4Address", methodHook());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 - 13
            // http://aospxref.com/android-12.0.0_r3/xref/packages/modules/Connectivity/Tethering/src/android/net/ip/IpServer.java#655
            // http://aospxref.com/android-13.0.0_r3/xref/packages/modules/Connectivity/Tethering/src/android/net/ip/IpServer.java#664
            XposedHelpers.findAndHookMethod(className, param.classLoader, "requestIpv4Address", boolean.class, methodHook());
        }
    }

    private XC_MethodReplacement methodReplacement() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return WIFI_HOST_IFACE_ADDR;
            }
        };
    }

    private XC_MethodHook methodHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Constructor<?> constructor = LinkAddress.class.getDeclaredConstructor(String.class);
                final Object mLinkAddress = constructor.newInstance(WIFI_HOST_IFACE_ADDR + "/" + WIFI_HOST_IFACE_PREFIX_LENGTH);
                if (StackUtils.isCallingFrom(className, "configureIPv4")) {
                    param.setResult(mLinkAddress);
                }
            }
        };
    }
}