package com.hsq.flutter.dynamicupdate;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Field;

import io.flutter.app.FlutterApplication;
import io.flutter.embedding.engine.loader.CustomFlutterLoader;
import io.flutter.embedding.engine.loader.FlutterLoader;

/**
 *  Created by heshiqi on 20/01/16.
 */
public class BaseApplication  extends FlutterApplication {

    public static Application instance;

    @Override
    public void onCreate() {
        instance=this;
        try {
            //替换成自定义的Flutter加载类 干预Flutter的加载
            FlutterLoader flutterLoader = new CustomFlutterLoader();
            Field field = FlutterLoader.class.getDeclaredField("instance");
            field.setAccessible(true);//
            field.set(null, flutterLoader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance=this;
    }
}
