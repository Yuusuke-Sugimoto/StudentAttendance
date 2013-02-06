package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceListAdapter;
import jp.ddo.kingdragon.attendance.student.AttendanceLocation;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;
import jp.ddo.kingdragon.attendance.student.Student;
import jp.ddo.kingdragon.attendance.student.StudentCounter;
import jp.ddo.kingdragon.attendance.student.StudentMaster;
import jp.ddo.kingdragon.attendance.student.StudentSheet;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * 出席管理画面
 * @author 杉本祐介
 */
public class StudentAttendanceActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CHOOSE_OPEN_FILE = 0;
    private static final int REQUEST_CHOOSE_SAVE_FILE = 1;
    // ダイアログのID
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING       = 0;
    private static final int DIALOG_ASK_OPEN_WITHOUT_SAVING       = 1;
    private static final int DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO = 2;
    private static final int DIALOG_EDIT_SUBJECT                  = 3;
    private static final int DIALOG_EDIT_TIME                     = 4;
    private static final int DIALOG_TOTALIZATION                  = 5;
    private static final int DIALOG_ATTENDANCE_MENU               = 6;
    private static final int DIALOG_ADD_ATTENDANCE_MENU           = 7;
    private static final int DIALOG_CSV_FILE_LIST                 = 8;
    private static final int DIALOG_STUDENT_LIST                  = 9;
    private static final int DIALOG_SEARCH_STUDENT_NO             = 10;
    private static final int DIALOG_INPUT_STUDENT_INFO            = 11;
    private static final int DIALOG_ASK_REGISTER_READ_ID          = 12;
    private static final int DIALOG_REGISTER_ID_MENU              = 13;
    private static final int DIALOG_CSV_FILE_LIST_R               = 14;
    private static final int DIALOG_STUDENT_LIST_R                = 15;
    private static final int DIALOG_SEARCH_STUDENT_NO_R           = 16;
    private static final int DIALOG_READING_BARCODE               = 17;
    private static final int DIALOG_ASK_OVERWRITE                 = 18;
    private static final int DIALOG_FETCHING_LOCATION             = 19;
    private static final int DIALOG_ASK_OPEN_REGISTER             = 20;
    private static final int DIALOG_ASK_OPEN_GPS_PREFERENCE       = 21;
    private static final int DIALOG_REFRESHING_MASTER_FILE        = 22;
    /** 使用する文字コード */
    private static final String CHARACTER_CODE = "Shift_JIS";

    // 変数の宣言
    /** アプリケーションクラス */
    private CustomApplication application;

    /** 他スレッドからのUIの更新に使用 */
    private Handler mHandler;
    /** 自動保存タスク */
    private Runnable autoSaveTask;

    /** 読み取り中かどうか */
    private boolean isReading;
    /** NFCタグを登録中かどうか */
    private boolean isRegistering;
    /** 保存済みかどうか */
    private boolean isSaved;
    /** 保存中かどうか */
    private boolean isSaving;
    /** 現在地を取得中かどうか */
    private boolean isFetchingLocation;
    /** 再読み込み中かどうか */
    private boolean isRefreshing;
    /** 自動保存タスクが実行中かどうか */
    private boolean isAutoSaveRunning;

    /** ベースフォルダ */
    private File baseDir;
    /** マスタフォルダ */
    private File masterDir;
    /** リスト格納用フォルダ */
    private File listDir;
    /** 保存用フォルダ */
    private File saveDir;
    /** 保存先のファイル */
    private File destFile;

    /** キーボード(バーコードリーダ)から入力された内容 */
    private StringBuilder inputBuffer;
    /** 追加待ちの学籍番号 */
    private String readStudentNo;
    /** 追加待ちのNFCタグ */
    private String readNfcId;
    /** 現在扱っている出席データ */
    private Attendance currentAttendance;
    /** 現在編集しているシート */
    private AttendanceSheet mAttendanceSheet;
    /** リストから追加する際に選択されたシート */
    private StudentSheet selectedSheet;
    /** 学生マスタ */
    private StudentMaster master;

    /** 出席データの一覧を表示するビュー */
    private ListView attendanceListView;
    /** 出席データの一覧を表示するアダプタ */
    private AttendanceListAdapter mAttendanceListAdapter;
    /** 出席の種別を指定するスピナー */
    private Spinner attendanceKindSpinner;
    /** 読み取り開始ボタン */
    private Button readStartButton;
    /** 科目名用のTextView */
    private TextView textViewForSubject;
    /** 授業時間用のTextView */
    private TextView textViewForTime;
    /** 出席人数表示用のTextView */
    private TextView textViewForCount;
    /** 科目名用のEditText */
    private EditText editTextForSubject;
    /** 授業時間用のEditText */
    private EditText editTextForTime;
    /** 検索時の学籍番号用のEditText */
    private EditText editTextForStudentNoForSearch;
    /** 手動登録時の学籍番号用のEditText */
    private EditText editTextForStudentNoForManual;
    /** 所属用のEditText */
    private EditText editTextForClassName;
    /** 氏名用のEditText */
    private EditText editTextForStudentName;
    /** カナ用のEditText */
    private EditText editTextForStudentRuby;
    /** NFCタグ登録時の学籍番号用のEditText */
    private EditText editTextForStudentNoForRegister;
    /** 集計表示用のTableLayout */
    private TableLayout tableLayoutForTotalization;
    /** 学生リスト読み込み状況表示用のProgressDialog */
    private ProgressDialog progressDialogForRefresh;
    /** 前回表示時のprogressDialogForRefreshのMaxの値 */
    private int prevMax;

    /** 設定内容の読み取り/変更に使用 */
    private PreferenceUtil mPreferenceUtil;

    /** NFCタグの読み取りに使用 */
    private NfcAdapter mNfcAdapter;
    /** NFCタグの読み取りに使用 */
    private PendingIntent mPendingIntent;

    /** 位置情報を取得するマネージャ */
    private LocationManager mLocationManager;
    /** 位置情報を取得した際のリスナ */
    private LocationListener mLocationListener;
    /** 取得した位置情報 */
    private AttendanceLocation mAttendanceLocation;

    // 配列の宣言
    /** 対応するインテントの種類 */
    private IntentFilter[] filters;
    /** 対応させるタグの一覧 */
    private String[][] techs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.attendance_title);
        setContentView(R.layout.student_attendance);

        application = (CustomApplication)getApplication();

        mHandler = new Handler();
        autoSaveTask = new Runnable() {
            @Override
            public void run() {
                destFile = new File(saveDir, makeFileName());
                saveCsvFileWithConfirmation(destFile, StudentAttendanceActivity.CHARACTER_CODE);

                mHandler.postDelayed(autoSaveTask, mPreferenceUtil.getAutoSaveInterval(3) * 60000);
            }
        };

        isReading = false;
        isRegistering = false;
        isSaved = true;
        isSaving = false;
        isFetchingLocation = false;
        isRefreshing = false;
        isAutoSaveRunning = false;

        mPreferenceUtil = new PreferenceUtil(StudentAttendanceActivity.this);

        // 各フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        masterDir = new File(baseDir, "StudentMaster");
        listDir = new File(baseDir, "StudentList");
        saveDir = new File(mPreferenceUtil.getAttendanceDir(baseDir.getAbsolutePath() + "/AttendanceData"));
        if (!masterDir.exists() && !masterDir.mkdirs()) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_make_master_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        if (!listDir.exists() && !listDir.mkdirs()) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_make_list_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_make_save_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        inputBuffer = new StringBuilder();

        // アクティビティ再生成前のデータがあれば復元する
        int attendanceKind = -1;
        if (savedInstanceState != null) {
            isReading = savedInstanceState.getBoolean("IsReading");
            isRegistering = savedInstanceState.getBoolean("IsRegistering");
            isSaved = savedInstanceState.getBoolean("IsSaved");
            isSaving = savedInstanceState.getBoolean("IsSaving");
            isFetchingLocation = savedInstanceState.getBoolean("IsFetchingLocation");
            isRefreshing = savedInstanceState.getBoolean("IsRefreshing");
            destFile = (File)savedInstanceState.getSerializable("DestFile");
            readStudentNo = savedInstanceState.getString("ReadStudentNo");
            readNfcId = savedInstanceState.getString("ReadNfcId");
            attendanceKind = savedInstanceState.getInt("AttendanceKind");
            currentAttendance = (Attendance)savedInstanceState.getSerializable("CurrentAttendance");
            mAttendanceSheet = (AttendanceSheet)savedInstanceState.getSerializable("AttendanceSheet");
            selectedSheet = (StudentSheet)savedInstanceState.getSerializable("SelectedSheet");
            mAttendanceListAdapter = new AttendanceListAdapter(StudentAttendanceActivity.this, 0, mAttendanceSheet.getAttendanceList());
            mAttendanceLocation = (AttendanceLocation)savedInstanceState.getSerializable("AttendanceLocation");
            prevMax = savedInstanceState.getInt("PrevMax", -1);
        }
        else {
            readStudentNo = null;
            readNfcId = null;
            currentAttendance = null;
            selectedSheet = null;
            mAttendanceSheet = new AttendanceSheet();
            mAttendanceListAdapter = new AttendanceListAdapter(StudentAttendanceActivity.this, 0);
            mAttendanceLocation = null;
            prevMax = -1;
        }

        // 設定情報にデフォルト値をセットする
        PreferenceManager.setDefaultValues(StudentAttendanceActivity.this, R.xml.preference, false);

        /**
         * ListViewのレイアウトを変更する
         * 参考:リストビューをカスタマイズする | Tech Booster
         *      http://techbooster.org/android/ui/1282/
         *
         *      List14.java | Android Develpers
         *      http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/List14.html
         */
        attendanceListView = (ListView)findViewById(R.id.student_list);
        attendanceListView.setSelector(R.drawable.list_selector_background);
        attendanceListView.setAdapter(mAttendanceListAdapter);
        attendanceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentAttendance = (Attendance)parent.getItemAtPosition(position);
                attendanceListView.invalidateViews();
            }
        });
        attendanceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                attendanceListView.performItemClick(view, position, id);
                showDialog(StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU);

                return true;
            }
        });
        if (currentAttendance != null) {
            int position = mAttendanceListAdapter.getPosition(currentAttendance);
            if (position != -1) {
                attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                attendanceListView.setSelection(position);
            }
        }

        attendanceKindSpinner = (Spinner)findViewById(R.id.attendance_kind);
        if (attendanceKind != -1) {
            attendanceKindSpinner.setSelection(attendanceKind);
        }
        readStartButton = (Button)findViewById(R.id.read_start);
        readStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReading) {
                    readStartButton.setText(R.string.attendance_read_start_label);
                    isReading = false;
                }
                else {
                    readStartButton.setText(R.string.attendance_read_finish_label);
                    isReading = true;
                }
            }
        });

        textViewForSubject = (TextView)findViewById(R.id.subject);
        textViewForSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(StudentAttendanceActivity.DIALOG_EDIT_SUBJECT);
            }
        });
        if (mAttendanceSheet.getSubject().length() != 0) {
            textViewForSubject.setText(mAttendanceSheet.getSubject());
        }

        textViewForTime = (TextView)findViewById(R.id.time);
        textViewForTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(StudentAttendanceActivity.DIALOG_EDIT_TIME);
            }
        });
        if (mAttendanceSheet.getTime().length() != 0) {
            textViewForTime.setText(mAttendanceSheet.getTime());
        }

        textViewForCount = (TextView)findViewById(R.id.count);
        textViewForCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(StudentAttendanceActivity.DIALOG_TOTALIZATION);
            }
        });
        textViewForCount.setText(String.valueOf(mAttendanceSheet.getNumOfConfirmedStudents()));

        mNfcAdapter = NfcAdapter.getDefaultAdapter(StudentAttendanceActivity.this);
        mPendingIntent = PendingIntent.getActivity(StudentAttendanceActivity.this, 0,
                                                   new Intent(StudentAttendanceActivity.this, getClass())
                                                   .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // NfcAdapter.ACTION_NDEF_DISCOVEREDだと拾えない
        IntentFilter mFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters = new IntentFilter[] {mFilter};
        // どの種類のタグでも対応できるようにTagTechnologyクラスを指定する
        techs = new String[][] {new String[] {TagTechnology.class.getName()}};

        // 位置情報を取得するための設定
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isFetchingLocation) {
                    try {
                        dismissDialog(StudentAttendanceActivity.DIALOG_FETCHING_LOCATION);
                    }
                    catch (IllegalArgumentException e) {}
                    mAttendanceLocation = new AttendanceLocation(location.getLatitude(), location.getLongitude(),
                                                                 location.getAccuracy());
                    Toast.makeText(StudentAttendanceActivity.this, R.string.notice_location_fetched, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProviderDisabled(String provider) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mPreferenceUtil.isDisasterModeEnabled()) {
            master = application.getStudentMaster();
            if (master == null) {
                Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();

                finish();
            }
            else {
                master.setOnRefreshListener(new StudentMaster.OnRefreshListener() {
                    @Override
                    public void onRefreshBegin(final int num) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                isRefreshing = true;
                                showDialog(StudentAttendanceActivity.DIALOG_REFRESHING_MASTER_FILE);
                                progressDialogForRefresh.setMax(num);
                                progressDialogForRefresh.setProgress(0);
                                progressDialogForRefresh.setMessage("");
                                prevMax = num;
                            }
                        });
                    }

                    @Override
                    public void onOpenBegin(final String fileName) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialogForRefresh.setMessage(fileName + getString(R.string.notice_student_master_opening));
                            }
                        });
                    }

                    @Override
                    public void onOpenFinish(String fileName) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialogForRefresh.incrementProgressBy(1);
                            }
                        });
                    }

                    @Override
                    public void onRefreshFinish() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialogForRefresh.setProgress(progressDialogForRefresh.getMax());
                                progressDialogForRefresh.setMessage(getString(R.string.notice_refresh_finish));

                                try {
                                    dismissDialog(StudentAttendanceActivity.DIALOG_REFRESHING_MASTER_FILE);
                                }
                                catch (IllegalArgumentException e) {}
                                isRefreshing = false;
                            }
                        });
                    }

                    @Override
                    public void onError(final String fileName, final IOException e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialogForRefresh.incrementProgressBy(1);
                                Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                                Log.e("onCreate", e.getMessage(), e);
                            }
                        });
                    }
                });
            }

            if (mNfcAdapter != null) {
                mNfcAdapter.enableForegroundDispatch(StudentAttendanceActivity.this, mPendingIntent, filters, techs);
            }

            if (mPreferenceUtil.isLocationEnabled()) {
                /**
                 * GPSが選択されていてGPSが無効になっている場合、設定画面を表示するか確認する
                 * 参考:[Android] GSPが有効か確認し、必要であればGPS設定画面を表示する。 | 株式会社ノベラック スタッフBlog
                 *      http://www.noveluck.co.jp/blog/archives/159
                 */
                if (mPreferenceUtil.getLocationProvider(PreferenceUtil.LOCATION_PROVIDER_NETWORK) == PreferenceUtil.LOCATION_PROVIDER_NETWORK
                    || mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    startUpdateLocation();
                }
                else {
                    stopUpdateLocation();
                    showDialog(StudentAttendanceActivity.DIALOG_ASK_OPEN_GPS_PREFERENCE);
                }
            }
            else {
                mAttendanceLocation = null;
                stopUpdateLocation();
            }

            if (master.size() != 0) {
                readStartButton.setEnabled(true);
                if (!isReading) {
                    readStartButton.setText(R.string.attendance_read_start_label);
                }
                else {
                    readStartButton.setText(R.string.attendance_read_finish_label);
                }
            }
            else {
                readStartButton.setEnabled(false);
                readStartButton.setText(R.string.attendance_read_start_label);
                isReading = false;
            }

            if (mPreferenceUtil.isAutoSaveEnabled() && !isAutoSaveRunning) {
                mHandler.postDelayed(autoSaveTask, mPreferenceUtil.getAutoSaveInterval(3) * 60000);
                isAutoSaveRunning = true;
            }
        }
        else {
            Intent mIntent = new Intent(StudentAttendanceActivity.this, DisasterModeActivity.class);
            startActivity(mIntent);

            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(StudentAttendanceActivity.this, R.string.error_nfc_read_failed, Toast.LENGTH_SHORT).show();
            }
            mNfcAdapter.disableForegroundDispatch(StudentAttendanceActivity.this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // NFCタグ読み取り時にonPause()が実行されるためonStop()に移動
        stopUpdateLocation();

        mHandler.removeCallbacks(autoSaveTask);
        isAutoSaveRunning = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE: {
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = data.getStringExtra(FileChooseActivity.FILE_NAME);
                    String filePath = data.getStringExtra(FileChooseActivity.FILE_PATH);
                    try {
                        mAttendanceSheet = new AttendanceSheet(new File(filePath), CHARACTER_CODE, getResources());
                        mAttendanceListAdapter = new AttendanceListAdapter(StudentAttendanceActivity.this, 0, mAttendanceSheet.getAttendanceList());
                        attendanceListView.setAdapter(mAttendanceListAdapter);
                        textViewForSubject.setText(mAttendanceSheet.getSubject());
                        textViewForTime.setText(mAttendanceSheet.getTime());
                        textViewForCount.setText(String.valueOf(mAttendanceSheet.getNumOfConfirmedStudents()));
                        isReading = false;
                        isSaved = false;
                        Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        Toast.makeText(StudentAttendanceActivity.this, R.string.error_unsupported_list_file, Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }

                break;
            }
            case StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE: {
                if (resultCode == Activity.RESULT_OK) {
                    destFile = new File(data.getStringExtra(FileChooseActivity.FILE_PATH));
                    saveCsvFileWithConfirmation(destFile, StudentAttendanceActivity.CHARACTER_CODE);
                }

                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_attendance_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open_register: {
                showDialog(StudentAttendanceActivity.DIALOG_ASK_OPEN_REGISTER);

                break;
            }
            case R.id.menu_setting: {
                Intent mIntent = new Intent(StudentAttendanceActivity.this, SettingActivity.class);
                startActivity(mIntent);

                break;
            }
            case R.id.menu_add_attendance: {
                showDialog(StudentAttendanceActivity.DIALOG_ADD_ATTENDANCE_MENU);

                break;
            }
            case R.id.menu_refresh: {
                refreshStudentMaster();

                break;
            }
            case R.id.menu_open: {
                if (!isSaved) {
                    showDialog(StudentAttendanceActivity.DIALOG_ASK_OPEN_WITHOUT_SAVING);
                }
                else {
                    Intent mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
                    mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, listDir.getAbsolutePath());
                    mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                    mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                    startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE);
                }

                break;
            }
            case R.id.menu_save: {
                destFile = new File(saveDir, makeFileName());
                saveCsvFileWithConfirmation(destFile, StudentAttendanceActivity.CHARACTER_CODE);

                break;
            }
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        switch (id) {
            case StudentAttendanceActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_exit_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StudentAttendanceActivity.super.onBackPressed();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_OPEN_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_remove_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
                        mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, listDir.getAbsolutePath());
                        mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                        mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                        startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_add_student_by_student_no);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 存在しない場合は他のリストを検索する
                        Student mStudent = master.getStudentByStudentNo(readStudentNo);
                        if (mStudent != null) {
                            currentAttendance = new Attendance(mStudent, getResources());
                        }
                        else {
                            // 他のリストにも存在しない場合は学籍番号のみで追加する
                            currentAttendance = new Attendance(new Student(readStudentNo), getResources());
                        }

                        updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                        addAttendance(currentAttendance);
                        int position = mAttendanceListAdapter.getCount() - 1;
                        attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                        attendanceListView.setSelection(position);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_EDIT_SUBJECT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_edit_subject_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                View mView = inflater.inflate(R.layout.dialog_edit_subject, null);
                editTextForSubject = (EditText)mView.findViewById(R.id.dialog_subject);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String subject = editTextForSubject.getEditableText().toString();
                        mAttendanceSheet.setSubject(subject);
                        textViewForSubject.setText(subject);
                        isSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_EDIT_TIME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_edit_time_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                View mView = inflater.inflate(R.layout.dialog_edit_time, null);
                editTextForTime = (EditText)mView.findViewById(R.id.dialog_time);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String time = editTextForTime.getEditableText().toString();
                        mAttendanceSheet.setTime(time);
                        textViewForTime.setText(time);
                        isSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_TOTALIZATION: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_totalization_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                View mView = inflater.inflate(R.layout.dialog_totalization, null);
                tableLayoutForTotalization = (TableLayout)mView.findViewById(R.id.dialog_totalization_layout);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_attendance_menu_title);
                builder.setItems(R.array.dialog_attendance_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateStatus(currentAttendance, which);
                        attendanceListView.invalidateViews();
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_ADD_ATTENDANCE_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_add_attendance_menu_title);
                builder.setItems(R.array.dialog_add_attendance_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // リストから追加する
                                if (master.size() != 0) {
                                    showDialog(StudentAttendanceActivity.DIALOG_CSV_FILE_LIST);
                                }
                                else {
                                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 1: {
                                // 学籍番号で検索する
                                if (master.size() != 0) {
                                    showDialog(StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO);
                                }
                                else {
                                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 2: {
                                // 手動で登録する
                                showDialog(StudentAttendanceActivity.DIALOG_INPUT_STUDENT_INFO);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_CSV_FILE_LIST: {
                selectedSheet = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_csv_file_list_title);
                String[] classNames = master.getClassNames();
                builder.setItems(classNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedSheet = master.getStudentSheetByIndex(which);
                        showDialog(StudentAttendanceActivity.DIALOG_STUDENT_LIST);
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentAttendanceActivity.DIALOG_STUDENT_LIST: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(selectedSheet.getClassName());
                final ArrayList<Student> students = selectedSheet.getStudentList();
                String[] labels = new String[students.size()];
                for (int i = 0; i < students.size(); i++) {
                    Student mStudent = students.get(i);
                    labels[i] = mStudent.getStudentNo() + " " + mStudent.getStudentName();
                }
                builder.setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Student mStudent = students.get(which);
                        int position;
                        if (mAttendanceSheet.hasStudentNo(mStudent.getStudentNo())) {
                            // 学籍番号に対応するデータが存在する場合はその行を選択する
                            currentAttendance = mAttendanceSheet.getByStudentNo(mStudent.getStudentNo());
                            if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                                updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                            }
                            else {
                                Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_already_read, Toast.LENGTH_SHORT).show();
                            }
                            position = mAttendanceListAdapter.getPosition(currentAttendance);
                        }
                        else {
                            currentAttendance = new Attendance(mStudent, getResources());
                            updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                            addAttendance(currentAttendance);
                            position = mAttendanceListAdapter.getCount() - 1;
                        }
                        attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                        attendanceListView.setSelection(position);
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                editTextForStudentNoForSearch = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNoForSearch);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo = editTextForStudentNoForSearch.getText().toString().toUpperCase();
                        if (studentNo.length() != 0) {
                            boolean prevReading = isReading;
                            if (!isReading) {
                                isReading = true;
                            }
                            onStudentNoRead(studentNo);
                            isReading = prevReading;
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_INPUT_STUDENT_INFO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_input_student_info_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                View mView = inflater.inflate(R.layout.dialog_input_student_info, null);
                editTextForStudentNoForManual = (EditText)mView.findViewById(R.id.dialog_student_no);
                editTextForClassName          = (EditText)mView.findViewById(R.id.dialog_class_name);
                editTextForStudentName        = (EditText)mView.findViewById(R.id.dialog_student_name);
                editTextForStudentRuby        = (EditText)mView.findViewById(R.id.dialog_student_ruby);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo   = editTextForStudentNoForManual.getEditableText().toString();
                        String className   = editTextForClassName.getEditableText().toString();
                        String studentName = editTextForStudentName.getEditableText().toString();
                        String studentRuby = editTextForStudentRuby.getEditableText().toString();
                        int position;
                        if (mAttendanceSheet.hasStudentNo(studentNo)) {
                            // 学籍番号に対応するデータが存在する場合はその行を選択する
                            currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                            if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                                updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                            }
                            else {
                                Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_already_read, Toast.LENGTH_SHORT).show();
                            }
                            position = mAttendanceListAdapter.getPosition(currentAttendance);
                        }
                        else {
                            currentAttendance = new Attendance(new Student(studentNo, className, studentName,
                                                                           studentRuby, (String[])null), -1, getResources());
                            updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                            addAttendance(currentAttendance);
                            position = mAttendanceListAdapter.getCount() - 1;
                        }
                        attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                        attendanceListView.setSelection(position);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_REGISTER_READ_ID: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_register_read_id);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDialog(StudentAttendanceActivity.DIALOG_REGISTER_ID_MENU);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_REGISTER_ID_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_register_id_menu_title);
                builder.setItems(R.array.dialog_register_id_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // リストから追加する
                                if (master.size() != 0) {
                                    showDialog(StudentAttendanceActivity.DIALOG_CSV_FILE_LIST_R);
                                }
                                else {
                                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 1: {
                                // 学籍番号で検索する
                                if (master.size() != 0) {
                                    showDialog(StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO_R);
                                }
                                else {
                                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 2: {
                                // バーコードを読み取る
                                isRegistering = true;
                                showDialog(StudentAttendanceActivity.DIALOG_READING_BARCODE);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_CSV_FILE_LIST_R: {
                selectedSheet = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_csv_file_list_title);
                String[] classNames = master.getClassNames();
                builder.setItems(classNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedSheet = master.getStudentSheetByIndex(which);
                        showDialog(StudentAttendanceActivity.DIALOG_STUDENT_LIST_R);
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentAttendanceActivity.DIALOG_STUDENT_LIST_R: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(selectedSheet.getClassName());
                final ArrayList<Student> students = selectedSheet.getStudentList();
                final String[] studentNos = new String[students.size()];
                String[] labels = new String[students.size()];
                for (int i = 0; i < students.size(); i++) {
                    Student mStudent = students.get(i);
                    studentNos[i] = mStudent.getStudentNo();
                    labels[i] = mStudent.getStudentNo() + " " + mStudent.getStudentName();
                }
                builder.setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (readNfcId != null) {
                            addNfcId(studentNos[which], readNfcId);
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO_R: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(StudentAttendanceActivity.this);
                editTextForStudentNoForRegister = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNoForRegister);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (readNfcId != null) {
                            String studentNo = editTextForStudentNoForRegister.getText().toString().toUpperCase();
                            addNfcId(studentNo, readNfcId);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_READING_BARCODE: {
                ProgressDialog mProgressDialog = new ProgressDialog(StudentAttendanceActivity.this);
                mProgressDialog.setMessage(getString(R.string.dialog_reading_barcode));
                mProgressDialog.setCancelable(true);
                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        isRegistering = false;
                    }
                });
                mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        boolean retBool = false;

                        if (event.isPrintingKey() && event.getAction() == KeyEvent.ACTION_DOWN) {
                            retBool = onKeyDown(keyCode, event);
                        }

                        return retBool;
                    }
                });
                retDialog = mProgressDialog;

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveCsvFileWithOverwrite(destFile, StudentAttendanceActivity.CHARACTER_CODE);
                    }
                });
                builder.setNeutralButton(R.string.dialog_save_as, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
                        String initDirPath;
                        File parent = destFile.getParentFile();
                        if (parent != null) {
                            initDirPath = parent.getAbsolutePath();
                        }
                        else {
                            initDirPath = "/";
                        }
                        mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, initDirPath);
                        mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                        mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                        startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_FETCHING_LOCATION: {
                ProgressDialog mProgressDialog = new ProgressDialog(StudentAttendanceActivity.this);
                mProgressDialog.setMessage(getString(R.string.dialog_fetching_location));
                mProgressDialog.setCancelable(true);
                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mPreferenceUtil.putLocationEnabled(false);
                        stopUpdateLocation();
                        Toast.makeText(StudentAttendanceActivity.this, R.string.notice_add_location_disabled, Toast.LENGTH_SHORT).show();
                    }
                });
                retDialog = mProgressDialog;

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_OPEN_REGISTER: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_open_register);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(StudentAttendanceActivity.this, StudentRegisterActivity.class);
                        startActivity(mIntent);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_OPEN_GPS_PREFERENCE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_open_gps_preference);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(mIntent);
                    }
                });
                builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mPreferenceUtil.putLocationEnabled(false);
                        stopUpdateLocation();
                        Toast.makeText(StudentAttendanceActivity.this, R.string.notice_add_location_disabled, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentAttendanceActivity.DIALOG_REFRESHING_MASTER_FILE: {
                progressDialogForRefresh = new ProgressDialog(StudentAttendanceActivity.this);
                progressDialogForRefresh.setTitle(getString(R.string.dialog_refreshing_master_file_title));
                progressDialogForRefresh.setMessage("");
                if (prevMax != -1) {
                    progressDialogForRefresh.setMax(prevMax);
                }
                progressDialogForRefresh.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialogForRefresh.setCancelable(false);
                retDialog = progressDialogForRefresh;

                break;
            }
        }

        return(retDialog);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog mAlertDialog = null;
        if (dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch (id) {
            case StudentAttendanceActivity.DIALOG_EDIT_SUBJECT: {
                editTextForSubject.setText(mAttendanceSheet.getSubject());

                break;
            }
            case StudentAttendanceActivity.DIALOG_EDIT_TIME: {
                editTextForTime.setText(mAttendanceSheet.getTime());

                break;
            }
            case StudentAttendanceActivity.DIALOG_TOTALIZATION: {
                ArrayList<String> classNames = mAttendanceSheet.getClassNames();
                Collections.sort(classNames);

                int wholeNumOfStudents   = 0;
                int wholeNumOfAttendance = 0;
                int wholeNumOfLateness   = 0;
                int wholeNumOfLeaveEarly = 0;
                int wholeNumOfAbsence    = 0;
                for (String className : classNames) {
                    StudentCounter mCounter = mAttendanceSheet.getStudentCounter(className);
                    int numOfAbsence = mCounter.getNumOfStudents() - (mCounter.getNumOfAttendance() + mCounter.getNumOfLateness() + mCounter.getNumOfLeaveEarly());
                    wholeNumOfStudents   += mCounter.getNumOfStudents();
                    wholeNumOfAttendance += mCounter.getNumOfAttendance();
                    wholeNumOfLateness   += mCounter.getNumOfLateness();
                    wholeNumOfLeaveEarly += mCounter.getNumOfLeaveEarly();
                    wholeNumOfAbsence    += numOfAbsence;

                    TableRow mTableRow = new TableRow(StudentAttendanceActivity.this);

                    TextView textViewForClassName = new TextView(StudentAttendanceActivity.this);
                    textViewForClassName.setGravity(Gravity.CENTER);
                    textViewForClassName.setText(className);
                    mTableRow.addView(textViewForClassName);

                    TextView textViewForNumOfStudents = new TextView(StudentAttendanceActivity.this);
                    textViewForNumOfStudents.setGravity(Gravity.RIGHT);
                    textViewForNumOfStudents.setText(String.valueOf(mCounter.getNumOfStudents()));
                    mTableRow.addView(textViewForNumOfStudents);

                    TextView textViewForNumOfAttendance = new TextView(StudentAttendanceActivity.this);
                    textViewForNumOfAttendance.setGravity(Gravity.RIGHT);
                    textViewForNumOfAttendance.setText(String.valueOf(mCounter.getNumOfAttendance()));
                    mTableRow.addView(textViewForNumOfAttendance);

                    TextView textViewForNumOfLateness = new TextView(StudentAttendanceActivity.this);
                    textViewForNumOfLateness.setGravity(Gravity.RIGHT);
                    textViewForNumOfLateness.setText(String.valueOf(mCounter.getNumOfLateness()));
                    mTableRow.addView(textViewForNumOfLateness);

                    TextView textViewForNumOfLeaveEarly = new TextView(StudentAttendanceActivity.this);
                    textViewForNumOfLeaveEarly.setGravity(Gravity.RIGHT);
                    textViewForNumOfLeaveEarly.setText(String.valueOf(mCounter.getNumOfLeaveEarly()));
                    mTableRow.addView(textViewForNumOfLeaveEarly);

                    TextView textViewForNumOfAbsence = new TextView(StudentAttendanceActivity.this);
                    textViewForNumOfAbsence.setGravity(Gravity.RIGHT);
                    textViewForNumOfAbsence.setText(String.valueOf(numOfAbsence));
                    mTableRow.addView(textViewForNumOfAbsence);

                    tableLayoutForTotalization.addView(mTableRow);
                }

                TextView tvForWholeNumOfStudents = (TextView)tableLayoutForTotalization.findViewById(R.id.whole_num_of_students);
                tvForWholeNumOfStudents.setText(String.valueOf(wholeNumOfStudents));
                TextView tvForWholeNumOfAttendance = (TextView)tableLayoutForTotalization.findViewById(R.id.whole_num_of_attendance);
                tvForWholeNumOfAttendance.setText(String.valueOf(wholeNumOfAttendance));
                TextView tvForWholeNumOfLateness = (TextView)tableLayoutForTotalization.findViewById(R.id.whole_num_of_lateness);
                tvForWholeNumOfLateness.setText(String.valueOf(wholeNumOfLateness));
                TextView tvForWholeNumOfLeaveEarly = (TextView)tableLayoutForTotalization.findViewById(R.id.whole_num_of_leave_early);
                tvForWholeNumOfLeaveEarly.setText(String.valueOf(wholeNumOfLeaveEarly));
                TextView tvForWholeNumOfAbsence = (TextView)tableLayoutForTotalization.findViewById(R.id.whole_num_of_absence);
                tvForWholeNumOfAbsence.setText(String.valueOf(wholeNumOfAbsence));

                break;
            }
            case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU: {
                mAlertDialog.setTitle(currentAttendance.getStudentNo() + " " + currentAttendance.getStudentName());

                break;
            }
            case StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO: {
                editTextForStudentNoForSearch.setText("");

                break;
            }
            case StudentAttendanceActivity.DIALOG_INPUT_STUDENT_INFO: {
                editTextForStudentNoForManual.setText("");
                editTextForClassName.setText("");
                editTextForStudentName.setText("");
                editTextForStudentRuby.setText("");

                break;
            }
            case StudentAttendanceActivity.DIALOG_SEARCH_STUDENT_NO_R: {
                editTextForStudentNoForRegister.setText("");

                break;
            }
            case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE: {
                mAlertDialog.setMessage(destFile.getName() + getString(R.string.dialog_ask_overwrite));

                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isSaved) {
            showDialog(StudentAttendanceActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean retBool;

        if (event.isPrintingKey()) {
            onCharTyped((char)event.getUnicodeChar());

            retBool = true;
        }
        else {
            retBool = super.onKeyDown(keyCode, event);
        }

        return retBool;
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagRead(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("IsReading", isReading);
        outState.putBoolean("IsRegistering", isRegistering);
        outState.putBoolean("IsSaved", isSaved);
        outState.putBoolean("IsSaving", isSaving);
        outState.putBoolean("IsFetchingLocation", isFetchingLocation);
        outState.putBoolean("IsRefreshing", isRefreshing);
        outState.putSerializable("DestFile", destFile);
        outState.putString("ReadStudentNo", readStudentNo);
        outState.putString("ReadNfcId", readNfcId);
        outState.putInt("AttendanceKind", attendanceKindSpinner.getSelectedItemPosition());
        outState.putSerializable("CurrentAttendance", currentAttendance);
        outState.putSerializable("SelectedSheet", selectedSheet);
        outState.putSerializable("AttendanceSheet", mAttendanceSheet);
        outState.putSerializable("AttendanceLocation", mAttendanceLocation);
        outState.putInt("PrevMax", prevMax);
    }

    /**
     * 保存ファイル名を生成する
     * @return 生成されたファイル名
     */
    private String makeFileName() {
        // ファイル名を生成
        StringBuilder rawFileName = new StringBuilder(mPreferenceUtil.getAttendanceName(PreferenceUtil.DEFAULT_ATTENDANCE_NAME) + ".csv");
        // 科目名と授業時間を置換
        int subjectPos;
        while ((subjectPos = rawFileName.indexOf("%S")) != -1) {
            rawFileName.replace(subjectPos, subjectPos + 2, mAttendanceSheet.getSubject());
        }
        int timePos;
        while ((timePos = rawFileName.indexOf("%t")) != -1) {
            rawFileName.replace(timePos, timePos + 2, mAttendanceSheet.getTime());
        }

        // 年月日時分秒を置換
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = format.format(new Date());
        int yearPos;
        while ((yearPos = rawFileName.indexOf("%y")) != -1) {
            rawFileName.replace(yearPos, yearPos + 2, dateString.substring(0, 4));
        }
        int monthPos;
        while ((monthPos = rawFileName.indexOf("%M")) != -1) {
            rawFileName.replace(monthPos, monthPos + 2, dateString.substring(4, 6));
        }
        int dayPos;
        while ((dayPos = rawFileName.indexOf("%d")) != -1) {
            rawFileName.replace(dayPos, dayPos + 2, dateString.substring(6, 8));
        }
        int hourPos;
        while ((hourPos = rawFileName.indexOf("%h")) != -1) {
            rawFileName.replace(hourPos, hourPos + 2, dateString.substring(8, 10));
        }
        int minutePos;
        while ((minutePos = rawFileName.indexOf("%m")) != -1) {
            rawFileName.replace(minutePos, minutePos + 2, dateString.substring(10, 12));
        }
        int secondPos;
        while ((secondPos = rawFileName.indexOf("%s")) != -1) {
            rawFileName.replace(secondPos, secondPos + 2, dateString.substring(12, 14));
        }

        return rawFileName.toString();
    }

    /**
     * 出席データをCSV形式で保存する<br>
     * 同名のファイルが存在する場合は確認のダイアログを表示する。
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     */
    private void saveCsvFileWithConfirmation(File csvFile, String encode) {
        if (csvFile.exists()) {
            showDialog(StudentAttendanceActivity.DIALOG_ASK_OVERWRITE);
        }
        else {
            saveCsvFileWithOverwrite(csvFile, encode);
        }
    }

    /**
     * 出席データをCSV形式で保存する<br>
     * 同名のファイルが存在する場合は上書き保存する。
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     */
    private void saveCsvFileWithOverwrite(File csvFile, String encode) {
        if (mAttendanceSheet.size() != 0) {
            try {
                isSaving = true;
                if (!mPreferenceUtil.isLocationEnabled()) {
                    mAttendanceSheet.saveCsvFile(csvFile, encode);
                }
                else {
                    mAttendanceSheet.saveCsvFile(csvFile, encode, mPreferenceUtil.isLatitudeEnabled(), mPreferenceUtil.isLongitudeEnabled(),
                                                 mPreferenceUtil.isAccuracyEnabled());
                }
                isSaved = true;
                Toast.makeText(StudentAttendanceActivity.this, csvFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                Toast.makeText(StudentAttendanceActivity.this, csvFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                Log.e("saveCsvFileWithOverwrite", e.getMessage(), e);
            }
            finally {
                isSaving = false;
            }
        }
        else {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_saving_data_null, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    private void onCharTyped(char c) {
        if (Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
        }
        inputBuffer.append(c);
        if (inputBuffer.length() == 6) {
            onStudentNoRead(inputBuffer.toString());
        }
    }

    /**
     * 学籍番号を読み取った際に呼び出される
     * @param studentNo 学籍番号
     */
    private void onStudentNoRead(String studentNo) {
        if (!isRegistering) {
            // 通常時
            if (isReading && !isSaving && !isRefreshing) {
                if (mAttendanceSheet.hasStudentNo(studentNo)) {
                    // 学籍番号に対応するデータが存在する場合はその行を選択する
                    currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                    if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                        updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                    }
                    else {
                        Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_already_read, Toast.LENGTH_SHORT).show();
                    }
                    int position = mAttendanceListAdapter.getPosition(currentAttendance);
                    attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                    attendanceListView.setSelection(position);
                }
                else {
                    if (mPreferenceUtil.getBehaviorStudentNo(PreferenceUtil.BEHAVIOR_DIALOG) == PreferenceUtil.BEHAVIOR_DIALOG) {
                        readStudentNo = studentNo;
                        showDialog(StudentAttendanceActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO);
                    }
                    else {
                        Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        else {
            // NFCタグ登録時
            addNfcId(studentNo, readNfcId);
            isRegistering = false;
            try {
                dismissDialog(StudentAttendanceActivity.DIALOG_READING_BARCODE);
            }
            catch (IllegalArgumentException e) {}
        }
    }

    /**
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    private void onNfcTagRead(Intent inIntent) {
        if (isReading && !isSaving && !isRefreshing) {
            String id = Util.byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            Student mStudent = master.getStudentByNfcId(id);
            if (mStudent != null) {
                int position;
                if (mAttendanceSheet.hasStudentNo(mStudent.getStudentNo())) {
                    // 学籍番号に対応するデータが存在する場合はその行を選択する
                    currentAttendance = mAttendanceSheet.getByStudentNo(mStudent.getStudentNo());
                    if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                        updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                    }
                    else {
                        Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_already_read, Toast.LENGTH_SHORT).show();
                    }
                    position = mAttendanceListAdapter.getPosition(currentAttendance);
                }
                else {
                    currentAttendance = new Attendance(mStudent, getResources());
                    updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                    addAttendance(currentAttendance);
                    position = mAttendanceListAdapter.getCount() - 1;
                }
                attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                attendanceListView.setSelection(position);
            }
            else {
                if (mPreferenceUtil.getBehaviorNfcId(PreferenceUtil.BEHAVIOR_DIALOG) == PreferenceUtil.BEHAVIOR_DIALOG) {
                    readNfcId = id;
                    showDialog(StudentAttendanceActivity.DIALOG_ASK_REGISTER_READ_ID);
                }
                else {
                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 出席データを追加する
     * @param id NFCタグのID
     * @param inAttendance 出席データ
     */
    private void addAttendance(Attendance inAttendance) {
        inAttendance.setAttendanceNo(mAttendanceSheet.size() + 1);
        mAttendanceSheet.add(inAttendance);
        mAttendanceListAdapter.add(inAttendance);
        textViewForCount.setText(String.valueOf(mAttendanceSheet.getNumOfConfirmedStudents()));
    }

    /**
     * 出席種別を更新する
     * @param inAttendance 更新する出席データ
     * @param status 出席種別
     */
    private void updateStatus(Attendance inAttendance, int status) {
        inAttendance.setStatus(status, mAttendanceLocation);
        isSaved = false;
        textViewForCount.setText(String.valueOf(mAttendanceSheet.getNumOfConfirmedStudents()));
    }

    /**
     * 学生データにNFCタグを追加して出席データを追加する
     * @param studentNo 学籍番号
     * @param id NFCタグ
     */
    private void addNfcId(String studentNo, String id) {
        try {
            Student mStudent = master.addNfcId(studentNo, id);
            if (mStudent != null) {
                Toast.makeText(StudentAttendanceActivity.this, R.string.notice_id_registered, Toast.LENGTH_SHORT).show();

                int position;
                if (mAttendanceSheet.hasStudentNo(studentNo)) {
                    // 学籍番号に対応するデータが存在する場合はその行を選択する
                    currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                    if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                        updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                    }
                    else {
                        Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_already_read, Toast.LENGTH_SHORT).show();
                    }
                    position = mAttendanceListAdapter.getPosition(currentAttendance);
                }
                else {
                    currentAttendance = new Attendance(mStudent, getResources());
                    updateStatus(currentAttendance, attendanceKindSpinner.getSelectedItemPosition());
                    addAttendance(currentAttendance);
                    position = mAttendanceListAdapter.getCount() - 1;
                }
                attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                attendanceListView.setSelection(position);

                refreshStudentMaster();
            }
            else {
                Toast.makeText(StudentAttendanceActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
            }
        }
        catch (IOException e) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_id_register_failed, Toast.LENGTH_SHORT).show();
            Log.e("registerNfcId", e.getMessage(), e);
        }
    }

    /** 学生マスタを読み込み直す */
    private void refreshStudentMaster() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    master.refresh();
                    application.setStudentMaster(master);
                }
                catch (UnsupportedEncodingException e) {
                    Toast.makeText(StudentAttendanceActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();
                    Log.e("refreshStudentMaster", e.getMessage(), e);

                    finish();
                }
            }
        }).start();
    }

    /** 位置情報の取得を開始する */
    private void startUpdateLocation() {
        if (mAttendanceLocation == null) {
            showDialog(StudentAttendanceActivity.DIALOG_FETCHING_LOCATION);
        }
        if (!isFetchingLocation) {
            isFetchingLocation = true;
            if (mPreferenceUtil.getLocationProvider(PreferenceUtil.LOCATION_PROVIDER_NETWORK) == PreferenceUtil.LOCATION_PROVIDER_GPS) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                                        mPreferenceUtil.getLocationInterval(PreferenceUtil.DEFAULT_LOCATION_INTERVAL) * 60000,
                                                        0, mLocationListener);
            }
            else {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                                        mPreferenceUtil.getLocationInterval(PreferenceUtil.DEFAULT_LOCATION_INTERVAL) * 60000,
                                                        0, mLocationListener);
            }
        }
    }

    /** 位置情報の取得を停止する */
    private void stopUpdateLocation() {
        isFetchingLocation = false;
        mLocationManager.removeUpdates(mLocationListener);
    }
}