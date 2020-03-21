package com.hsq.flutter.dynamicupdate.entities;

import androidx.annotation.NonNull;

/**
 * Created by heshiqi on 20/01/17.
 * Flutter so 文件信息
 */
public class FlutterSoEntity {
    public int appVersion;//应用的版本号
    @NonNull
    public int pluginSoVersion;//Flutter so 库的版本号
    @NonNull
    public String appName;//应用名
    @NonNull
    public String ABIName;//CPU类型
    @NonNull
    public long lastUpdateTime;//最后更新的时间
    @NonNull
    public String fileMd5;//file 文件 md5 签名，保证文件的完整性
}
