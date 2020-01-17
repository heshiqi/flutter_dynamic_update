package com.hsq.flutter.dynamicupdate.util;

import android.os.Build;

import io.flutter.plugin.common.PluginRegistry;

/**
 *  Created by heshiqi on 20/01/16.
 */
public class Utils {

    public static PluginRegistry.Registrar mRegistrar;

    public static String[] getApis(){
        String[] abis;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
        {
            abis = Build.SUPPORTED_ABIS;
        } else {
            abis = new String[]{Build.CPU_ABI,Build.CPU_ABI2};
        }
        return abis;
    }

}
