package com.hsq.flutter.dynamicupdate;

import android.content.Context;

import com.hsq.flutter.dynamicupdate.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Created by heshiqi on 20/01/16.
 * 动态更新执行类
 */
public class DynamicUpdateManager {

    /**
     * 加载 最新的 Flutter so 文件
     *
     * @param context
     * @param fromFilePath 所要安装的so文件路径 安装完成后删除
     */
    public static boolean installNewFlutterSo(Context context, String fromFilePath) {
        File srcfile = FileUtils.getFileByPath(fromFilePath);
        if (srcfile == null) {
            return false;
        }
        File dir = FileUtils.getLibsFile(context, Constants.APP_JNI_LIBS_FILE_NAME);
        if (!isLoadSoFile(dir, srcfile)) {
            return FileUtils.copy(srcfile.getAbsolutePath(), dir.getAbsolutePath() + File.separator + srcfile.getName());
        }
        return false;
    }

    /**
     * 判断 so 文件是否存在
     */
    public static boolean isLoadSoFile(File dir, File srcFile) {
        File[] currentFiles = dir.listFiles();
        boolean hasSoLib = false;
        if (currentFiles == null) {
            return false;
        }
        for (File currentFile : currentFiles) {
            //判断里面是否存在这个库,以及sd也有这个库，那就删除，然后进行外面拷贝进去
            if (currentFile.getName().contains(srcFile.getName())) {
                hasSoLib = srcFile.exists() && !currentFile.delete();
            }
        }
        return hasSoLib;
    }

    /**
     * 删除老版本的Flutter 应用so文件
     *
     * @return
     */
    public static boolean deleteAppLibsOldSo(Context context,File newFile) {
        File dir = FileUtils.getLibsFile(context, Constants.APP_JNI_LIBS_FILE_NAME);
        List<File> currentFiles = FileUtils.listFilesInDir(dir);
        for (File file:currentFiles){
            if(!file.getName().equalsIgnoreCase(newFile.getName())){
               file.delete();
            }
        }
        return true;
    }
}
