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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import jp.ddo.kingdragon.attendance.student.Attendance;
import jp.ddo.kingdragon.attendance.student.AttendanceListAdapter;
import jp.ddo.kingdragon.attendance.student.AttendanceLocation;
import jp.ddo.kingdragon.attendance.student.AttendanceSheet;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * JettyをAndroidアプリに組み込むテスト用のアプリ
 * @author 杉本祐介
 */
public class DisasterModeActivity extends Activity {
    // 定数の宣言
    // ダイアログのID
    private static final int DIALOG_ASK_EXIT                = 0;
    private static final int DIALOG_ATTENDANCE_MENU         = 1;
    private static final int DIALOG_FETCHING_LOCATION       = 2;
    private static final int DIALOG_ASK_OPEN_LIST_MAKER     = 3;
    private static final int DIALOG_ASK_OPEN_GPS_PREFERENCE = 4;
    /**
     * ポート番号
     */
    private static final int PORT_NUMBER = 8080;

    // 変数の宣言
    /**
     * 読み取り中かどうか
     */
    private boolean isReading;
    /**
     * 現在地を取得中かどうか
     */
    private boolean isFetchingLocation;

    /**
     * 保存用フォルダ
     */
    private File baseDir;
    /**
     * 現在扱っている出席データ
     */
    private Attendance currentAttendance;
    /**
     * 出席データの一覧を表示するビュー
     */
    private ListView attendanceListView;
    /**
     * 出席データの一覧を表示するアダプタ
     */
    private AttendanceListAdapter mAttendanceListAdapter;
    /**
     * 現在編集しているシート
     */
    private AttendanceSheet mAttendanceSheet;
    /**
     * 読み取り開始ボタン
     */
    private Button readStartButton;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disaster_mode);

        isReading = false;
        isFetchingLocation = false;

        mAttendanceLocation = null;
        mPreferenceUtil = new PreferenceUtil(DisasterModeActivity.this);

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
        mAttendanceListAdapter = new AttendanceListAdapter(DisasterModeActivity.this, 0);
        attendanceListView.setAdapter(mAttendanceListAdapter);
        attendanceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                attendanceListView.performItemClick(view, position, id);
                currentAttendance = (Attendance)parent.getItemAtPosition(position);
                showDialog(DisasterModeActivity.DIALOG_ATTENDANCE_MENU);

                return true;
            }
        });

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

        // 保存用フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "DisasterMode");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(DisasterModeActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

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
                                                                 location.getAltitude(), location.getAccuracy());
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
        mResourceHandler.setResourceBase(baseDir.getAbsolutePath());
        mHandlerList.addHandler(mResourceHandler);

        ServletContextHandler mServletContextHandler = new ServletContextHandler();
        mServletContextHandler.addServlet(TestServlet.class, "/TestServlet");
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
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPreferenceUtil.isDisasterModeEnabled()) {
            if (mNfcAdapter != null) {
                mNfcAdapter.enableForegroundDispatch(DisasterModeActivity.this, mPendingIntent, filters, techs);
            }

            if (mPreferenceUtil.isLocationEnabled()) {
                /**
                 * GPSが選択されていてGPSが無効になっている場合、設定画面を表示するか確認する
                 * 参考:[Android] GSPが有効か確認し、必要であればGPS設定画面を表示する。 | 株式会社ノベラック スタッフBlog
                 *      http://www.noveluck.co.jp/blog/archives/159
                 */
                if (mPreferenceUtil.getLocationProvider() == PreferenceUtil.LOCATION_PROVIDER_NETWORK
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.disaster_mode_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent mIntent;

        switch (item.getItemId()) {
        case R.id.menu_make_list:
            showDialog(DisasterModeActivity.DIALOG_ASK_OPEN_LIST_MAKER);

            break;
        case R.id.menu_setting:
            mIntent = new Intent(DisasterModeActivity.this, SettingActivity.class);
            startActivity(mIntent);

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;

        switch (id) {
        case DisasterModeActivity.DIALOG_ASK_EXIT:
            builder = new AlertDialog.Builder(DisasterModeActivity.this);
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
        case DisasterModeActivity.DIALOG_ATTENDANCE_MENU:
            builder = new AlertDialog.Builder(DisasterModeActivity.this);
            builder.setTitle(R.string.dialog_attendance_menu_title);
            builder.setItems(R.array.dialog_attendance_menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!mPreferenceUtil.isLocationEnabled()) {
                        currentAttendance.setStatus(which);
                    }
                    else {
                        currentAttendance.setStatus(which, mAttendanceLocation);
                    }
                    attendanceListView.invalidateViews();
                }
            });
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case DisasterModeActivity.DIALOG_FETCHING_LOCATION:
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
        case DisasterModeActivity.DIALOG_ASK_OPEN_LIST_MAKER:
            builder = new AlertDialog.Builder(DisasterModeActivity.this);
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
        case DisasterModeActivity.DIALOG_ASK_OPEN_GPS_PREFERENCE:
            builder = new AlertDialog.Builder(DisasterModeActivity.this);
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

        return(retDialog);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog mAlertDialog = null;
        if (dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch (id) {
        case DisasterModeActivity.DIALOG_ATTENDANCE_MENU:
            mAlertDialog.setTitle(currentAttendance.getStudentNo() + " " + currentAttendance.getStudentName());

            break;
        }
    }

    @Override
    public void onBackPressed() {
        showDialog(DisasterModeActivity.DIALOG_ASK_EXIT);
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagReaded(intent);
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
        if (isReading && mAttendanceSheet != null && mAttendanceSheet.hasNfcId(id)) {
            currentAttendance = mAttendanceSheet.get(id);
            if (!mPreferenceUtil.isLocationEnabled()) {
                currentAttendance.setStatus(Attendance.ATTENDANCE);
            }
            else {
                currentAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
            }
            int position = mAttendanceListAdapter.getPosition(currentAttendance);
            attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
            attendanceListView.setSelection(position);
        }
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
            if (mPreferenceUtil.getLocationProvider() == PreferenceUtil.LOCATION_PROVIDER_GPS) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mPreferenceUtil.getLocationInterval() * 60000,
                                                        0, mLocationListener);
            }
            else {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mPreferenceUtil.getLocationInterval() * 60000,
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