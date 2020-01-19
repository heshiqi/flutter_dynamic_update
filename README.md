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
然后调用 DynamicUpdateExtractor安装最新的Flutter so库

    new DynamicUpdateExtractor(dataDirPath,sourceFilePath,flutterSoEntity,updateListener)
                           .start();
                           
参数介绍
dataDirPath：应用安装目录，这里定义的是/data/user/0/packagename/app_jniLibs  
sourceFilePath：最新的Flutter so文件绝对路径  
flutterSoEntity：Flutter更新包的信息  

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
updateListener:更新包更新状态回调事件。  

## step 5
安装完成后杀进程，重新打开完成更新应用



现在只是简单实现，后续逐步完善中。
    


