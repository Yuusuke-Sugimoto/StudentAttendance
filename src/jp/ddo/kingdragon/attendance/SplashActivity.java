package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jp.ddo.kingdragon.attendance.student.StudentMaster;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;

/**
 * スプラッシュ画面
 * 参考:Androidアプリでスプラッシュ画面を表示させる方法 - MIRAI THE FUTURE
 *      http://d.hatena.ne.jp/yamamotodaisaku/20100126/1264504434
 * @author 杉本祐介
 */
public class SplashActivity extends Activity {
    // 定数の宣言
    /**
     * 使用する文字コード
     */
    private static final String CHARACTER_CODE = "Shift_JIS";

    // 変数の宣言
    /**
     * 他スレッドからのUIの更新に使用
     */
    private Handler mHandler;

    /**
     * ベースフォルダ
     */
    private File baseDir;
    /**
     * マスタフォルダ
     */
    private File masterDir;

    /**
     * 学生マスタ読み込み状況表示用のProgressBar
     */
    private ProgressBar progressBarForRefresh;
    /**
     * 学生マスタ読み込み状況表示用のTextView
     */
    private TextView textViewForRefresh;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        mHandler = new Handler();

        // 各フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        masterDir = new File(baseDir, "StudentMaster");
        if (!masterDir.exists() && !masterDir.mkdirs()) {
            Toast.makeText(SplashActivity.this, R.string.error_make_master_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        progressBarForRefresh = (ProgressBar)findViewById(R.id.progress_bar);
        textViewForRefresh = (TextView)findViewById(R.id.progress_text);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CustomApplication application = (CustomApplication)getApplication();
                    application.setStudentMaster(new StudentMaster(masterDir, SplashActivity.CHARACTER_CODE, new StudentMaster.OnRefreshListener() {
                        @Override
                        public void onRefreshBegin(final int num) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBarForRefresh.setMax(num);
                                }
                            });
                        }

                        @Override
                        public void onOpenBegin(final String fileName) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    textViewForRefresh.setText(fileName + getString(R.string.notice_student_master_opening));
                                }
                            });
                        }

                        @Override
                        public void onOpenFinish(String fileName) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBarForRefresh.incrementProgressBy(1);
                                }
                            });
                        }

                        @Override
                        public void onRefreshFinish() {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBarForRefresh.setProgress(progressBarForRefresh.getMax());
                                    textViewForRefresh.setText(R.string.notice_refresh_finish);
                                }
                            });

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent mIntent;
                                    PreferenceUtil mPreferenceUtil = new PreferenceUtil(SplashActivity.this);
                                    if (!mPreferenceUtil.isDisasterModeEnabled()) {
                                        mIntent = new Intent(SplashActivity.this, StudentAttendanceActivity.class);
                                    }
                                    else {
                                        mIntent = new Intent(SplashActivity.this, DisasterModeActivity.class);
                                    }
                                    startActivity(mIntent);

                                    finish();
                                }
                            }, 1000);
                        }

                        @Override
                        public void onError(final String fileName, final IOException e) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SplashActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                                    Log.e("onCreate", e.getMessage(), e);
                                }
                            });
                        }
                    }));
                }
                catch (UnsupportedEncodingException e) {
                    Toast.makeText(SplashActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();
                    Log.e("onCreate", e.getMessage(), e);

                    finish();
                }
            }
        }).start();
    }
}