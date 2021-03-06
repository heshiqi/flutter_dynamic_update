package io.flutter.embedding.engine.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hsq.flutter.dynamicupdate.DynamicUpdateExtractor;
import com.hsq.flutter.dynamicupdate.entities.FlutterSoEntity;
import com.hsq.flutter.dynamicupdate.utils.FileUtils;
import com.hsq.flutter.dynamicupdate.utils.ThreadPool;
import com.hsq.flutter.dynamicupdate.utils.Utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.flutter.BuildConfig;
import io.flutter.embedding.engine.FlutterJNI;
import io.flutter.util.PathUtils;
import io.flutter.view.VsyncWaiter;

/**
 * Created by heshiqi on 20/01/16.
 * 自定义的Flutter加载器，实现动态替换加载路径
 */
public class CustomFlutterLoader extends FlutterLoader {
    // Must match values in flutter::switches
    private static final String AOT_SHARED_LIBRARY_NAME = "aot-shared-library-name";
    private static final String SNAPSHOT_ASSET_PATH_KEY = "snapshot-asset-path";
    private static final String VM_SNAPSHOT_DATA_KEY = "vm-snapshot-data";
    private static final String ISOLATE_SNAPSHOT_DATA_KEY = "isolate-snapshot-data";
    private static final String FLUTTER_ASSETS_DIR_KEY = "flutter-assets-dir";

    // XML Attribute keys supported in AndroidManifest.xml
    private static final String PUBLIC_AOT_SHARED_LIBRARY_NAME =
            FlutterLoader.class.getName() + '.' + AOT_SHARED_LIBRARY_NAME;
    private static final String PUBLIC_VM_SNAPSHOT_DATA_KEY =
            FlutterLoader.class.getName() + '.' + VM_SNAPSHOT_DATA_KEY;
    private static final String PUBLIC_ISOLATE_SNAPSHOT_DATA_KEY =
            FlutterLoader.class.getName() + '.' + ISOLATE_SNAPSHOT_DATA_KEY;
    private static final String PUBLIC_FLUTTER_ASSETS_DIR_KEY =
            FlutterLoader.class.getName() + '.' + FLUTTER_ASSETS_DIR_KEY;

    // Resource names used for components of the precompiled snapshot.
    private static final String DEFAULT_AOT_SHARED_LIBRARY_NAME = "libapp.so";
    private static final String DEFAULT_VM_SNAPSHOT_DATA = "vm_snapshot_data";
    private static final String DEFAULT_ISOLATE_SNAPSHOT_DATA = "isolate_snapshot_data";
    private static final String DEFAULT_LIBRARY = "libflutter.so";
    private static final String DEFAULT_KERNEL_BLOB = "kernel_blob.bin";
    private static final String DEFAULT_FLUTTER_ASSETS_DIR = "flutter_assets";

    // Mutable because default values can be overridden via config properties
    private String aotSharedLibraryName = DEFAULT_AOT_SHARED_LIBRARY_NAME;
    private String vmSnapshotData = DEFAULT_VM_SNAPSHOT_DATA;
    private String isolateSnapshotData = DEFAULT_ISOLATE_SNAPSHOT_DATA;
    private String flutterAssetsDir = DEFAULT_FLUTTER_ASSETS_DIR;

    private boolean initialized = false;
    @Nullable
    private ResourceExtractor resourceExtractor;
    @Nullable
    private FlutterLoader.Settings settings;

    /**
     * Starts initialization of the native system.
     *
     * @param applicationContext The Android application context.
     */
    public void startInitialization(@NonNull Context applicationContext) {
        startInitialization(applicationContext, new FlutterLoader.Settings());
    }

    /**
     * Starts initialization of the native system.
     * <p>
     * This loads the Flutter engine's native library to enable subsequent JNI calls. This also
     * starts locating and unpacking Dart resources packaged in the app's APK.
     * <p>
     * Calling this method multiple times has no effect.
     *
     * @param applicationContext The Android application context.
     * @param settings           Configuration settings.
     */
    public void startInitialization(@NonNull Context applicationContext, @NonNull FlutterLoader.Settings settings) {
        // Do not run startInitialization more than once.
        if (this.settings != null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("startInitialization must be called on the main thread");
        }

        this.settings = settings;

        long initStartTimestampMillis = SystemClock.uptimeMillis();
        initConfig(applicationContext);
        initResources(applicationContext);
        System.loadLibrary("flutter");
        VsyncWaiter
                .getInstance((WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE))
                .init();

        // We record the initialization time using SystemClock because at the start of the
        // initialization we have not yet loaded the native library to call into dart_tools_api.h.
        // To get Timeline timestamp of the start of initialization we simply subtract the delta
        // from the Timeline timestamp at the current moment (the assumption is that the overhead
        // of the JNI call is negligible).
        long initTimeMillis = SystemClock.uptimeMillis() - initStartTimestampMillis;
        FlutterJNI.nativeRecordStartTimestamp(initTimeMillis);
    }

    /**
     * Blocks until initialization of the native system has completed.
     * <p>
     * Calling this method multiple times has no effect.
     *
     * @param applicationContext The Android application context.
     * @param args               Flags sent to the Flutter runtime.
     */
    public void ensureInitializationComplete(@NonNull Context applicationContext, @Nullable String[] args) {
        if (initialized) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ensureInitializationComplete must be called on the main thread");
        }
        if (settings == null) {
            throw new IllegalStateException("ensureInitializationComplete must be called after startInitialization");
        }
        try {
            if (resourceExtractor != null) {
                resourceExtractor.waitForCompletion();
            }

            List<String> shellArgs = new ArrayList<>();
            shellArgs.add("--icu-symbol-prefix=_binary_icudtl_dat");

            ApplicationInfo applicationInfo = getApplicationInfo(applicationContext);
            shellArgs.add("--icu-native-lib-path=" + applicationInfo.nativeLibraryDir + File.separator + DEFAULT_LIBRARY);

            if (args != null) {
                Collections.addAll(shellArgs, args);
            }
            loadNewFlutterSo(applicationContext);
            String kernelPath = null;
            if (io.flutter.BuildConfig.DEBUG || io.flutter.BuildConfig.JIT_RELEASE) {
                String snapshotAssetPath = PathUtils.getDataDirectory(applicationContext) + File.separator + flutterAssetsDir;
                kernelPath = snapshotAssetPath + File.separator + DEFAULT_KERNEL_BLOB;
                shellArgs.add("--" + SNAPSHOT_ASSET_PATH_KEY + "=" + snapshotAssetPath);
                shellArgs.add("--" + VM_SNAPSHOT_DATA_KEY + "=" + vmSnapshotData);
                shellArgs.add("--" + ISOLATE_SNAPSHOT_DATA_KEY + "=" + isolateSnapshotData);
            } else {
                String nowAotLibraryName = aotSharedLibraryName;
                String nowAotLibraryPath = applicationInfo.nativeLibraryDir + File.separator + aotSharedLibraryName;
                String[] flutterSoInfo=loadNewFlutterSo(applicationContext);
                if(flutterSoInfo!=null){
                    nowAotLibraryPath =  flutterSoInfo[0];
                    nowAotLibraryName = flutterSoInfo[1];
                }

                shellArgs.add("--" + AOT_SHARED_LIBRARY_NAME + "=" + nowAotLibraryName);

                // Most devices can load the AOT shared library based on the library name
                // with no directory path.  Provide a fully qualified path to the library
                // as a workaround for devices where that fails.

                shellArgs.add("--" + AOT_SHARED_LIBRARY_NAME + "=" + nowAotLibraryPath);
            }

            shellArgs.add("--cache-dir-path=" + PathUtils.getCacheDirectory(applicationContext));
            if (settings.getLogTag() != null) {
                shellArgs.add("--log-tag=" + settings.getLogTag());
            }
            String appStoragePath = PathUtils.getFilesDir(applicationContext);
            String engineCachesPath = PathUtils.getCacheDirectory(applicationContext);
            FlutterJNI.nativeInit(applicationContext, shellArgs.toArray(new String[0]),
                    kernelPath, appStoragePath, engineCachesPath);

            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] loadNewFlutterSo(Context context) throws IOException {
        String[] result=null;
        final File dir = context.getDir("jniLibs", Context.MODE_PRIVATE);
        List<File> files = FileUtils.listFilesInDirWithFilter(dir, pathname -> pathname.getName().startsWith(FileUtils.TIMESTAMP_PREFIX), (o1, o2) -> {
            if (o1.isDirectory() && o2.isFile())
                return -1;
            if (o1.isFile() && o2.isDirectory())
                return 1;
            return o2.getName().compareTo(o1.getName());
        });
        if (!files.isEmpty()) {
            //获取最新的Flutter so
            File file = files.get(0);
            FlutterSoEntity entity = DynamicUpdateExtractor.readSoInfo(file);
            String flutterSoPath = FileUtils.generateSoFileName(entity);
            File soFile = new File(dir,flutterSoPath);
            if (soFile.exists() && Utils.getFileMD5(soFile).equals(entity.fileMd5)) {
                result=new String[2];
                result[0]=soFile.getAbsolutePath();
                result[1]=soFile.getName();
                ThreadPool.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        DynamicUpdateExtractor.deleteOtherFlutterSoFile(dir.getAbsolutePath(),entity);
                    }
                });
            }
        }
        return result;
    }


    /**
     * Same as {@link #ensureInitializationComplete(Context, String[])} but waiting on a background
     * thread, then invoking {@code callback} on the {@code callbackHandler}.
     */
    public void ensureInitializationCompleteAsync(
            @NonNull Context applicationContext,
            @Nullable String[] args,
            @NonNull Handler callbackHandler,
            @NonNull Runnable callback
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ensureInitializationComplete must be called on the main thread");
        }
        if (settings == null) {
            throw new IllegalStateException("ensureInitializationComplete must be called after startInitialization");
        }
        if (initialized) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (resourceExtractor != null) {
                    resourceExtractor.waitForCompletion();
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ensureInitializationComplete(applicationContext.getApplicationContext(), args);
                        callbackHandler.post(callback);
                    }
                });
            }
        }).start();
    }

    @NonNull
    private ApplicationInfo getApplicationInfo(@NonNull Context applicationContext) {
        try {
            return applicationContext
                    .getPackageManager()
                    .getApplicationInfo(applicationContext.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize our Flutter config values by obtaining them from the
     * manifest XML file, falling back to default values.
     */
    private void initConfig(@NonNull Context applicationContext) {
        Bundle metadata = getApplicationInfo(applicationContext).metaData;

        // There isn't a `<meta-data>` tag as a direct child of `<application>` in
        // `AndroidManifest.xml`.
        if (metadata == null) {
            return;
        }

        aotSharedLibraryName = metadata.getString(PUBLIC_AOT_SHARED_LIBRARY_NAME, DEFAULT_AOT_SHARED_LIBRARY_NAME);
        flutterAssetsDir = metadata.getString(PUBLIC_FLUTTER_ASSETS_DIR_KEY, DEFAULT_FLUTTER_ASSETS_DIR);

        vmSnapshotData = metadata.getString(PUBLIC_VM_SNAPSHOT_DATA_KEY, DEFAULT_VM_SNAPSHOT_DATA);
        isolateSnapshotData = metadata.getString(PUBLIC_ISOLATE_SNAPSHOT_DATA_KEY, DEFAULT_ISOLATE_SNAPSHOT_DATA);
    }

    /**
     * Extract assets out of the APK that need to be cached as uncompressed
     * files on disk.
     */
    private void initResources(@NonNull Context applicationContext) {
        new ResourceCleaner(applicationContext).start();

        if (io.flutter.BuildConfig.DEBUG || BuildConfig.JIT_RELEASE) {
            final String dataDirPath = PathUtils.getDataDirectory(applicationContext);
            final String packageName = applicationContext.getPackageName();
            final PackageManager packageManager = applicationContext.getPackageManager();
            final AssetManager assetManager = applicationContext.getResources().getAssets();
            resourceExtractor = new ResourceExtractor(dataDirPath, packageName, packageManager, assetManager);

            // In debug/JIT mode these assets will be written to disk and then
            // mapped into memory so they can be provided to the Dart VM.
            resourceExtractor
                    .addResource(fullAssetPathFrom(vmSnapshotData))
                    .addResource(fullAssetPathFrom(isolateSnapshotData))
                    .addResource(fullAssetPathFrom(DEFAULT_KERNEL_BLOB));

            resourceExtractor.start();
        }
    }

    @NonNull
    public String findAppBundlePath() {
        return flutterAssetsDir;
    }

    /**
     * Returns the file name for the given asset.
     * The returned file name can be used to access the asset in the APK
     * through the {@link android.content.res.AssetManager} API.
     *
     * @param asset the name of the asset. The name can be hierarchical
     * @return the filename to be used with {@link android.content.res.AssetManager}
     */
    @NonNull
    public String getLookupKeyForAsset(@NonNull String asset) {
        return fullAssetPathFrom(asset);
    }

    /**
     * Returns the file name for the given asset which originates from the
     * specified packageName. The returned file name can be used to access
     * the asset in the APK through the {@link android.content.res.AssetManager} API.
     *
     * @param asset       the name of the asset. The name can be hierarchical
     * @param packageName the name of the package from which the asset originates
     * @return the file name to be used with {@link android.content.res.AssetManager}
     */
    @NonNull
    public String getLookupKeyForAsset(@NonNull String asset, @NonNull String packageName) {
        return getLookupKeyForAsset(
                "packages" + File.separator + packageName + File.separator + asset);
    }

    @NonNull
    private String fullAssetPathFrom(@NonNull String filePath) {
        return flutterAssetsDir + File.separator + filePath;
    }
}
