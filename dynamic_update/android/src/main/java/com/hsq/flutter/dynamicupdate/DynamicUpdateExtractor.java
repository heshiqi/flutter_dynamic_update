package com.hsq.flutter.dynamicupdate;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.hsq.flutter.dynamicupdate.entities.FlutterSoEntity;
import com.hsq.flutter.dynamicupdate.utils.FileUtils;
import com.hsq.flutter.dynamicupdate.utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by heshiqi on 20/01/16.
 * 动态更新执行类 异步加载
 */
public class DynamicUpdateExtractor {


    private static final String TAG = "DynamicUpdateExtractor";
    private static final String[] SUPPORTED_ABIS = Utils.getSupportedAbis();

    public interface OnUpdateListener {

        int SUCCESS_UPDATE =1;//应用已经是最新版本
        int ALREADY_UPDATE =2;//应用已经是最新版本
        int FAIL_ABI =3;//应用已经是最新版本
        int FAIL_UPDATE =4;//应用已经是最新版本
        /**
         * 动态更新成功
         * @param code
         * @param msg
         */
        void onInstallSuccess(int code, String msg);

        /**
         * 动态更新失败
         * @param code
         * @param msg
         */
        void onInstallFail(int code,String msg);
    }

    private static class ExtractTask extends AsyncTask<Void, Void, Boolean> {

        @NonNull
        private final String mDataDirPath;//应用安装目录
        @NonNull
        private final String mSourceFilePath;//最新的Flutter so文件绝对路径
        @NonNull
        private final FlutterSoEntity mFlutterSoEntity;

        private final OnUpdateListener mUpdateListener;
        private final Handler handler=new Handler(Looper.getMainLooper());

        ExtractTask(@NonNull String dataDirPath,
                    @NonNull String sourceFilePath,
                    @NonNull FlutterSoEntity flutterSoEntity,
                    OnUpdateListener updateListener) {
            mDataDirPath = dataDirPath;
            mSourceFilePath = sourceFilePath;
            mFlutterSoEntity = flutterSoEntity;
            this.mUpdateListener = updateListener;
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            final File dataDir = new File(mDataDirPath);

            final String timestamp = checkTimestamp(dataDir, mFlutterSoEntity,mUpdateListener,handler);
            if (timestamp == null) {
                return false;
            }

            if (!extractSo(dataDir)) {
                if(mUpdateListener!=null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mUpdateListener.onInstallFail(OnUpdateListener.FAIL_UPDATE,"");
                        }
                    });
                }
                return false;
            }

            if (timestamp != null) {
                try {
                    File file = new File(dataDir, timestamp);
                    if (file.createNewFile()) {
                        writeSoInfo(file, mFlutterSoEntity);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to write resource timestamp");
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mUpdateListener == null) {
                return;
            }
            if(result) {
                mUpdateListener.onInstallSuccess(OnUpdateListener.SUCCESS_UPDATE, "");
            }
        }

        /**
         * 安装 最新的 Flutter so 文件
         *
         * @param dataDir
         * @return true 安装成功
         */
        @WorkerThread
        private boolean extractSo(@NonNull File dataDir) {
            String fileName = FileUtils.generateSoFileName(mFlutterSoEntity);
            return FileUtils.copy(mSourceFilePath, mDataDirPath + File.separator + fileName);
        }
    }

    @NonNull
    private final String mDataDirPath;//应用安装目录
    @NonNull
    private final String mSourceFilePath;//最新的Flutter so文件绝对路径
    @NonNull
    private final FlutterSoEntity mFlutterSoEntity;
    private final OnUpdateListener mUpdateListener;

    private DynamicUpdateExtractor.ExtractTask mExtractTask;

    DynamicUpdateExtractor(@NonNull String dataDirPath,
                           @NonNull String sourceFilePath,
                           @NonNull FlutterSoEntity flutterSoEntity,
                           OnUpdateListener updateListener) {
        mDataDirPath = dataDirPath;
        mSourceFilePath = sourceFilePath;
        mFlutterSoEntity = flutterSoEntity;
        mUpdateListener = updateListener;
    }

    DynamicUpdateExtractor start() {
        if (io.flutter.BuildConfig.DEBUG && mExtractTask != null) {
            Log.e(TAG, "开启线程安装最新的 Flutter so 文件");
        }
        mExtractTask = new DynamicUpdateExtractor.ExtractTask(mDataDirPath, mSourceFilePath, mFlutterSoEntity, mUpdateListener);
        mExtractTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return this;
    }

    public static String[] getExistingTimestamps(File dataDir) {
        return dataDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(FileUtils.TIMESTAMP_PREFIX);
            }
        });
    }


    /**
     * 删除之前老版本的Flutter so 文件
     *
     * @param dataDirPath     应用安装目录
     * @param currentSoEntity 应用当前加载的flutter so 文件
     */
    public static void deleteOtherFlutterSoFile(@NonNull String dataDirPath, @NonNull FlutterSoEntity currentSoEntity) {
        final File dataDir = new File(dataDirPath);
        if(!dataDir.exists()){
            return;
        }
        String soFileName = FileUtils.generateSoFileName(currentSoEntity);
        String timestampFileName = FileUtils.generateTimestampFileName(currentSoEntity);
        List<File> soFiles = FileUtils.listFilesInDir(dataDir);
        for (File f : soFiles) {
            if (!f.getName().contains(soFileName) && !f.getName().contains(timestampFileName)) {
                f.delete();
            }
        }

    }


    /**
     * 写入Flutter so 文件信息
     */
    public static void writeSoInfo(File file, FlutterSoEntity soEntity) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(soEntity.appVersion);
        dos.writeInt(soEntity.pluginSoVersion);
        dos.writeUTF(soEntity.appName);
        dos.writeUTF(soEntity.ABIName);
        dos.writeLong(soEntity.lastUpdateTime);
        dos.writeUTF(soEntity.fileMd5);
        dos.close();
        out.close();
    }

    /**
     * 读Flutter so 文件信息
     */
    public static FlutterSoEntity readSoInfo(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        DataInputStream dif = new DataInputStream(in);
        FlutterSoEntity entity = new FlutterSoEntity();
        entity.appVersion = dif.readInt();
        entity.pluginSoVersion = dif.readInt();
        entity.appName = dif.readUTF();
        entity.ABIName = dif.readUTF();
        entity.lastUpdateTime = dif.readLong();
        entity.fileMd5 = dif.readUTF();
        dif.close();
        in.close();
        return entity;
    }

    private static String checkTimestamp(@NonNull File dataDir,
                                         @NonNull FlutterSoEntity flutterSoEntity,OnUpdateListener updateListener,Handler handler) {

        boolean supported = false;
        //验证设备是否支持当前的so库
        if (SUPPORTED_ABIS != null) {
            for (String abi : SUPPORTED_ABIS) {
                if (abi.contains(flutterSoEntity.ABIName)) {
                    supported = true;
                    break;
                }
            }
        }
        if (!supported) {
            if (io.flutter.BuildConfig.DEBUG) {
                Log.i(TAG, "当前设备不支持flutter so 文件  abiname=" + flutterSoEntity.ABIName);
            }
            if(updateListener!=null){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateListener.onInstallFail(OnUpdateListener.FAIL_ABI,"当前设备不支持"+flutterSoEntity.ABIName);
                    }
                });
            }
            return null;
        }
        String expectedTimestamp = FileUtils.generateTimestampFileName(flutterSoEntity);

        final String[] existingTimestamps = getExistingTimestamps(dataDir);

        if (existingTimestamps == null) {
            if (io.flutter.BuildConfig.DEBUG) {
                Log.i(TAG, "应用目录中没有更新过flutter so 文件");
            }
            return expectedTimestamp;
        }

        if (existingTimestamps.length == 0) {
            if (io.flutter.BuildConfig.DEBUG) {
                Log.i(TAG, "应用目录中没有更新过flutter so 文件");
            }
            return expectedTimestamp;
        }

        if (Arrays.asList(existingTimestamps).contains(expectedTimestamp)) {
            if (io.flutter.BuildConfig.DEBUG) {
                Log.i(TAG, "所要更新的flutter so 文件 已经存在");
            }
            if(updateListener!=null){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateListener.onInstallFail(OnUpdateListener.ALREADY_UPDATE,"已经是最新版本");
                    }
                });
            }
            return null;
        }
        return expectedTimestamp;
    }
}
