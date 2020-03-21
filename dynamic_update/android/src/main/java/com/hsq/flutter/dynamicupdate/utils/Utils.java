package com.hsq.flutter.dynamicupdate.utils;

import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;

import com.hsq.flutter.dynamicupdate.entities.FlutterSoEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static java.util.Arrays.asList;

/**
 *  Created by heshiqi on 20/01/16.
 */
public class Utils {

    @SuppressWarnings("deprecation")
    public static long getVersionCode(@NonNull PackageInfo packageInfo) {
        // Linter needs P (28) hardcoded or else it will fail these lines.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        } else {
            return packageInfo.versionCode;
        }
    }

    @SuppressWarnings("deprecation")
    public static String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            ArrayList<String> cpuAbis = new ArrayList<String>(asList(Build.CPU_ABI, Build.CPU_ABI2));
            cpuAbis.removeAll(asList(null, ""));
            return cpuAbis.toArray(new String[0]);
        }
    }

    /**
     * 获取一个文件的md5值
     * @return md5 value
     */
    public static String getFileMD5(File file){
        BigInteger bigInt = null;
        FileInputStream fis=null;
        try {
            fis = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                md.update(buffer, 0, length);
            }
            bigInt = new BigInteger(1, md.digest());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (fis != null){
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bigInt.toString(16);
    }
}
