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
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
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
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING = 0;
    private static final int DIALOG_ATTENDANCE_MENU         = 1;
    private static final int DIALOG_ASK_OVERWRITE           = 2;
    private static final int DIALOG_FETCHING_LOCATION       = 3;
    private static final int DIALOG_ASK_OPEN_LIST_MAKER     = 4;

    // 変数の宣言
    /**
     * 読み取り中かどうか
     */
    private boolean isReading;
    /**
     * 保存済みかどうか
     */
    private boolean isSaved;
    /**
     * 現在地を取得中かどうか
     */
    private boolean isFetchingLocation;

    /**
     * 保存用ディレクトリ
     */
    private File baseDir;
    /**
     * 保存先のファイル
     */
    private File saveFile;
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
     * 出席の種別を指定するスピナー
     */
    private Spinner attendanceKindSpinner;
    /**
     * 読み取り開始ボタン
     */
    private Button readStartButton;

    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    /**
     * NFCタグの読み取りに使用
     */
    private NfcAdapter mNfcAdapter;
    /**
     * NFCタグの読み取りに使用
     */
    private PendingIntent mPendingIntent;

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
        setTitle(R.string.attendance_title);
        setContentView(R.layout.student_attendance);

        isReading = false;
        isSaved = true;
        isFetchingLocation = false;

        mAttendanceLocation = null;
        mPreferenceUtil = new PreferenceUtil(StudentAttendanceActivity.this);

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
        mAttendanceListAdapter = new AttendanceListAdapter(StudentAttendanceActivity.this, 0);
        attendanceListView.setAdapter(mAttendanceListAdapter);
        attendanceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                attendanceListView.performItemClick(view, position, id);
                currentAttendance = (Attendance)parent.getItemAtPosition(position);
                showDialog(StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU);

                return true;
            }
        });

        attendanceKindSpinner = (Spinner)findViewById(R.id.attendance_kind);
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

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        // 設定情報にデフォルト値をセットする
        PreferenceManager.setDefaultValues(StudentAttendanceActivity.this, R.xml.preference, false);

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
                                                                 location.getAltitude(), location.getAccuracy());
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

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(StudentAttendanceActivity.this, mPendingIntent, filters, techs);
        }

        if (mPreferenceUtil.isLocationEnabled()) {
            if (mAttendanceLocation == null) {
                showDialog(StudentAttendanceActivity.DIALOG_FETCHING_LOCATION);
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
        else {
            mAttendanceLocation = null;
            isFetchingLocation = false;
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(StudentAttendanceActivity.this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // NFCタグ読み取り時にonPauseが実行されるためonStopに移動
        isFetchingLocation = false;
        mLocationManager.removeUpdates(mLocationListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE:
            if (resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                try {
                    mAttendanceSheet = new AttendanceSheet(new File(filePath), "Shift_JIS", getResources());
                    mAttendanceListAdapter = new AttendanceListAdapter(StudentAttendanceActivity.this, 0, mAttendanceSheet.getAttendanceList());
                    attendanceListView.setAdapter(mAttendanceListAdapter);
                    readStartButton.setEnabled(true);
                    readStartButton.setText(R.string.attendance_read_start_label);
                    isReading = false;
                    isSaved = false;
                    Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                    Log.e("onActivityResult", e.getMessage(), e);
                }
            }

            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_attendance_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent mIntent;

        switch (item.getItemId()) {
        case R.id.menu_make_list:
            showDialog(StudentAttendanceActivity.DIALOG_ASK_OPEN_LIST_MAKER);

            break;
        case R.id.menu_setting:
            mIntent = new Intent(StudentAttendanceActivity.this, SettingActivity.class);
            startActivity(mIntent);

            break;
        case R.id.menu_open:
            mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*");
            mIntent.putExtra("extension", "csv");
            startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE);

            break;
        case R.id.menu_save:
            if (mAttendanceSheet != null) {
                // ファイル名を生成
                StringBuilder rawFileName = new StringBuilder(mPreferenceUtil.getAttendanceName() + ".csv");
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

                String fileName = rawFileName.toString();
                File saveDir = new File(mPreferenceUtil.getAttendanceDir());
                saveFile = new File(saveDir, fileName);
                if (saveFile.exists()) {
                    showDialog(StudentAttendanceActivity.DIALOG_ASK_OVERWRITE);
                }
                else {
                    try {
                        if (!mPreferenceUtil.isLocationEnabled()) {
                            mAttendanceSheet.saveCsvFile(saveFile, "Shift_JIS");
                        }
                        else {
                            mAttendanceSheet.saveCsvFile(saveFile, "Shift_JIS", mPreferenceUtil.isLatitudeEnabled(), mPreferenceUtil.isLongitudeEnabled(),
                                                         mPreferenceUtil.isAltitudeEnabled(), mPreferenceUtil.isAccuracyEnabled());
                        }
                        isSaved = true;
                        Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }
            }
            else {
                Toast.makeText(StudentAttendanceActivity.this, R.string.error_saving_data_null, Toast.LENGTH_SHORT).show();
            }

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;

        switch (id) {
        case StudentAttendanceActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
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
        case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
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
                    isSaved = false;
                }
            });
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.dialog_ask);
            builder.setMessage("");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        if (!mPreferenceUtil.isLocationEnabled()) {
                            mAttendanceSheet.saveCsvFile(saveFile, "Shift_JIS");
                        }
                        else {
                            mAttendanceSheet.saveCsvFile(saveFile, "Shift_JIS", mPreferenceUtil.isLatitudeEnabled(), mPreferenceUtil.isLongitudeEnabled(),
                                                         mPreferenceUtil.isAltitudeEnabled(), mPreferenceUtil.isAccuracyEnabled());
                        }
                        Toast.makeText(StudentAttendanceActivity.this, saveFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentAttendanceActivity.this, saveFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
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
                    Intent mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
                    String initDirPath;
                    File parent = saveFile.getParentFile();
                    if (parent != null) {
                        initDirPath = parent.getAbsolutePath();
                    }
                    else {
                        initDirPath = "/";
                    }
                    mIntent.putExtra("initDirPath", initDirPath);
                    mIntent.putExtra("filter", ".*");
                    mIntent.putExtra("extension", "csv");
                    startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE);
                }
            });
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case StudentAttendanceActivity.DIALOG_FETCHING_LOCATION:
            ProgressDialog mProgressDialog = new ProgressDialog(StudentAttendanceActivity.this);
            mProgressDialog.setMessage(getString(R.string.dialog_fetching_location));
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mPreferenceUtil.putLocationEnabled(false);
                    isFetchingLocation = false;
                    mLocationManager.removeUpdates(mLocationListener);
                    Toast.makeText(StudentAttendanceActivity.this, R.string.notice_add_location_disabled, Toast.LENGTH_SHORT).show();
                }
            });
            retDialog = mProgressDialog;

            break;
        case StudentAttendanceActivity.DIALOG_ASK_OPEN_LIST_MAKER:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.dialog_ask);
            builder.setMessage(R.string.dialog_ask_open_list_maker);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent mIntent = new Intent(StudentAttendanceActivity.this, StudentListMakerActivity.class);
                    startActivity(mIntent);
                }
            });
            builder.setNegativeButton(android.R.string.no, null);
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
        case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU:
            mAlertDialog.setTitle(currentAttendance.getStudentNo() + " " + currentAttendance.getStudentName());

            break;
        case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE:
            mAlertDialog.setMessage(saveFile.getName() + getString(R.string.dialog_ask_overwrite));

            break;
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
                currentAttendance.setStatus(attendanceKindSpinner.getSelectedItemPosition());
            }
            else {
                currentAttendance.setStatus(attendanceKindSpinner.getSelectedItemPosition(), mAttendanceLocation);
            }
            int position = mAttendanceListAdapter.getPosition(currentAttendance);
            attendanceListView.performItemClick(attendanceListView, position, attendanceListView.getItemIdAtPosition(position));
            attendanceListView.setSelection(position);
            isSaved = false;
        }
    }
}