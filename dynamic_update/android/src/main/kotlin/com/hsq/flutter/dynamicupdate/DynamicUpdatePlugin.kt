package com.hsq.flutter.dynamicupdate

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat
import com.hsq.flutter.dynamicupdate.util.FileUtils
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
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
            val results = HashMap<String, Any>()
            var resultMsg="";
            if(DynamicUpdateManager.installNewFlutterSo(BaseApplication.instance,FileUtils.getFlutterLibPath()+File.separator+"libapp100.so")){
                results.put("success",true);
                resultMsg="插件更新成功,请杀进程重新启动";
            }else{
                resultMsg="插件更新失败";
            }
            result.success(resultMsg)
        } else {

            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    }

}
