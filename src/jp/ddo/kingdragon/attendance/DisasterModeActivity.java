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
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
import jp.ddo.kingdragon.attendance.servlet.SearchStudentServlet;
import jp.ddo.kingdragon.attendance.servlet.ShowMovieServlet;
import jp.ddo.kingdragon.attendance.servlet.StudentListServlet;
import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceListAdapter;
import jp.ddo.kingdragon.attendance.student.AttendanceLocation;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;
import jp.ddo.kingdragon.attendance.student.Student;
import jp.ddo.kingdragon.attendance.student.StudentSheet;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * 災害モード
 * @author 杉本祐介
 */
public class DisasterModeActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CAPTURE_PHOTO    = 0;
    private static final int REQUEST_CAPTURE_MOVIE    = 1;
    private static final int REQUEST_CHOOSE_OPEN_FILE = 2;
    // ダイアログのID
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING = 0;
    private static final int DIALOG_DISASTER_MENU           = 1;
    private static final int DIALOG_ADD_ATTENDANCE_MENU     = 2;
    private static final int DIALOG_CSV_FILE_LIST           = 3;
    private static final int DIALOG_STUDENT_LIST            = 4;
    private static final int DIALOG_SEARCH_STUDENT_NO       = 5;
    private static final int DIALOG_INPUT_STUDENT_INFO      = 6;
    private static final int DIALOG_ASK_REGISTER_READ_ID    = 7;
    private static final int DIALOG_REGISTER_ID_MENU        = 8;
    private static final int DIALOG_CSV_FILE_LIST_R         = 9;
    private static final int DIALOG_STUDENT_LIST_R          = 10;
    private static final int DIALOG_SEARCH_STUDENT_NO_R     = 11;
    private static final int DIALOG_READING_BARCODE         = 12;
    private static final int DIALOG_ASK_OVERWRITE           = 13;
    private static final int DIALOG_FETCHING_LOCATION       = 14;
    private static final int DIALOG_ASK_OPEN_LIST_MAKER     = 15;
    private static final int DIALOG_ASK_OPEN_GPS_PREFERENCE = 16;
    /**
     * CSVファイルへの保存に使用する文字コード
     */
    private static final String CHARACTER_CODE = "Shift_JIS";
    /**
     * サーバへの送信に使用する文字コード
     */
    private static final String CHARACTER_CODE_FOR_SEND = "Shift_JIS";
    /**
     * ポート番号
     */
    private static final int PORT_NUMBER = 8080;

    // 変数の宣言
    /**
     * 現在編集しているシート
     */
    private static AttendanceSheet mAttendanceSheet;
    /**
     * サーブレットに渡すコンテキスト<br />
     * アプリケーションコンテキストを格納すること。
     */
    private static Context applicationContextForServlet;

    /**
     * 読み取り中かどうか
     */
    private boolean isReading;
    /**
     * 送信中かどうか
     */
    private boolean isSending;
    /**
     * NFCタグを登録中かどうか
     */
    private boolean isRegistering;
    /**
     * 保存済みかどうか
     */
    private boolean isSaved;
    /**
     * 現在地を取得中かどうか
     */
    private boolean isFetchingLocation;

    /**
     * ベースフォルダ
     */
    private File baseDir;
    /**
     * リスト格納用フォルダ
     */
    private File listDir;
    /**
     * 保存用フォルダ
     */
    private File saveDir;
    /**
     * 保存先のファイル
     */
    private File destFile;
    /**
     * キーボード(バーコードリーダ)から入力された内容
     */
    private StringBuilder inputBuffer;
    /**
     * 追加待ちのNFCタグ
     */
    private String readNfcId;
    /**
     * 現在扱っている出席データ
     */
    private Attendance currentAttendance;
    /**
     * リストから追加する際に選択されたシート
     */
    private StudentSheet selectedSheet;
    /**
     * 出席データを送信するキュー
     */
    private SendAttendanceQueue attendanceQueue;
    /**
     * 出席データの一覧を表示するビュー
     */
    private ListView attendanceListView;
    /**
     * 出席データの一覧を表示するアダプタ
     */
    private AttendanceListAdapter mAttendanceListAdapter;
    /**
     * 読み取り開始ボタン
     */
    private Button readStartButton;
    /**
     * 送信停止ボタン
     */
    private Button sendPauseButton;
    /**
     * 学籍番号用のEditText
     */
    private EditText editTextForStudentNo;
    /**
     * 所属用のEditText
     */
    private EditText editTextForClassName;
    /**
     * 氏名用のEditText
     */
    private EditText editTextForStudentName;
    /**
     * カナ用のEditText
     */
    private EditText editTextForStudentRuby;

    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    /**
     * サーバのインスタンス
     */
    private Server mServer;

    /**
     * NFCタグの読み取りに使用
     */
    private NfcAdapter mNfcAdapter;
    /**
     * NFCタグの読み取りに使用
     */
    private PendingIntent mPendingIntent;

    /**
     * スリープ時にWi-Fiを維持するために使用
     */
    private WakeLock mWakeLock;
    /**
     * スリープ時にWi-Fiを維持するために使用
     */
    private WifiLock mWifiLock;

    /**
     * 位置情報を取得するマネージャ
     */
    private LocationManager mLocationManager;
    /**
     * 位置情報を取得した際のリスナ
     */
    private LocationListener mLocationListener;
    /**
     * 取得した位置情報
     */
    private AttendanceLocation mAttendanceLocation;

    // 配列の宣言
    /**
     * 対応するインテントの種類
     */
    private IntentFilter[] filters;
    /**
     * 対応させるタグの一覧
     */
    private String[][] techs;

    // コレクションの宣言
    /**
     * リストディレクトリから読み取った全てのシートを格納するリスト
     */
    private ArrayList<StudentSheet> studentSheets;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disaster_mode);

        mAttendanceSheet = new AttendanceSheet();
        applicationContextForServlet = getApplicationContext();

        isReading = false;
        isSending = true;
        isRegistering = false;
        isSaved = true;
        isFetchingLocation = false;

        inputBuffer = new StringBuilder();
        readNfcId = null;
        currentAttendance = null;
        selectedSheet = null;
        mAttendanceLocation = null;
        mPreferenceUtil = new PreferenceUtil(DisasterModeActivity.this);

        // アクティビティ再生成前のデータがあれば復元する
        if (savedInstanceState != null) {
            isReading = savedInstanceState.getBoolean("IsReading");
            isSending = savedInstanceState.getBoolean("IsSending");
            isRegistering = savedInstanceState.getBoolean("IsRegistering");
            isSaved = savedInstanceState.getBoolean("IsSaved");
            isFetchingLocation = savedInstanceState.getBoolean("IsFetchingLocation");
            readNfcId = savedInstanceState.getString("ReadNfcId");
            attendanceQueue = (SendAttendanceQueue)savedInstanceState.getSerializable("AttendanceQueue");
            currentAttendance = (Attendance)savedInstanceState.getSerializable("CurrentAttendance");
            selectedSheet = (StudentSheet)savedInstanceState.getSerializable("SelectedSheet");
            mAttendanceSheet = (AttendanceSheet)savedInstanceState.getSerializable("AttendanceSheet");
            mAttendanceListAdapter = new AttendanceListAdapter(DisasterModeActivity.this, 0, mAttendanceSheet.getAttendanceList());
            mAttendanceLocation = (AttendanceLocation)savedInstanceState.getSerializable("AttendanceLocation");
        }
        else {
            attendanceQueue = new SendAttendanceQueue(mPreferenceUtil.getServerAddress(PreferenceUtil.DEFAULT_SERVER_ADDRESS),
                                                      DisasterModeActivity.CHARACTER_CODE_FOR_SEND, 100);
            mAttendanceListAdapter = new AttendanceListAdapter(DisasterModeActivity.this, 0);
        }

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
                showDialog(DisasterModeActivity.DIALOG_DISASTER_MENU);

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

        sendPauseButton = (Button)findViewById(R.id.send_pause);
        sendPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSending) {
                    sendPauseButton.setText(R.string.disaster_send_resume_label);
                    attendanceQueue.pause();
                    isSending = false;
                }
                else {
                    sendPauseButton.setText(R.string.disaster_send_pause_label);
                    attendanceQueue.resume();
                    isSending = true;
                }
            }
        });

        // 各フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        listDir = new File(baseDir, "StudentList");
        saveDir = new File(mPreferenceUtil.getAttendanceDir(baseDir.getAbsolutePath() + "/AttendanceData"));
        File webDir = new File(baseDir, "WebDoc");
        File servletDir = new File(baseDir, "Servlet");
        if (!listDir.exists() && !listDir.mkdirs()) {
            Toast.makeText(DisasterModeActivity.this, R.string.error_make_list_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            Toast.makeText(DisasterModeActivity.this, R.string.error_make_save_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        if (!webDir.exists() && !webDir.mkdirs()) {
            Toast.makeText(DisasterModeActivity.this, R.string.error_make_web_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        if (!servletDir.exists() && !servletDir.mkdirs()) {
            Toast.makeText(DisasterModeActivity.this, R.string.error_make_servlet_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(DisasterModeActivity.this);
        mPendingIntent = PendingIntent.getActivity(DisasterModeActivity.this, 0,
                                                   new Intent(DisasterModeActivity.this, getClass())
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
                        dismissDialog(DisasterModeActivity.DIALOG_FETCHING_LOCATION);
                    }
                    catch (IllegalArgumentException e) {}
                    mAttendanceLocation = new AttendanceLocation(location.getLatitude(), location.getLongitude(),
                                                                 location.getAccuracy());
                    Toast.makeText(DisasterModeActivity.this, R.string.notice_location_fetched, Toast.LENGTH_SHORT).show();
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

        /**
         * スリープ時にWi-Fiを維持する
         * 参考:android - How do I keep Wifi from disconnecting when phone is asleep? - Stack Overflow
         *      http://stackoverflow.com/questions/3871824/how-do-i-keep-wifi-from-disconnecting-when-phone-is-asleep
         */
        PowerManager mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        WifiManager mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");

        /**
         * サーバを起動
         * 参考:ループのスケッチブック >> AndroidアプリにJettyを組込む
         *      http://www.loopsketch.com/blog/2011/08/31/940/
         */
        mServer = new Server(DisasterModeActivity.PORT_NUMBER);
        HandlerList mHandlerList = new HandlerList();

        ResourceHandler mResourceHandler = new ResourceHandler();
        mResourceHandler.setResourceBase(webDir.getAbsolutePath());
        mHandlerList.addHandler(mResourceHandler);

        ServletContextHandler mServletContextHandler = new ServletContextHandler();
        mServletContextHandler.addServlet(SearchStudentServlet.class, "/SearchStudent");
        mServletContextHandler.addServlet(StudentListServlet.class, "/StudentList");
        mServletContextHandler.addServlet(ShowMovieServlet.class, "/ShowMovie");
//        File[] dexFiles = servletDir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String filename) {
//                boolean isDexFile = false;
//
//                if (filename.endsWith(".dex") || filename.endsWith(".jar")) {
//                    isDexFile = true;
//                }
//
//                return isDexFile;
//            }
//        });
//        // 最適化されたdexファイルの保存先にプライベートフォルダを指定する
//        File optimizedDir = getDir("Servlet", Context.MODE_PRIVATE);
//        DexClassLoader loader = new DexClassLoader(servletDir.getAbsolutePath(), optimizedDir.getAbsolutePath(),
//                                                   null, getClassLoader());
//        for (File dexFile : dexFiles) {
//            try {
//                String dexFileName = dexFile.getName();
//                String className = dexFileName.substring(0, dexFileName.length() - 4);
//                Class<?> loadedClass = loader.loadClass("hoge." + className);
//                mServletContextHandler.addServlet(loadedClass.asSubclass(Servlet.class), "/" + className);
//            }
//            catch (ClassNotFoundException e) {
//                Log.e("onCreate", e.getMessage(), e);
//            }
//            catch (ClassCastException e) {
//                Log.e("onCreate", e.getMessage(), e);
//            }
//        }
        mHandlerList.addHandler(mServletContextHandler);

        mServer.setHandler(mHandlerList);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mServer.start();
                }
                catch (Exception e) {
                    Log.e("onCreate", e.getMessage(), e);
                }
            }
        }).start();

        refreshStudentSheets();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPreferenceUtil.isDisasterModeEnabled(false)) {
            if (mNfcAdapter != null) {
                mNfcAdapter.enableForegroundDispatch(DisasterModeActivity.this, mPendingIntent, filters, techs);
            }

            if (mPreferenceUtil.isLocationEnabled(false)) {
                /**
                 * GPSが選択されていてGPSが無効になっている場合、設定画面を表示するか確認する
                 * 参考:[Android] GPSが有効か確認し、必要であればGPS設定画面を表示する。 | 株式会社ノベラック スタッフBlog
                 *      http://www.noveluck.co.jp/blog/archives/159
                 */
                if (mPreferenceUtil.getLocationProvider(PreferenceUtil.LOCATION_PROVIDER_NETWORK) == PreferenceUtil.LOCATION_PROVIDER_NETWORK
                    || mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    startUpdateLocation();
                }
                else {
                    stopUpdateLocation();
                    showDialog(DisasterModeActivity.DIALOG_ASK_OPEN_GPS_PREFERENCE);
                }
            }
            else {
                mAttendanceLocation = null;
                stopUpdateLocation();
            }

            if (studentSheets.size() != 0) {
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

            if (mPreferenceUtil.isSendServerEnabled(false)) {
                sendPauseButton.setEnabled(true);
                if (!isSending) {
                    sendPauseButton.setText(R.string.disaster_send_resume_label);
                }
                else {
                    sendPauseButton.setText(R.string.disaster_send_pause_label);
                }
            }
            else {
                sendPauseButton.setEnabled(false);
                sendPauseButton.setText(R.string.disaster_send_resume_label);
                isSending = false;
            }

            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
        }
        else {
            Intent mIntent = new Intent(DisasterModeActivity.this, StudentAttendanceActivity.class);
            startActivity(mIntent);

            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(DisasterModeActivity.this);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // NFCタグ読み取り時にonPause()が実行されるためonStop()に移動
        stopUpdateLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mServer != null) {
            try {
                mServer.stop();
            }
            catch (Exception e) {
                Log.e("onDestroy", e.getMessage(), e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DisasterModeActivity.REQUEST_CAPTURE_PHOTO: {
                if (resultCode == Activity.RESULT_OK) {
                    String photoPath = data.getStringExtra(CameraActivity.MEDIA_PATH);
                    if (photoPath != null) {
                        currentAttendance.putExtra(Attendance.PHOTO_PATH, photoPath);
                        isSaved = false;
                    }
                }

                break;
            }
            case DisasterModeActivity.REQUEST_CAPTURE_MOVIE: {
                if (resultCode == Activity.RESULT_OK) {
                    String moviePath = data.getStringExtra(CameraActivity.MEDIA_PATH);
                    if (moviePath != null) {
                        currentAttendance.putExtra(Attendance.MOVIE_PATH, moviePath);
                        isSaved = false;
                    }
                }

                break;
            }
            case DisasterModeActivity.REQUEST_CHOOSE_OPEN_FILE: {
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = data.getStringExtra(FileChooseActivity.FILE_NAME);
                    String filePath = data.getStringExtra(FileChooseActivity.FILE_PATH);
                    try {
                        AttendanceSheet tempAttendanceSheet = new AttendanceSheet(new File(filePath), DisasterModeActivity.CHARACTER_CODE,
                                                                                  getResources());
                        for (Attendance mAttendance : tempAttendanceSheet.getAttendanceList()) {
                            if (mAttendance.getStatus() != Attendance.ABSENCE) {
                                if (!mAttendanceSheet.hasStudentNo(mAttendance.getStudentNo())) {
                                    currentAttendance = mAttendance;
                                    addAttendance(currentAttendance.getStudentNo(), currentAttendance);
                                }
                            }
                        }
                        isSaved = false;
                        Toast.makeText(DisasterModeActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(DisasterModeActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }

                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.disaster_mode_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_make_list: {
                showDialog(DisasterModeActivity.DIALOG_ASK_OPEN_LIST_MAKER);

                break;
            }
            case R.id.menu_setting: {
                Intent mIntent = new Intent(DisasterModeActivity.this, SettingActivity.class);
                startActivity(mIntent);

                break;
            }
            case R.id.menu_add_attendance: {
                showDialog(DisasterModeActivity.DIALOG_ADD_ATTENDANCE_MENU);

                break;
            }
            case R.id.menu_refresh: {
                refreshStudentSheets();

                break;
            }
            case R.id.menu_open: {
                Intent mIntent = new Intent(DisasterModeActivity.this, FileChooseActivity.class);
                mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, saveDir.getAbsolutePath());
                mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                startActivityForResult(mIntent, DisasterModeActivity.REQUEST_CHOOSE_OPEN_FILE);

                break;
            }
            case R.id.menu_save: {
                if (mAttendanceSheet.size() != 0) {
                    // ファイル名を生成
                    StringBuilder rawFileName = new StringBuilder(mPreferenceUtil.getAttendanceName(PreferenceUtil.DEFAULT_ATTENDANCE_NAME) + ".csv");
                    // 科目名を置換
                    int subjectPos;
                    while ((subjectPos = rawFileName.indexOf("%S")) != -1) {
                        rawFileName.replace(subjectPos, subjectPos + 2, getString(R.string.disaster_title));
                    }
                    int timePos;
                    while ((timePos = rawFileName.indexOf("%t")) != -1) {
                        rawFileName.replace(timePos, timePos + 2, "");
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

                    String fileName = rawFileName.toString();
                    destFile = new File(saveDir, fileName);
                    if (destFile.exists()) {
                        showDialog(DisasterModeActivity.DIALOG_ASK_OVERWRITE);
                    }
                    else {
                        saveCsvFile(destFile, DisasterModeActivity.CHARACTER_CODE);
                    }
                }
                else {
                    Toast.makeText(DisasterModeActivity.this, R.string.error_saving_data_null, Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        switch (id) {
            case DisasterModeActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_exit_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DisasterModeActivity.super.onBackPressed();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_DISASTER_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_attendance_menu_title);
                builder.setItems(R.array.dialog_disaster_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // 写真を撮影
                                Intent mIntent = new Intent(DisasterModeActivity.this, CameraActivity.class);
                                mIntent.putExtra(CameraActivity.CAPTURE_MODE, CameraActivity.CAPTURE_MODE_PHOTO);
                                startActivityForResult(mIntent, DisasterModeActivity.REQUEST_CAPTURE_PHOTO);

                                break;
                            }
                            case 1: {
                                // 動画を撮影
                                Intent mIntent = new Intent(DisasterModeActivity.this, CameraActivity.class);
                                mIntent.putExtra(CameraActivity.CAPTURE_MODE, CameraActivity.CAPTURE_MODE_MOVIE);
                                startActivityForResult(mIntent, DisasterModeActivity.REQUEST_CAPTURE_MOVIE);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_ADD_ATTENDANCE_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_add_attendance_menu_title);
                builder.setItems(R.array.dialog_add_attendance_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // リストから追加する
                                if (studentSheets.size() != 0) {
                                    showDialog(DisasterModeActivity.DIALOG_CSV_FILE_LIST);
                                }
                                else {
                                    Toast.makeText(DisasterModeActivity.this, R.string.error_list_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 1: {
                                // 学籍番号で検索する
                                showDialog(DisasterModeActivity.DIALOG_SEARCH_STUDENT_NO);

                                break;
                            }
                            case 2: {
                                // 手動で登録する
                                showDialog(DisasterModeActivity.DIALOG_INPUT_STUDENT_INFO);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_CSV_FILE_LIST: {
                selectedSheet = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_csv_file_list_title);
                String[] subjects = new String[studentSheets.size()];
                for (int i = 0; i < studentSheets.size(); i++) {
                    subjects[i] = studentSheets.get(i).getSubject();
                }
                builder.setItems(subjects, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedSheet = studentSheets.get(which);
                        showDialog(DisasterModeActivity.DIALOG_STUDENT_LIST);
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
            case DisasterModeActivity.DIALOG_STUDENT_LIST: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(selectedSheet.getSubject());
                final ArrayList<Student> mSheet = selectedSheet.getStudentList();
                String[] students = new String[mSheet.size()];
                for (int i = 0; i < mSheet.size(); i++) {
                    Student mStudent = mSheet.get(i);
                    students[i] = mStudent.getStudentNo() + " " + mStudent.getStudentName();
                }
                builder.setItems(students, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentAttendance = new Attendance(mSheet.get(which), getResources());
                        int position;
                        if (!mAttendanceSheet.hasStudentNo(currentAttendance.getStudentNo())) {
                            if (!mPreferenceUtil.isLocationEnabled(false)) {
                                currentAttendance.setStatus(Attendance.ATTENDANCE);
                            }
                            else {
                                currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                            }
                            addAttendance(currentAttendance.getStudentNo(), currentAttendance);
                            position = mAttendanceListAdapter.getCount() - 1;
                            isSaved = false;
                        }
                        else {
                            currentAttendance = mAttendanceSheet.getByStudentNo(currentAttendance.getStudentNo());
                            if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                                if (!mPreferenceUtil.isLocationEnabled(false)) {
                                    currentAttendance.setStatus(Attendance.ATTENDANCE);
                                }
                                else {
                                    currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                                }
                                isSaved = false;
                            }
                            else {
                                Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                            }
                            position = mAttendanceListAdapter.getPosition(currentAttendance);
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
            case DisasterModeActivity.DIALOG_SEARCH_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(DisasterModeActivity.this);
                editTextForStudentNo = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNo);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean beforeReadingFlag = isReading;
                        if (!isReading) {
                            isReading = true;
                        }
                        String studentNo = editTextForStudentNo.getText().toString().toUpperCase();
                        onStudentNoReaded(studentNo);
                        isReading = beforeReadingFlag;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_INPUT_STUDENT_INFO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_input_student_info_title);

                LayoutInflater inflater = LayoutInflater.from(DisasterModeActivity.this);
                View mView = inflater.inflate(R.layout.dialog_input_student_info, null);
                editTextForStudentNo   = (EditText)mView.findViewById(R.id.dialog_student_no);
                editTextForClassName   = (EditText)mView.findViewById(R.id.dialog_class_name);
                editTextForStudentName = (EditText)mView.findViewById(R.id.dialog_student_name);
                editTextForStudentRuby = (EditText)mView.findViewById(R.id.dialog_student_ruby);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo   = editTextForStudentNo.getEditableText().toString();
                        String className   = editTextForClassName.getEditableText().toString();
                        String studentName = editTextForStudentName.getEditableText().toString();
                        String studentRuby = editTextForStudentRuby.getEditableText().toString();
                        int position;
                        if (!mAttendanceSheet.hasStudentNo(studentNo)) {
                            currentAttendance = new Attendance(new Student(studentNo, -1, className, studentName,
                                                                           studentRuby, (String[])null), getResources());
                            if (!mPreferenceUtil.isLocationEnabled(false)) {
                                currentAttendance.setStatus(Attendance.ATTENDANCE);
                            }
                            else {
                                currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                            }
                            addAttendance(studentNo, currentAttendance);
                            position = mAttendanceListAdapter.getCount() - 1;
                            isSaved = false;
                        }
                        else {
                            currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                            if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                                if (!mPreferenceUtil.isLocationEnabled(false)) {
                                    currentAttendance.setStatus(Attendance.ATTENDANCE);
                                }
                                else {
                                    currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                                }
                                isSaved = false;
                            }
                            else {
                                Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                            }
                            position = mAttendanceListAdapter.getPosition(currentAttendance);
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
            case DisasterModeActivity.DIALOG_ASK_REGISTER_READ_ID: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_register_read_id);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDialog(DisasterModeActivity.DIALOG_REGISTER_ID_MENU);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_REGISTER_ID_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_register_id_menu_title);
                builder.setItems(R.array.dialog_register_id_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // リストから追加する
                                if (studentSheets.size() != 0) {
                                    showDialog(DisasterModeActivity.DIALOG_CSV_FILE_LIST_R);
                                }
                                else {
                                    Toast.makeText(DisasterModeActivity.this, R.string.error_list_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 1: {
                                // 学籍番号で検索する
                                showDialog(DisasterModeActivity.DIALOG_SEARCH_STUDENT_NO_R);

                                break;
                            }
                            case 2: {
                                // バーコードを読み取る
                                isRegistering = true;
                                showDialog(DisasterModeActivity.DIALOG_READING_BARCODE);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_CSV_FILE_LIST_R: {
                selectedSheet = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_csv_file_list_title);
                String[] subjects = new String[studentSheets.size()];
                for (int i = 0; i < studentSheets.size(); i++) {
                    subjects[i] = studentSheets.get(i).getSubject();
                }
                builder.setItems(subjects, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedSheet = studentSheets.get(which);
                        showDialog(DisasterModeActivity.DIALOG_STUDENT_LIST_R);
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
            case DisasterModeActivity.DIALOG_STUDENT_LIST_R: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(selectedSheet.getSubject());
                final ArrayList<Student> mSheet = selectedSheet.getStudentList();
                String[] students = new String[mSheet.size()];
                for (int i = 0; i < mSheet.size(); i++) {
                    Student mStudent = mSheet.get(i);
                    students[i] = mStudent.getStudentNo() + " " + mStudent.getStudentName();
                }
                builder.setItems(students, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (readNfcId != null) {
                            Student mStudent = mSheet.get(which);
                            mStudent.addNfcId(readNfcId);
                            try {
                                selectedSheet.saveCsvFile(selectedSheet.getBaseFile(), DisasterModeActivity.CHARACTER_CODE);
                                Toast.makeText(DisasterModeActivity.this, R.string.notice_id_registered, Toast.LENGTH_SHORT).show();

                                currentAttendance = new Attendance(mStudent, getResources());
                                int position;
                                if (!mAttendanceSheet.hasStudentNo(currentAttendance.getStudentNo())) {
                                    if (!mPreferenceUtil.isLocationEnabled(false)) {
                                        currentAttendance.setStatus(Attendance.ATTENDANCE);
                                    }
                                    else {
                                        currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                                    }
                                    addAttendance(currentAttendance.getStudentNo(), currentAttendance);
                                    position = mAttendanceListAdapter.getCount() - 1;
                                    isSaved = false;
                                }
                                else {
                                    currentAttendance = mAttendanceSheet.getByStudentNo(currentAttendance.getStudentNo());
                                    if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                                        if (!mPreferenceUtil.isLocationEnabled(false)) {
                                            currentAttendance.setStatus(Attendance.ATTENDANCE);
                                        }
                                        else {
                                            currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                                        }
                                        isSaved = false;
                                    }
                                    else {
                                        Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                                    }
                                    position = mAttendanceListAdapter.getPosition(currentAttendance);
                                }
                                attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                                attendanceListView.setSelection(position);

                                refreshStudentSheets();
                            }
                            catch (IOException e) {
                                Toast.makeText(DisasterModeActivity.this, R.string.error_id_register_failed, Toast.LENGTH_SHORT).show();
                                Log.e("onCreateDialog", e.getMessage(), e);
                            }
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
            case DisasterModeActivity.DIALOG_SEARCH_STUDENT_NO_R: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(DisasterModeActivity.this);
                editTextForStudentNo = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNo);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo = editTextForStudentNo.getText().toString().toUpperCase();
                        try {
                            if (!registerByStudentNo(studentNo, readNfcId)) {
                                Toast.makeText(DisasterModeActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch (IOException e) {
                            Toast.makeText(DisasterModeActivity.this, R.string.error_id_register_failed, Toast.LENGTH_SHORT).show();
                            Log.e("onCreateDialog", e.getMessage(), e);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_READING_BARCODE: {
                ProgressDialog mProgressDialog = new ProgressDialog(DisasterModeActivity.this);
                mProgressDialog.setMessage(getString(R.string.dialog_reading_barcode));
                mProgressDialog.setCancelable(true);
                mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        boolean retBool = false;

                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            retBool = onKeyDown(keyCode, event);
                        }

                        return retBool;
                    }
                });
                retDialog = mProgressDialog;

                break;
            }
            case DisasterModeActivity.DIALOG_ASK_OVERWRITE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveCsvFile(destFile, DisasterModeActivity.CHARACTER_CODE);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_FETCHING_LOCATION: {
                ProgressDialog mProgressDialog = new ProgressDialog(DisasterModeActivity.this);
                mProgressDialog.setMessage(getString(R.string.dialog_fetching_location));
                mProgressDialog.setCancelable(true);
                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mPreferenceUtil.putLocationEnabled(false);
                        stopUpdateLocation();
                        Toast.makeText(DisasterModeActivity.this, R.string.notice_add_location_disabled, Toast.LENGTH_SHORT).show();
                    }
                });
                retDialog = mProgressDialog;

                break;
            }
            case DisasterModeActivity.DIALOG_ASK_OPEN_LIST_MAKER: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_open_list_maker);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(DisasterModeActivity.this, StudentListMakerActivity.class);
                        startActivity(mIntent);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case DisasterModeActivity.DIALOG_ASK_OPEN_GPS_PREFERENCE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisasterModeActivity.this);
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
                        Toast.makeText(DisasterModeActivity.this, R.string.notice_add_location_disabled, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

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
            case DisasterModeActivity.DIALOG_DISASTER_MENU: {
                mAlertDialog.setTitle(currentAttendance.getStudentNo() + " " + currentAttendance.getStudentName());

                break;
            }
            case DisasterModeActivity.DIALOG_SEARCH_STUDENT_NO: {
                editTextForStudentNo.setText("");

                break;
            }
            case DisasterModeActivity.DIALOG_INPUT_STUDENT_INFO: {
                editTextForStudentNo.setText("");
                editTextForClassName.setText("");
                editTextForStudentName.setText("");
                editTextForStudentRuby.setText("");

                break;
            }
            case DisasterModeActivity.DIALOG_ASK_OVERWRITE: {
                mAlertDialog.setMessage(destFile.getName() + getString(R.string.dialog_ask_overwrite));

                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isSaved) {
            showDialog(DisasterModeActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING);
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
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagReaded(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("IsReading", isReading);
        outState.putBoolean("IsSending", isSending);
        outState.putBoolean("IsRegistering", isRegistering);
        outState.putBoolean("IsSaved", isSaved);
        outState.putBoolean("IsFetchingLocation", isFetchingLocation);
        outState.putString("ReadNfcId", readNfcId);
        outState.putSerializable("AttendanceQueue", attendanceQueue);
        outState.putSerializable("CurrentAttendance", currentAttendance);
        outState.putSerializable("SelectedSheet", selectedSheet);
        outState.putSerializable("AttendanceSheet", mAttendanceSheet);
        outState.putSerializable("AttendanceLocation", mAttendanceLocation);
    }

    /**
     * 出席データを取得する
     * @return 出席データ
     */
    public static AttendanceSheet getAttendanceSheet() {
        return mAttendanceSheet;
    }

    /**
     * アプリケーションコンテキストを取得する
     * @return アプリケーションコンテキスト
     */
    public static Context getApplicationContextForServlet() {
        return applicationContextForServlet;
    }

    /**
     * 出席データをCSV形式で保存する
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     */
    public void saveCsvFile(File csvFile, String encode) {
        try {
            if (!mPreferenceUtil.isLocationEnabled(false)) {
                mAttendanceSheet.saveCsvFile(csvFile, encode);
            }
            else {
                mAttendanceSheet.saveCsvFile(csvFile, encode, mPreferenceUtil.isLatitudeEnabled(false), mPreferenceUtil.isLongitudeEnabled(false),
                                             mPreferenceUtil.isAccuracyEnabled(false));
            }
            isSaved = true;
            Toast.makeText(DisasterModeActivity.this, csvFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Toast.makeText(DisasterModeActivity.this, csvFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
            Log.e("saveCsvFile", e.getMessage(), e);
        }
    }

    /**
     * CSVファイルを読み込み直す
     */
    public void refreshStudentSheets() {
        studentSheets = new ArrayList<StudentSheet>();
        ArrayList<File> csvFiles = new ArrayList<File>();
        for (File mFile : listDir.listFiles()) {
            if (mFile.getName().endsWith(".csv")) {
                csvFiles.add(mFile);
            }
        }
        Comparator<File> mComparator = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        };
        Collections.sort(csvFiles, mComparator);
        for (File csvFile : csvFiles) {
            try {
                studentSheets.add(new StudentSheet(csvFile, DisasterModeActivity.CHARACTER_CODE));
            }
            catch (IOException e) {
                Log.e("refreshStudentSheets", e.getMessage(), e);
            }
        }
    }

    /**
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    public void onCharTyped(char c) {
        if (Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
        }
        inputBuffer.append(c);
        if (inputBuffer.length() == 6) {
            onStudentNoReaded(inputBuffer.toString());
        }
    }

    /**
     * 学籍番号を読み取った際に呼び出される
     * @param studentNo 学籍番号
     */
    public void onStudentNoReaded(String studentNo) {
        if (!isRegistering) {
            // 通常時
            if (isReading) {
                int position;
                if (mAttendanceSheet.hasStudentNo(studentNo)) {
                    // 既に学籍番号に対応するデータが存在する場合はその行を選択する
                    currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                    if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                        if (!mPreferenceUtil.isLocationEnabled(false)) {
                            currentAttendance.setStatus(Attendance.ATTENDANCE);
                        }
                        else {
                            currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                        }
                        isSaved = false;
                    }
                    else {
                        Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                    }
                    position = mAttendanceListAdapter.getPosition(currentAttendance);
                }
                else {
                    // 存在しない場合は他のリストを検索する
                    boolean isExisted = false;
                    if (studentSheets.size() != 0) {
                        for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                            StudentSheet tempStudentSheet = studentSheets.get(i);
                            if (tempStudentSheet.hasStudentNo(studentNo)) {
                                currentAttendance = new Attendance(tempStudentSheet.getByStudentNo(studentNo), getResources());
                                isExisted = true;
                            }
                        }
                    }
                    if (!isExisted) {
                        // 他のリストにも存在しない場合は学籍番号のみで追加する
                        currentAttendance = new Attendance(new Student(studentNo), getResources());
                    }
                    if (!mPreferenceUtil.isLocationEnabled(false)) {
                        currentAttendance.setStatus(Attendance.ATTENDANCE);
                    }
                    else {
                        currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                    }
                    addAttendance(studentNo, currentAttendance);
                    position = mAttendanceListAdapter.getCount() - 1;
                    isSaved = false;
                }
                attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                attendanceListView.setSelection(position);
            }
        }
        else {
            // NFCタグ登録時
            try {
                if (!registerByStudentNo(studentNo, readNfcId)) {
                    Toast.makeText(DisasterModeActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e) {
                Toast.makeText(DisasterModeActivity.this, R.string.error_id_register_failed, Toast.LENGTH_SHORT).show();
                Log.e("onStudentNoReaded", e.getMessage(), e);
            }
            isRegistering = false;
            try {
                dismissDialog(DisasterModeActivity.DIALOG_READING_BARCODE);
            }
            catch (IllegalArgumentException e) {}
        }
    }

    /**
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        StringBuilder rawId = new StringBuilder(Util.byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
        while (rawId.length() < 16) {
            rawId.append("0");
        }
        String id = rawId.toString();
        if (isReading && studentSheets.size() != 0) {
            boolean isExisted = false;
            for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                StudentSheet tempStudentSheet = studentSheets.get(i);
                if (tempStudentSheet.hasNfcId(id)) {
                    Student mStudent = tempStudentSheet.getByNfcId(id);
                    int position;
                    if (!mAttendanceSheet.hasStudentNo(mStudent.getStudentNo())) {
                        currentAttendance = new Attendance(mStudent, getResources());
                        if (!mPreferenceUtil.isLocationEnabled(false)) {
                            currentAttendance.setStatus(Attendance.ATTENDANCE);
                        }
                        else {
                            currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                        }
                        addAttendance(id, currentAttendance);
                        position = mAttendanceListAdapter.getCount() - 1;
                        isSaved = false;
                    }
                    else {
                        currentAttendance = mAttendanceSheet.getByStudentNo(mStudent.getStudentNo());
                        position = mAttendanceListAdapter.getPosition(currentAttendance);
                        Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                    }
                    attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                    attendanceListView.setSelection(position);
                    isExisted = true;
                }
            }

            if (!isExisted) {
                readNfcId = id;
                showDialog(DisasterModeActivity.DIALOG_ASK_REGISTER_READ_ID);
            }
        }
    }

    /**
     * 出席データを追加する
     * @param inAttendance 出席データ
     */
    public void addAttendance(String id, Attendance inAttendance) {
        inAttendance.setStudentNum(mAttendanceSheet.size() + 1);
        mAttendanceSheet.add(id, inAttendance);
        mAttendanceListAdapter.add(inAttendance);

        if (mPreferenceUtil.isSendServerEnabled(false)) {
            attendanceQueue.enqueue(inAttendance);
        }
    }

    /**
     * 学生データにNFCタグを登録して出席データを追加する
     * @param studentNo 学籍番号
     * @param id NFCタグ
     * @return 学生が存在したならばtrue しなかったならばfalse
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public boolean registerByStudentNo(String studentNo, String id) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        boolean isExisted = false;
        if (studentSheets.size() != 0) {
            for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                StudentSheet tempStudentSheet = studentSheets.get(i);
                if (tempStudentSheet.hasStudentNo(studentNo)) {
                    isExisted = true;
                    Student mStudent = tempStudentSheet.getByStudentNo(studentNo);
                    mStudent.addNfcId(id);
                    tempStudentSheet.saveCsvFile(tempStudentSheet.getBaseFile(), DisasterModeActivity.CHARACTER_CODE);
                    Toast.makeText(DisasterModeActivity.this, R.string.notice_id_registered, Toast.LENGTH_SHORT).show();

                    currentAttendance = new Attendance(mStudent, getResources());
                    int position;
                    if (!mAttendanceSheet.hasStudentNo(studentNo)) {
                        if (!mPreferenceUtil.isLocationEnabled(false)) {
                            currentAttendance.setStatus(Attendance.ATTENDANCE);
                        }
                        else {
                            currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                        }
                        addAttendance(currentAttendance.getStudentNo(), currentAttendance);
                        position = mAttendanceListAdapter.getCount() - 1;
                        isSaved = false;
                    }
                    else {
                        currentAttendance = mAttendanceSheet.getByStudentNo(studentNo);
                        if (currentAttendance.getStatus() == Attendance.ABSENCE) {
                            if (!mPreferenceUtil.isLocationEnabled(false)) {
                                currentAttendance.setStatus(Attendance.ATTENDANCE);
                            }
                            else {
                                currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                            }
                            isSaved = false;
                        }
                        else {
                            Toast.makeText(DisasterModeActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                        }
                        position = mAttendanceListAdapter.getPosition(currentAttendance);
                    }
                    attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
                    attendanceListView.setSelection(position);

                    refreshStudentSheets();
                }
            }
        }

        return isExisted;
    }

    /**
     * 位置情報の取得を開始する
     */
    public void startUpdateLocation() {
        if (mAttendanceLocation == null) {
            showDialog(DisasterModeActivity.DIALOG_FETCHING_LOCATION);
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

    /**
     * 位置情報の取得を停止する
     */
    public void stopUpdateLocation() {
        isFetchingLocation = false;
        mLocationManager.removeUpdates(mLocationListener);
    }
}