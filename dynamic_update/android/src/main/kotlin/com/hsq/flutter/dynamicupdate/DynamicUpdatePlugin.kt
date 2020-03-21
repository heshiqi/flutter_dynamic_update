package com.hsq.flutter.dynamicupdate

import android.util.Log
import androidx.annotation.NonNull;
import com.hsq.flutter.dynamicupdate.entities.FlutterSoEntity
import com.hsq.flutter.dynamicupdate.utils.FileUtils
import com.hsq.flutter.dynamicupdate.utils.Utils
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import kotlin.collections.HashMap

/** DynamicUpdatePlugin */
class DynamicUpdatePlugin : FlutterPlugin, MethodCallHandler {

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "dynamic_update")
        channel.setMethodCallHandler(DynamicUpdatePlugin());
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "dynamic_update")
            channel.setMethodCallHandler(DynamicUpdatePlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "dynamicUpdate") {
            val soEntity = FlutterSoEntity();
            soEntity.appVersion = 100;
            soEntity.pluginSoVersion = 100;
            soEntity.appName = "flutterdemo"
            soEntity.ABIName = "arm64"
            soEntity.lastUpdateTime = System.currentTimeMillis();
            soEntity.fileMd5=Utils.getFileMD5(File(FileUtils.getFlutterLibPath() + File.separator + "libapp100.so"))
            val dirDataPath = FileUtils.getLibsFile(BaseApplication.instance, Constants.APP_JNI_LIBS_FILE_NAME)
            DynamicUpdateExtractor(dirDataPath.absolutePath,
                    FileUtils.getFlutterLibPath() + File.separator + "libapp100.so",
                    soEntity,
                    object :DynamicUpdateExtractor.OnUpdateListener{
                        /**
                         * 动态更新成功
                         * @param code
                         * @param msg
                         */
                        override fun onInstallSuccess(code: Int, msg: String){
                            result.success("插件更新成功,请杀进程重新启动")
                        }

                        /**
                         * 动态更新失败
                         * @param code
                         * @param msg
                         */
                        override fun onInstallFail(code: Int, msg: String){
                            result.success("插件更新失败")
                        }
                    }).start()
        } else {

            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    }

}
