# flutter_dynamic_update

A new Flutter application.

## Getting Started

This project is a starting point for a Flutter application.

项目结构中的dynamic_update插件项目实现动态更新

使用步骤：

## step 1：
Application继承dynamic_update库中的BaseApplication类

public class MyApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

## step 2
app项目实现检查是否需要动态更新Flutter应用

## step 3
将最新编译的Flutter so库推送到指定目录，demo中的推送指定目录为：/storage/emulated/0/ahflutter/jniLibs/libapp100.so

## step 4
然后调用 installNewFlutterSo安装最新的Flutter so库
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
}

## step 5
安装完成后杀进程，重新打开完成更新应用



现在只是简单实现，后续逐步完善中。
    


