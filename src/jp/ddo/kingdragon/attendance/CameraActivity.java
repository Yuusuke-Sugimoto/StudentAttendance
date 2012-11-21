package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * カメラ画面
 * @author 杉本祐介
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback,
                                                        SensorEventListener {
    // 定数の宣言
    // 撮影モード
    public static final int CAPTURE_MODE_PHOTO = 0;
    public static final int CAPTURE_MODE_MOVIE = 1;
    // Intent用
    public static final String CAPTURE_MODE = "CaptureMode";
    public static final String MEDIA_PATH   = "MediaPath";

    // 変数の宣言
    /** 撮影中かどうか */
    private volatile boolean isCapturing;
    /** オートフォーカス中かどうか */
    private boolean isFocusing;
    /** オートフォーカス済みかどうか */
    private boolean isFocused;
    /** カメラが起動したかどうか */
    private boolean isCameraLaunched;

    /** ベースフォルダ */
    private File baseDir;
    /** メディアフォルダ */
    private File mediaDir;

    /** 撮影ボタン */
    private ImageButton captureButton;
    /** プレビュー部分 */
    private SurfaceView preview;
    /** カメラのインスタンス */
    private Camera mCamera;
    /** カメラのパラメータ */
    private Camera.Parameters params;
    /** 動画の撮影に使用 */
    private MediaRecorder recorder;
    /** 表示中の画面の向き */
    private int rotation;
    /** 撮影した動画ファイル */
    private File destMovieFile;
    /** 撮影モード */
    private int captureMode;

    /** 設定内容の読み取り/変更に使用 */
    private PreferenceUtil mPreferenceUtil;

    /** センサマネージャ */
    private SensorManager mSensorManager;
    /** 加速度センサ */
    private Sensor mAccelerometer;
    /** 地磁気センサ */
    private Sensor mMagneticField;

    /** 地磁気センサによって読み取られた値が格納される */
    private float[] magneticValues;
    /** 加速度センサによって読み取られた値が格納される */
    private float[] accelValues;
    /** 現在の各方向の傾きが格納される */
    private int[] degrees;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        // 各種変数の初期化
        isCapturing = false;
        isFocusing = false;
        isFocused = false;
        isCameraLaunched = false;

        mPreferenceUtil = new PreferenceUtil(CameraActivity.this);

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        File webDir = new File(baseDir, "WebDoc");
        mediaDir = new File(webDir, "Media");
        if (!mediaDir.exists() && !mediaDir.mkdirs()) {
            Toast.makeText(CameraActivity.this, R.string.error_make_media_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        Intent mIntent = getIntent();
        captureMode = mIntent.getIntExtra(CAPTURE_MODE, CAPTURE_MODE_PHOTO);

        rotation = 0;
        magneticValues = null;
        accelValues = null;
        degrees = new int[3];

        captureButton = (ImageButton)findViewById(R.id.capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCameraLaunched) {
                    switch (captureMode) {
                        case CameraActivity.CAPTURE_MODE_PHOTO: {
                            if (!isCapturing) {
                                synchronized (CameraActivity.class) {
                                    if (!isCapturing) {
                                        // 撮影中でなければ撮影
                                        isCapturing = true;
                                        captureButton.setEnabled(false);
                                        if (!mPreferenceUtil.isTakeAutoFocusEnable() || isFocused) {
                                            mCamera.takePicture(null, null, null, CameraActivity.this);
                                        }
                                        else {
                                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                                @Override
                                                public void onAutoFocus(boolean success, Camera camera) {
                                                    mCamera.takePicture(null, null, null, CameraActivity.this);
                                                }
                                            });
                                        }
                                    }
                                }
                            }

                            break;
                        }
                        case CameraActivity.CAPTURE_MODE_MOVIE: {
                            if (isCapturing) {
                                recorder.stop();
                                recorder.reset();
                                recorder.release();
                                mCamera.lock();
                                isCapturing = false;
                                isCameraLaunched = false;
                                surfaceChanged(preview.getHolder(), 0, 0, 0);
                                onMovieTaken(destMovieFile);
                            }
                            else {
                                initRecorder();
                                recorder.start();
                                isCapturing = true;
                            }

                            break;
                        }
                    }
                }
            }
        });

        preview = (SurfaceView)findViewById(R.id.preview);
        preview.getHolder().addCallback(CameraActivity.this);
        preview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        preview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 画面がタッチされたらオートフォーカスを実行
                if (isCameraLaunched && mPreferenceUtil.isTapAutoFocusEnable() && !isFocusing
                    && captureMode == CameraActivity.CAPTURE_MODE_PHOTO) {
                    // オートフォーカス中でなければオートフォーカスを実行
                    // フラグを更新
                    isFocusing = true;

                    captureButton.setEnabled(false);
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            isFocusing = false;
                            isFocused = true;
                            captureButton.setEnabled(true);
                        }
                    });
                }
            }
        });

        // 設定情報にデフォルト値をセットする
        PreferenceManager.setDefaultValues(CameraActivity.this, R.xml.camera_preference, false);

        // 傾きを検出するための設定
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onResume() {
        super.onResume();

        // 傾きを検出するためのリスナを登録
        mSensorManager.registerListener(CameraActivity.this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(CameraActivity.this, mMagneticField, SensorManager.SENSOR_DELAY_UI);

        surfaceChanged(preview.getHolder(), 0, 0, 0);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(CameraActivity.this);

        if (mCamera != null) {
            // カメラのリソースを利用中であれば解放する
            if (captureMode == CameraActivity.CAPTURE_MODE_MOVIE && isCapturing) {
                recorder.stop();
                recorder.reset();
                recorder.release();
                mCamera.lock();
                isCapturing = false;
                onMovieTaken(destMovieFile);
            }
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            params = null;
        }
        isCameraLaunched = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camera_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retBool = false;

        switch (item.getItemId()) {
            case R.id.menu_setting: {
                // 設定画面を開く
                Intent mIntent = new Intent(CameraActivity.this, CameraSettingActivity.class);
                startActivity(mIntent);

                retBool = true;

                break;
            }
        }

        return retBool;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        mCamera.stopPreview();

        // ファイル名を生成
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = "SA_" + dateFormat.format(new Date()) + ".jpg";
        final File destFile = new File(mediaDir, fileName);

        // 生成したファイル名で新規ファイルを登録
        FileOutputStream fos = null;
        try {
            Bitmap mBitmap;
            if (Build.MODEL.equals("Galaxy Nexus")) {
                // Galaxy Nexusでは回転済みの画像が渡されるため。
                mBitmap = Util.rotateBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), 0.0f);
            }
            else {
                mBitmap = Util.rotateBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), rotation);
            }
            fos = new FileOutputStream(destFile.getAbsolutePath());
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        }
        catch (OutOfMemoryError t) {
            Log.e("onPictureTaken", t.getMessage(), t);
        }
        catch (IOException e) {
            Log.e("onPictureTaken", e.getMessage(), e);
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                    Log.e("onPictureTaken", e.getMessage(), e);
                }
            }
        }

        MediaScannerConnection.scanFile(CameraActivity.this, new String[] {destFile.getAbsolutePath()},
                                        new String[] {"image/jpeg"}, null);

        Intent mIntent = new Intent();
        mIntent.putExtra(CameraActivity.MEDIA_PATH, mediaDir.getName() + "/" + destFile.getName());
        setResult(Activity.RESULT_OK, mIntent);

        finish();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        else {
            try {
                mCamera = Camera.open();
                params = null;
                params = mCamera.getParameters();
            }
            catch (RuntimeException e) {
                Toast.makeText(CameraActivity.this, R.string.error_launch_camera_failed, Toast.LENGTH_SHORT).show();
                Log.e("surfaceChanged", e.getMessage(), e);

                finish();
            }
        }

        if (!isCameraLaunched && params != null) {
            // 未設定の時のみサイズの設定を行う
            // 各種パラメータの設定
            if (!mPreferenceUtil.isSupportedPictureSizesSaved()) {
                List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
                Collections.sort(pictureSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return lhs.width * lhs.height - rhs.width * rhs.height;
                    }
                });
                mPreferenceUtil.putSupportedPictureSizes(pictureSizes.toArray(new Camera.Size[pictureSizes.size()]));
                mPreferenceUtil.putSupportedPictureSizesSaved(true);
            }

            if (!mPreferenceUtil.isSupportedPreviewSizesSaved()) {
                List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return lhs.width * lhs.height - rhs.width * rhs.height;
                    }
                });
                mPreferenceUtil.putSupportedPreviewSizes(previewSizes.toArray(new Camera.Size[previewSizes.size()]));
                mPreferenceUtil.putSupportedPreviewSizesSaved(true);
            }

            // 保存する画像サイズを決定
            Camera.Size picSize = null;
            if (captureMode == CameraActivity.CAPTURE_MODE_PHOTO) {
                List<Camera.Size> pictureSizes;
                int selectedPicSize = mPreferenceUtil.getSelectedPictureSize(-1);
                if (selectedPicSize != -1) {
                    int[] pictureSize = mPreferenceUtil.getSupportedPictureSize(selectedPicSize);
                    picSize = mCamera.new Size(pictureSize[PreferenceUtil.WIDTH], pictureSize[PreferenceUtil.HEIGHT]);
                }
                else {
                    pictureSizes = params.getSupportedPictureSizes();
                    picSize = pictureSizes.get(0);
                    for (int i = 1; i < pictureSizes.size(); i++) {
                        Camera.Size tempSize = pictureSizes.get(i);
                        if (picSize.width * picSize.height > PreferenceUtil.DEFAULT_SIZE_WIDTH * PreferenceUtil.DEFAULT_SIZE_HEIGHT
                            || picSize.width * picSize.height < tempSize.width * tempSize.height) {
                            // DEFAULT_SIZE_WIDTH x DEFAULT_SIZE_HEIGHT以下で一番大きな画像サイズを選択
                            picSize = tempSize;
                        }
                    }
                }
                params.setPictureSize(picSize.width, picSize.height);
            }

            // 画像サイズを元にプレビューサイズを決定
            WindowManager manager = (WindowManager)getSystemService(WINDOW_SERVICE);
            Display mDisplay = manager.getDefaultDisplay();
            Camera.Size preSize = null;
            List<Camera.Size> previewSizes;
            previewSizes = params.getSupportedPreviewSizes();
            preSize = previewSizes.get(0);
            for (int i = 1; i < previewSizes.size(); i++) {
                Camera.Size tempSize = previewSizes.get(i);
                if (preSize.width * preSize.height > mDisplay.getWidth() * mDisplay.getHeight()
                    || captureMode == CameraActivity.CAPTURE_MODE_PHOTO
                       && (preSize.width * preSize.height < tempSize.width * tempSize.height)
                           && (Math.abs((double)picSize.width / (double)picSize.height - (double)preSize.width / (double)preSize.height)
                               >= Math.abs((double)picSize.width / (double)picSize.height - (double)tempSize.width / (double)tempSize.height))
                    || captureMode == CameraActivity.CAPTURE_MODE_MOVIE
                       && (preSize.width * preSize.height < tempSize.width * tempSize.height)) {
                    // ディスプレイのサイズ以下で一番保存サイズの比に近く、かつ一番大きなプレビューサイズを選択
                    // 動画モードの場合はディスプレイのサイズ以下かつ一番大きなプレビューサイズを選択
                    preSize = tempSize;
                }
            }
            params.setPreviewSize(preSize.width, preSize.height);

            // プレビューサイズを元にSurfaceViewのサイズを決定
            ViewGroup.LayoutParams lParams = preview.getLayoutParams();
            if (preSize.width <= mDisplay.getWidth() && preSize.height <= mDisplay.getHeight()) {
                lParams.width  = preSize.width;
                lParams.height = preSize.height;
            }
            else {
                lParams.width  = mDisplay.getWidth();
                lParams.height = mDisplay.getHeight();
                if ((double)preSize.width / (double)preSize.height
                    < (double)mDisplay.getWidth() / (double)mDisplay.getHeight()) {
                    // 縦の長さに合わせる
                    lParams.width  = preSize.width * mDisplay.getHeight() / preSize.height;
                }
                else if ((double)preSize.width / (double)preSize.height
                         > (double)mDisplay.getWidth() / (double)mDisplay.getHeight()) {
                    // 横の長さに合わせる
                    lParams.height = preSize.height * mDisplay.getWidth() / preSize.width;
                }
            }
            preview.setLayoutParams(lParams);
            params.setRotation(rotation);
            mCamera.setParameters(params);

            isCameraLaunched = true;
        }

        try {
            mCamera.setPreviewDisplay(preview.getHolder());
            if (Build.MODEL.equals("Galaxy Nexus")) {
                try {
                    // Galaxy Nexusにおいてウェイトを入れないと動作しなかったため。
                    Thread.sleep(300);
                }
                catch (InterruptedException e) {}
            }
            mCamera.startPreview();
        }
        catch (Exception e) {
            Toast.makeText(CameraActivity.this, R.string.error_set_preview_failed, Toast.LENGTH_SHORT).show();
            Log.e("surfaceChanged", e.getMessage(), e);

            finish();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD: {
                magneticValues = event.values.clone();

                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                accelValues = event.values.clone();

                break;
            }
        }

        float[] radians = new float[3];
        if (magneticValues != null && accelValues != null) {
            float[] inR = new float[16];
            float[] outR = new float[16];
            SensorManager.getRotationMatrix(inR, null, accelValues, magneticValues);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, radians);
        }
        for (int i = 0; i < event.values.length; i++) {
            degrees[i] = (int)Math.floor(Math.toDegrees(radians[i]));
        }

        int rotationSetting = mPreferenceUtil.getRotationSetting(PreferenceUtil.ROTATION_AUTO);
        if (captureMode == CameraActivity.CAPTURE_MODE_MOVIE) {
            rotationSetting = PreferenceUtil.ROTATION_NR_LANDSCAPE;
        }
        else if (rotationSetting == PreferenceUtil.ROTATION_USER) {
            // "端末の設定に従う"が選択されている場合、端末の設定を取得する
            if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1) == 1) {
                rotationSetting = PreferenceUtil.ROTATION_AUTO;
            }
            else {
                rotationSetting = PreferenceUtil.ROTATION_PORTRAIT;
            }
        }
        /**
         * 画面を正しい向きに回転させる
         * 表示した直後だとボタンのサイズが取得できないため、captureButton.getWidth() != 0を加えている。
         */
        if (!isCapturing && captureButton.getWidth() != 0) {
            // 設定内容と画面の向きに矛盾がある場合、画面を正しい向きに回転させる
            if (rotationSetting == PreferenceUtil.ROTATION_PORTRAIT && (rotation == 0 || rotation == 180)) {
                changeRotation(90);
            }
            else if ((rotationSetting == PreferenceUtil.ROTATION_LANDSCAPE || rotationSetting == PreferenceUtil.ROTATION_NR_LANDSCAPE)
                    && (rotation == 90 || rotation == 270)) {
                changeRotation(0);
            }

            if (degrees != null && degrees[1] <= 80) {
                if ((rotationSetting == PreferenceUtil.ROTATION_AUTO || rotationSetting == PreferenceUtil.ROTATION_PORTRAIT)
                   && Math.abs(degrees[2]) <= 20) {
                    // 縦
                    if (rotation != 90) {
                        changeRotation(90);
                    }
                }
                else if ((rotationSetting == PreferenceUtil.ROTATION_AUTO || rotationSetting == PreferenceUtil.ROTATION_LANDSCAPE)
                        && degrees[2] >= -110 && degrees[2] <= -70) {
                    // 左に90度傾けた状態
                    if (rotation != 0) {
                        changeRotation(0);
                    }
                }
                else if ((rotationSetting == PreferenceUtil.ROTATION_AUTO || rotationSetting == PreferenceUtil.ROTATION_PORTRAIT)
                        && Math.abs(degrees[2]) >= 160) {
                    // 逆さ
                    if (rotation != 270) {
                        changeRotation(270);
                    }
                }
                else if ((rotationSetting == PreferenceUtil.ROTATION_AUTO || rotationSetting == PreferenceUtil.ROTATION_LANDSCAPE)
                        && degrees[2] >= 70 && degrees[2] <= 110) {
                    // 右に90度傾けた状態
                    if (rotation != 180) {
                        changeRotation(180);
                    }
                }
            }
        }
    }

    /**
     * 画面の向きを回転させる
     * @param inRotation 画面の角度
     */
    private void changeRotation(int inRotation) {
        if (mCamera != null) {
            // Exif情報に書き込む向きを設定
            params.setRotation(inRotation);
            mCamera.setParameters(params);
        }

        // ボタンを回転
        float beginRotation = 360 - rotation;
        float destRotation  = 360 - inRotation;
        if (Math.abs(beginRotation - destRotation) >= 270) {
            if (beginRotation < destRotation) {
                beginRotation += 360;
            }
            else {
                destRotation += 360;
            }
        }
        RotateAnimation animation = new RotateAnimation(beginRotation, destRotation,
                                                        captureButton.getWidth() / 2, captureButton.getHeight() / 2);
        animation.setDuration(200);
        animation.setFillAfter(true);
        captureButton.startAnimation(animation);

        rotation = inRotation;
    }

    /** MediaRecorderを初期化する */
    private void initRecorder() {
        /**
         * 動画を撮影する
         * 参考:Camera | Android Developers
         *      http://developer.android.com/guide/topics/media/camera.html
         *
         *      MediaRecorder | Android Developers
         *      http://developer.android.com/reference/android/media/MediaRecorder.html
         *
         *      MediaRecorderの解像度設定: とくぼーのブログ
         *      http://tokubo.cocolog-nifty.com/blog/2011/07/mediarecorder-4.html
         */
        mCamera.unlock();

        recorder = new MediaRecorder();
        recorder.setCamera(mCamera);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

        int movieQuality = CamcorderProfile.QUALITY_LOW;
        if (mPreferenceUtil.getMovieQuality(PreferenceUtil.QUALITY_LOW) == PreferenceUtil.QUALITY_HIGH) {
            movieQuality = CamcorderProfile.QUALITY_HIGH;
        }
        CamcorderProfile profile = CamcorderProfile.get(movieQuality);
        if (movieQuality == CamcorderProfile.QUALITY_LOW) {
            profile.videoCodec = MediaRecorder.VideoEncoder.H264;
            profile.audioCodec = MediaRecorder.AudioEncoder.AAC;
        }
        Camera.Size preSize = params.getPreviewSize();
        profile.videoFrameWidth  = preSize.width;
        profile.videoFrameHeight = preSize.height;
        recorder.setProfile(profile);

        // ファイル名を生成
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String ext = "mp4";
        if (profile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
            ext = "3gp";
        }
        String fileName = "SA_" + dateFormat.format(new Date()) + "." + ext;
        destMovieFile = new File(mediaDir, fileName);
        recorder.setOutputFile(destMovieFile.getAbsolutePath());

        recorder.setPreviewDisplay(preview.getHolder().getSurface());
        try {
            recorder.prepare();
        }
        catch (IllegalStateException e) {
            Log.e("initRecorder", e.getMessage(), e);
            destMovieFile.delete();

            finish();
        }
        catch (IOException e) {
            Log.e("initRecorder", e.getMessage(), e);
            destMovieFile.delete();

            finish();
        }
    }

    /** 動画撮影完了時に呼び出される */
    private void onMovieTaken(File movieFile) {
        // 生成したファイル名で新規ファイルを登録
        String[] splittedName = movieFile.getName().split("\\.");
        String ext = splittedName[splittedName.length - 1];
        if (ext.equals("3gp")) {
            ext = "3gpp";
        }

        MediaScannerConnection.scanFile(CameraActivity.this, new String[] {movieFile.getAbsolutePath()},
                                        new String[] {"video/" + ext}, null);

        Intent mIntent = new Intent();
        mIntent.putExtra(CameraActivity.MEDIA_PATH, mediaDir.getName() + "/" + destMovieFile.getName());
        setResult(Activity.RESULT_OK, mIntent);

        finish();
    }
}