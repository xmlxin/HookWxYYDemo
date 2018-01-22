package com.example.admin.hookwxyydemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaSyncEvent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.example.admin.hookwxyydemo.CommandUtils.execCommand;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;

/**
 * Created by admin on 2018/1/11.
 */

public class Module implements IXposedHookLoadPackage {

    private static final String TAG = "Module";


    private HashMap<AudioRecord, FileOutputStream> mFosMap = new HashMap<>();




    private Context applicationContext;


    int mNum = 0;





    private HashMap<AudioRecord, String> mPcmFileMap = new HashMap<>();
//    private HashMap<AudioRecord, String> mAmrFileMap = new HashMap<>();

   // ContentResolver mResolver;
   // Uri uri = Uri.parse("content://com.example.admin.hookwxyydemo.provider");

    // 指定模式  1为录音  0为发送指定语音
    private static volatile int sMode = -1;


    // AudioRecord的getRecordingState的值的维护
    private HashMap<AudioRecord,Integer> mRecordingFlagMap = new HashMap<>();


    // 将语音录到指定的pcm文件
    private static String sRecordPcmFileName;

    // 发送指定的pcm文件
    private static String sSendPcmFileName;



    private boolean m_isInit = false;


    private HashMap<AudioRecord,FileInputStream> mFisMap = new HashMap<>();


    /**
     *  使得每次微信发语音 都发出指定的pcm语音
     * @param pcmFile  要发送的pcm文件目录 例如：/data/local/tmp/1.pcm
     * @return
     */
    public static int pushAudio(String pcmFile){
        sSendPcmFileName = pcmFile;
        sMode = 0;
        return 0;
    }



    /**
     *  将微信语音录到指定目录下
     * @param pcmFile  输出到指定的文件目录 例如：/data/local/tmp/hhh.pcm
     * @return
     */
    public static int recordAudio(String pcmFile){
        sRecordPcmFileName = pcmFile;
        sMode = 1;
        return 0;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        try {

//        Log.i(TAG, "handleLoadPackage: 开始Xposed");
            if (loadPackageParam.packageName.equals("com.tencent.mm") && m_isInit==false) {
                Log.i(TAG, "handleLoadPackage: 进来了com.tencent.mm方法");

                Runtime.getRuntime().exec("su");

                // TODO
//                recordAudio("/data/local/tmp/zzz.pcm");
                    pushAudio("/data/local/tmp/1.pcm");


//                if (sMode == 1){
//                    recordAudio(sRecordPcmFileName);
//                }else if(sMode == 0){
//                    pushAudio(sSendPcmFileName);
//                }

                Log.i(TAG, "handleLoadPackage: sMode ---- " + sMode);


//                // 获取到当前进程的上下文
//                try {
//                    Class<?> ContextClass = findClass("android.content.ContextWrapper", loadPackageParam.classLoader);
//                    findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            super.afterHookedMethod(param);
//                            applicationContext = (Context) param.getResult();
//                            XposedBridge.log("得到上下文");
//
////                            mResolver = applicationContext.getContentResolver();
////                            Cursor cursor = mResolver.query(uri, null, null, null, null);
////                            if (cursor!=null){
////                                Bundle bundle = cursor.getExtras();
////                                if (bundle != null) {
////                                    sMode = bundle.getInt("flag");
////
////                                    sMode = 0;
////                                    Log.i(TAG, "flag 的值为 "+sMode);
////                                }
////                            }
//                        }
//                    });
//                } catch (Throwable throwable) {
//                    XposedBridge.log("获取上下文失败 " + throwable);
//                }


////                //---------- 模拟器版本 ----------
                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "startRecording",
                        new XC_MethodHook() {


                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (sMode == 0) {
                                    try {
                                        // 修改要发送的语音文件的权限
                                        File file = new File(sSendPcmFileName);
                                        String parentName = file.getParent();
                                        Log.i(TAG, "parentName: " + parentName);


                                        File fileParent = file.getParentFile();
                                        if (!fileParent.exists()) {
                                            fileParent.mkdirs();
                                        }
                                        execCommand("chmod 777 " + parentName, true);
                                        file.createNewFile();

                                        execCommand("chmod 777 " + sSendPcmFileName, true);

                                        Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod ");


                                        AudioRecord record = (AudioRecord) param.thisObject;
                                        int flag = -1;

                                        Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod : 1");

                                        // 将录音状态置为start状态
                                        if (mRecordingFlagMap.get(record) == null || mRecordingFlagMap.get(record) != AudioRecord.RECORDSTATE_RECORDING) {
                                            flag = AudioRecord.RECORDSTATE_RECORDING;
                                            mRecordingFlagMap.put(record, flag);
                                            Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod : AudioRecord -- " + record);
                                            Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod : flag -- " + flag);
                                            Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod : 2");
                                        }

                                        Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod : 3");

                                        Object o = new Object();
                                        param.setResult(o);


                                    } catch (Exception e) {
                                        Log.i(TAG, "AudioRecord #  startRecording  beforeHookedMethod  出错");
                                        Log.i(TAG, "出错原因 —— " + e.getMessage());
                                    }
                                }

                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {


                                try {
                                    Log.i(TAG, "AudioRecord #  startRecording  afterHookedMethod ");
                                    Log.i(TAG, "AudioRecord 对象 : " + param.thisObject);

                                    Throwable throwable = param.getThrowable();
                                    if (throwable != null) {
                                        Log.i(TAG, "AudioRecord startRecording afterHookedMethod 的 throwable1 ---  : " + throwable.getMessage());
                                        Log.i(TAG, "AudioRecord startRecording afterHookedMethod 的 throwable2 ---  : " + throwable.toString());
                                        Log.i(TAG, "AudioRecord startRecording afterHookedMethod 的 throwable3 ---  : " + throwable.getStackTrace());
                                    }
                                } catch (Exception e) {
                                    Log.i(TAG, "AudioRecord #  startRecording  afterHookedMethod  出错");
                                    Log.i(TAG, "出错原因 —— " + e.getMessage());
                                }

                            }
                        });

                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "getRecordingState",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (sMode == 0) {
                                    try {

                                        Log.i(TAG, "AudioRecord # getRecordingState beforeHookedMethod ");


                                        AudioRecord record = (AudioRecord) param.thisObject;

                                        int res = mRecordingFlagMap.get(record) == null ? AudioRecord.RECORDSTATE_STOPPED : mRecordingFlagMap.get(record);

                                        Log.i(TAG, "AudioRecord # getRecordingState beforeHookedMethod 返回设置为 " + res);
                                        param.setResult(res);

                                        // 清理mRecordFlagMap
                                        mRecordingFlagMap.remove(record);


                                    } catch (Exception e) {
                                        Log.i(TAG, "AudioRecord # getRecordingState beforeHookedMethod 出错: " + e.getMessage());
                                    }
                                }

                            }
                        });
////                  --------------------------


                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", byte[].class, int.class,
                        int.class, new XC_MethodHook() {


                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                                try {
                                    Log.i(TAG, "AudioRecord # read (byte[],int,int) 进来了beforeHookedMethod方法");
                                    Log.i(TAG, "beforeHookedMethod: sMode " + sMode);
                                    if (sMode == 0) {

                                        AudioRecord record = (AudioRecord) param.thisObject;
                                        byte[] buffer = (byte[]) param.args[0];
                                        int off = (int) param.args[1];
                                        int size = (int) param.args[2];

                                        FileInputStream fis = null;

                                        // 指定发送的语音文件
                                        if (mFisMap.get(record)==null) {
                                            fis = new FileInputStream(sSendPcmFileName);
                                            mFisMap.put(record,fis);
                                        }else {
                                            fis = mFisMap.get(record);
                                        }

                                        Log.i(TAG, "beforeHookedMethod: mFis: " + fis.toString());
                                        Log.i(TAG, "beforeHookedMethod: buffer: " + buffer.toString());
                                        Log.i(TAG, "beforeHookedMethod: offset: " + off);
                                        Log.i(TAG, "beforeHookedMethod: size: " + size);

                                        int min = Math.min(buffer.length - off, size);
                                        Log.i(TAG, "beforeHookedMethod: min: " + min);
                                        byte[] bytes = new byte[min];
                                        int res = fis.read(bytes);

                                        Log.i(TAG, "beforeHookedMethod: res ----- " + res);
                                        if (res == -1) {
                                            param.setResult(0);

                                        } else {
                                            for (int i = 0; i < bytes.length; i++) {
                                                buffer[off + i] = bytes[i];
                                            }
                                            param.setResult(res);

                                        }


                                    }
                                } catch (Exception e) {
                                    Log.i(TAG, "beforeHookedMethod: e --- " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                try {
                                    Log.i(TAG, " AudioRecord # read (byte,int,int)  进来了afterHookedMethod方法");
                                    Log.i(TAG, "afterHookedMethod: sMode " + sMode);
                                    if (sMode == 1) {

                                        FileOutputStream fileOutputStream =null;
                                        AudioRecord record = (AudioRecord) param.thisObject;
                                        Log.i(TAG, "afterHookedMethod: mFileAudioRecord: " + record);
                                        byte[] buffer = (byte[]) param.args[0];
                                        Integer integer = (Integer) param.args[1];
                                        int offset = integer.intValue();
                                        Integer integer2 = (Integer) param.args[2];
                                        int size = integer2.intValue();
                                        Log.i(TAG, "afterHookedMethod: buffer: " + buffer.toString());
                                        Log.i(TAG, "afterHookedMethod: offset: " + offset);
                                        Log.i(TAG, "afterHookedMethod: size: " + size);

//                                        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                                            ActivityCompat.requestPermissions((Activity) applicationContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//                                        }


                                        if (mFosMap.get(record) == null) {
                                            Log.i(TAG, "get 为null ");

                                            execCommand("chmod 777 /data/local/tmp",true);

                                            String pcmFileName = "myPcmFile";
                                            // String amrFileName = "myAmrFile";
                                            mNum++;
                                            Log.i(TAG, "  mNum++  此时的num为 " + mNum);
                                            pcmFileName = pcmFileName + mNum;
                                            //  amrFileName = amrFileName + mNum;

                                            File file = new File("/data/local/tmp/" + pcmFileName + ".pcm");
                                            File fileParent = file.getParentFile();
                                            if (!fileParent.exists()) {
                                                fileParent.mkdirs();
                                            }
                                            file.createNewFile();
                                            Log.i(TAG, "pcmFileName: " + pcmFileName);
                                            fileOutputStream = new FileOutputStream("/data/local/tmp/" + pcmFileName + ".pcm");
                                            mFosMap.put(record, fileOutputStream);
                                            mPcmFileMap.put(record, pcmFileName);
                                            //  mAmrFileMap.put(mFileAudioRecord, amrFileName);

                                        } else {
                                            Log.i(TAG, "get 不为null ");
                                            fileOutputStream = mFosMap.get(record);
                                        }

                                        // 写入文件
                                        int read = (int) param.getResult();
                                        Log.i(TAG, "read: " + read);


                                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                            byte[] bytes = new byte[read];

                                            for (int i = 0; i < bytes.length; i++) {
                                                bytes[i] = buffer[i + offset];
                                            }
                                            fileOutputStream.write(bytes);
                                        }

                                        Log.i(TAG, "afterHookedMethod: 写入完成 fileOutputStream: " + fileOutputStream.toString());


                                    }
                                } catch (Exception e) {
                                    Log.i(TAG, "afterHookedMethod Exception: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });


                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "stop", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        try {

                            if (sMode == 0) {

                                AudioRecord record = (AudioRecord) param.thisObject;
                                if (mFisMap.get(record) != null) {
                                    FileInputStream fis = mFisMap.get(record);
                                    fis.close();
                                    mFisMap.remove(record);
                                }


                                Log.i(TAG, "AudioRecord #stop 里的 beforeHookedMethod ");


                                // --------------模拟器版本----------

                                // 将录音状态设置为stopped

                                int flag = -1;

                                Log.i(TAG, "AudioRecord #  stop  beforeHookedMethod : 1");

                                if (mRecordingFlagMap.get(record) == null || mRecordingFlagMap.get(record) != AudioRecord.RECORDSTATE_STOPPED) {
                                    flag = AudioRecord.RECORDSTATE_STOPPED;
                                    mRecordingFlagMap.put(record, flag);
                                    Log.i(TAG, "AudioRecord #  stop  beforeHookedMethod : AudioRecord -- " + record);
                                    Log.i(TAG, "AudioRecord #  stop  beforeHookedMethod : flag -- " + flag);
                                    Log.i(TAG, "AudioRecord #  stop  beforeHookedMethod : 2");
                                }

                                Log.i(TAG, "AudioRecord #  stop  beforeHookedMethod : 3");


                                Object o = new Object();
                                param.setResult(o);
//                          --------------------------
                            }

                        } catch (Exception e) {
                            Log.i(TAG, "AudioRecord  # stop 里的 beforeHookedMethod出错 : " + e.getMessage());
                        }


                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        try {

                            Log.i(TAG, "AudioRecord #stop 里的 afterHookedMethod ");

                            Throwable throwable = param.getThrowable();
                            if (throwable!=null){

                                Log.i(TAG, "AudioRecord stop afterHookedMethod 的 throwable ---  : " + throwable.getMessage());

                            }

                            if (sMode == 1) {

                                AudioRecord record = (AudioRecord) param.thisObject;
                                if (mFosMap.get(record) != null) {
                                    FileOutputStream fos = mFosMap.get(record);
                                    fos.close();
                                    mFosMap.remove(record);

                                    // 将pcm转为amr

//                                File amrFile = new File("/data/local/tmp/myAmr.amr");
//                                File amrFileParent = amrFile.getParentFile();
//                                if (!amrFileParent.exists()) {
//                                    amrFileParent.mkdirs();
//                                }
//                                amrFile.createNewFile();

                                    String pcmFileName = mPcmFileMap.get(record);
//                                    String amrFileName = mAmrFileMap.get(record);

//
//                                    PcmToAmr.pcm2Amr("/data/local/tmp/" + pcmFileName + ".pcm", "/data/local/tmp/" + amrFileName + ".amr");
                                    execCommand("chmod 777 /data/local/tmp/" + pcmFileName + ".pcm", true);
//                                    File file1 = new File(sRecordPcmFileName);
//                                    // 删除原来的
//                                    if (file1.exists()) {
//                                        file1.delete();
//                                    }
                                    // 覆盖拷贝
                                    execCommand("\\cp /data/local/tmp/" + pcmFileName + ".pcm  " + sRecordPcmFileName, true);
                                    Log.i(TAG, "命令 : \\cp /data/local/tmp/" + pcmFileName + ".pcm  " + sRecordPcmFileName);
                                    execCommand("chmod 777 " + sRecordPcmFileName, true);




                                    mPcmFileMap.remove(record);
//                                    mAmrFileMap.remove(record);
                                }

                            }


                        } catch (Exception e) {
                            Log.i(TAG, "AudioRecord  # stop 里的 afterHookedMethod出错 : " + e.getMessage());
                        }
                    }

                });


//                // ----------模拟器版----------
                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "release",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (sMode == 0) {
                                    Log.i(TAG, "AudioRecord # release beforeHookedMethod ");
                                    Object o = new Object();
                                    param.setResult(o);
                                }
                            }
                        });

//                --------------------
                m_isInit = true;

//
//


//                // Api23
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", byte[].class, int.class,
//                        int.class, int.class, new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (byte[],int,int,int)" + param.getResult().toString());
//
//                                Log.i(TAG, "afterHookedMethod: object  ----  " + param.thisObject.toString());
//
//
//                                Log.i(TAG, "params -- 1 : " + param.args[0] + " 2: " + param.args[1]
//                                        + " 3: " + param.args[2] + " 4: " + param.args[3]);
////                                int read = mFileAudioRecord.read(buffer, 0, mMinBufferSize);
////                                for (int i = 0; i < read; i++) {
////                                    byte b = buffer[i];
////                                    // 写到文件
////                                    outputStream.write(buffer, 0, read);
////                                }
//
////                                byte[] audioData, int offsetInBytes, int sizeInBytes
//
//
////                                byte[] bytes = (byte[]) param.args[0];
////                                Integer integer = (Integer) param.args[1];
////                                int offsetInBytes = integer.intValue();
////                                Integer integer2 = (Integer) param.args[2];
////                                int sizeInBytes = integer2.intValue();
////
////
////                                mFileAudioRecord = (AudioRecord) param.thisObject;
//
////
//////
//////                                XposedHelpers.findAndHookMethod(FileOutputStream.class, "write",byte[].class,int.class,int.class,
//////                                        new XC_MethodHook() {
//////                                            @Override
//////                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//////                                                Log.i(TAG, "beforeHookedMethod: FileOutputStream "+param.args[0]+" , "+param.args[1]+" , "+param.args[2]);
//////                                                Log.i(TAG, "beforeHookedMethod: object "+param.thisObject);
//////                                                File file = (File) param.thisObject;
//////                                                Log.i(TAG, "file.getAbsolutePath() -------- "+file.getAbsolutePath() );
//////                                                Log.i(TAG, "file.getPath()------- "+file.getPath());
//////                                                Log.i(TAG, "file.getCanonicalPath()------- "+file.getCanonicalPath());
//////                                                Log.i(TAG, "file.getName(): "+file.getName());
//////
//////
//////                                            }
//////
//////                                            @Override
//////                                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//////                                                Log.i(TAG, "afterHookedMethod: FileOutputStream ");
//////
//////                                            }
//////                                        });
////                            }
////                        });
//
//
////                XposedHelpers.findAndHookConstructor("java.io.File", loadPackageParam.classLoader,
////                        String.class, int.class,"File",  new XC_MethodHook() {
////                            @Override
////                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "beforeHookedMethod: File (String,int) --- ("+ param.args[0]+","+param.args[1]+")");
////                            }
////
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "afterHookedMethod: File (String,int)"+param.getResult().toString());
////                            }
////                        });
//
//
////
////                XposedHelpers.findAndHookConstructor("java.io.File", loadPackageParam.classLoader,
////                         String.class, File.class, "File",new XC_MethodHook() {
////                            @Override
////                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "beforeHookedMethod: File (String,File) --- ("+ param.args[0]+","+param.args[1]+")");
////                            }
////
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "afterHookedMethod: File (String,File)"+param.getResult().toString());
////                            }
////                        });
////
////                XposedHelpers.findAndHookConstructor("java.io.File", loadPackageParam.classLoader,
////                         String.class,"File", new XC_MethodHook() {
////                            @Override
////                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "beforeHookedMethod: File (String) --- ("+ param.args[0]+")");
////                            }
////
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "afterHookedMethod: File (String)"+param.getResult().toString());
////                            }
////                        });
////
////                XposedHelpers.findAndHookConstructor("java.io.File", loadPackageParam.classLoader,
////                        String.class,String.class, "File", new XC_MethodHook() {
////                            @Override
////                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "beforeHookedMethod: File (String,String) --- ("+ param.args[0]+","+param.args[1]+")");
////                            }
////
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "afterHookedMethod: File (String,String)"+param.getResult().toString());
////                            }
////                        });
//
////                XposedHelpers.findAndHookConstructor("java.io.File", loadPackageParam.classLoader,
////                         URI.class, "File",new XC_MethodHook() {
////                            @Override
////                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "beforeHookedMethod: File (URI) --- ("+ param.args[0]+")");
////                            }
////
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "afterHookedMethod: File (URI)"+param.getResult().toString());
////                            }
////                        });
//
//
////                XposedHelpers.findAndHookConstructor("android.media.MediaRecorder", loadPackageParam.classLoader, new XC_MethodHook() {
////                    @Override
////                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
////                        super.beforeHookedMethod(param);
////                        Log.i(TAG, "beforeHookedMethod: xxxxxxxxxxxx");
////                    }
////
////                    @Override
////                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                        Log.i(TAG, "afterHookedMethod: findAndHookConstructor  "+param.getResult().toString());
////                        XposedBridge.log("findAndHookConstructor  "+param.getResult().toString());
////                    }
////                });
//
//
////                XposedHelpers.findAndHookMethod("android.media.MediaRecorder",
////                        loadPackageParam.classLoader, "setAudioSource", int.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Object object = param.getResult();
////                                Log.i(TAG, "setAudioSource: object  " + object.toString());
////                                XposedBridge.log("setAudioSource: object  " + object.toString());
////                            }
////                        });
////
////                XposedHelpers.findAndHookMethod("android.media.MediaRecorder",
////                        loadPackageParam.classLoader, "setOutputFormat", int.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Object object = param.getResult();
////                                Log.i(TAG, "setOutputFormat: object  " + object.toString());
////                                XposedBridge.log("setOutputFormat: object  " + object.toString());
////                            }
////                        });
////
////                XposedHelpers.findAndHookMethod("android.media.MediaRecorder",
////                        loadPackageParam.classLoader, "setAudioEncoder", int.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Object object = param.getResult();
////                                Log.i(TAG, "setAudioEncoder: object  " + object.toString());
////                                XposedBridge.log("setAudioEncoder: object  " + object.toString());
////                            }
////                        });
////
////
////                XposedHelpers.findAndHookMethod("android.media.MediaRecorder",
////                        loadPackageParam.classLoader, "setOutputFile", String.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Object object = param.getResult();
////                                Log.i(TAG, "setOutputFile: object  " + object.toString());
////                                XposedBridge.log("setOutputFile: object  " + object.toString());
////                            }
////                        });
////
////                XposedHelpers.findAndHookMethod("android.media.MediaRecorder",
////                        loadPackageParam.classLoader, "setMaxDuration", int.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Object object = param.getResult();
////                                Log.i(TAG, "setMaxDuration: object  " + object.toString());
////                                XposedBridge.log("setMaxDuration: object  " + object.toString());
////                            }
////                        });
//
////            Class<?> ContextClass = findClass("android.content.ContextWrapper",loadPackageParam.classLoader);
////            findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook()
//
//
////                XposedHelpers.findAndHookMethod(android.media.MediaRecorder.class, "prepare", new XC_MethodHook() {
////                    @Override
////                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                        Log.i(TAG, "prepare : " + param.getResult().toString());
////                        XposedBridge.log("prepare : " + param.getResult().toString());
////                    }
////                });
//
//
//                                // 这个方法是在23Api的。所以这里报错了java.lang.NoSuchMethodError: android.media.AudioManager#getDevices(int)#exact
////                XposedHelpers.findAndHookMethod("android.media.AudioManager", loadPackageParam.classLoader,
////                        "getDevices", int.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////
////                                Log.i(TAG, "AudioManager # getDevices ");
////                            }
////                        });
//
////                XposedHelpers.findAndHookMethod("android.media.AudioManager", loadPackageParam.classLoader,
////                        "setSpeakerphoneOn", boolean.class, new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////
////                                boolean flag = (boolean) param.args[0];
////                                Log.i(TAG, "AudioManager # setSpeakerphoneOn");
////                                Log.i(TAG, "AudioManager # setSpeakerphoneOn  --- 参数 " + flag);
////                            }
////                        });
//
//
////                // Api 26
////                XposedHelpers.findAndHookMethod("android.media.AudioManager", loadPackageParam.classLoader,
////                        "registerAudioPlaybackCallback", AudioManager.AudioPlaybackCallback.class, Handler.class,
////                        new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "AudioManager # registerAudioPlaybackCallback ");
////                            }
////                        });
////
////
////                // Api 24
////                XposedHelpers.findAndHookMethod("android.media.AudioManager", loadPackageParam.classLoader,
////                        "registerAudioRecordingCallback", AudioManager.AudioPlaybackCallback.class,Handler.class,
////                        new XC_MethodHook() {
////                            @Override
////                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
////                                Log.i(TAG, "AudioManager # unregisterAudioPlaybackCallback ");
////                            }
////                        });
//                            }
//                        });


//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", short[].class, int.class, int.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (short[],int,int): ");
//                            }
//                        }
//                );

//                //Api 23
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", short[].class,int.class, int.class, int.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (short[],int,int,int): ");
//                            }
//                        }
//                );

//                //Api 23
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", float[].class,int.class, int.class, int.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (float[],int,int,int): ");
//                            }
//                        }
//                );
//
                // Api 3
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", ByteBuffer.class,int.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (ByteBuffer,int): ");
//                            }
//                        }
//                );


//                //Api 23
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "read", ByteBuffer.class,int.class, int.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "afterHookedMethod: AudioRecord #read (ByteBuffer,int,int): ");
//                            }
//                        }
//                );

//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "startRecording", MediaSyncEvent.class,
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "AudioRecord # startRecording(MediaSyncEvent): ");
//                            }
//                        });
//                XposedHelpers.findAndHookMethod("android.media.AudioRecord", loadPackageParam.classLoader, "getState",
//                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                Log.i(TAG, "AudioRecord # : getState");
//                                Log.i(TAG, "AudioRecord # : getState 的结果  ："+param.getResult().toString());
//                            }
//                        });
            }

        } catch (Exception e) {
            Log.i(TAG, "handleLoadPackage Exception: " + e.getMessage());
            e.printStackTrace();
        }


    }
}
