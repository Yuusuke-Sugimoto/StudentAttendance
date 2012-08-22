package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;

/**
 * 設定画面
 * @author 杉本祐介
 */
public class SettingActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CHANGE_ATTENDANCE_DIR = 0;
    // ダイアログのID
    private static final int DIALOG_ILLEGAL_FILE_NAME = 0;
    /**
     * 位置情報の更新間隔の初期値
     */
    private static final int DEFAULT_LOCATION_INTERVAL = 5;
    /**
     * 出席データの保存名の初期値
     */
    private static final String DEFAULT_ATTENDANCE_NAME = "%S_%y%M%d%h%m%s";

    // 変数の宣言
    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);

        mPreferenceUtil = new PreferenceUtil(SettingActivity.this);

        final EditTextPreference locationIntervalPreference = (EditTextPreference)findPreference("setting_location_interval");
        locationIntervalPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean retBool = true;

                String newLocationInterval = (String)newValue;
                if (newLocationInterval.length() == 0) {
                    // 値が空だった場合初期値をセットする
                    mPreferenceUtil.putLocationInterval(SettingActivity.DEFAULT_LOCATION_INTERVAL);
                    locationIntervalPreference.setText(String.valueOf(SettingActivity.DEFAULT_LOCATION_INTERVAL));
                    retBool = false;
                }

                return retBool;
            }
        });

        Preference attendanceDirPreference = (Preference)findPreference("setting_attendance_dir");
        attendanceDirPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent mIntent = new Intent(SettingActivity.this, FileChooseActivity.class);
                mIntent.putExtra("initDirPath", mPreferenceUtil.getAttendanceDir());
                mIntent.putExtra("filter", ".*");
                mIntent.putExtra("dirMode", true);
                startActivityForResult(mIntent, SettingActivity.REQUEST_CHANGE_ATTENDANCE_DIR);

                return true;
            }
        });

        final EditTextPreference attendanceNamePreference = (EditTextPreference)findPreference("setting_attendance_name");
        attendanceNamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean retBool = true;

                String newFileName = (String)newValue;
                if (newFileName.length() != 0) {
                    if (newFileName.matches(".*(<|>|:|\\*|\\?|\"|/|\\\\|\\||\u00a5).*")) {
                        // 使用不可能な文字列(< > : * ? " / \ |)が含まれていればダイアログを表示
                        showDialog(SettingActivity.DIALOG_ILLEGAL_FILE_NAME);
                        retBool = false;
                    }
                }
                else {
                    // 値が空だった場合初期値をセットする
                    mPreferenceUtil.putAttendanceName(SettingActivity.DEFAULT_ATTENDANCE_NAME);
                    attendanceNamePreference.setText(SettingActivity.DEFAULT_ATTENDANCE_NAME);
                    retBool = false;
                }

                return retBool;
            }
        });

        WifiManager mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = mWifiInfo.getIpAddress();
        String strIpAddress = (ipAddress & 0xff) + "."
                              + ((ipAddress >> 8)  & 0xff) + "."
                              + ((ipAddress >> 16) & 0xff) + "."
                              + ((ipAddress >> 24) & 0xff);
        Preference ipAddressPreference = (Preference)findPreference("setting_ip_address");
        ipAddressPreference.setSummary(strIpAddress);

        Preference apacheLicensePreference = (Preference)findPreference("setting_license_apache");
        apacheLicensePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent mIntent = new Intent(SettingActivity.this, ApacheLicenseActivity.class);
                startActivity(mIntent);

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(SettingActivity.this);

        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(SettingActivity.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SettingActivity.REQUEST_CHANGE_ATTENDANCE_DIR: {
                if (resultCode == Activity.RESULT_OK) {
                    String filePath = data.getStringExtra("filePath");
                    mPreferenceUtil.putAttendanceDir(filePath);
                }

                break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog retDialog = null;

        switch (id) {
            case SettingActivity.DIALOG_ILLEGAL_FILE_NAME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setTitle(R.string.error);
                builder.setMessage(R.string.error_illegal_file_name);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
        }

        return retDialog;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateUi();
    }

    /**
     * 各項目の表示を更新する
     */
    public void updateUi() {
        ListPreference locationProviderPreference = (ListPreference)findPreference("setting_location_provider");
        locationProviderPreference.setSummary(locationProviderPreference.getEntry());

        EditTextPreference locationIntervalPreference = (EditTextPreference)findPreference("setting_location_interval");
        locationIntervalPreference.setSummary(mPreferenceUtil.getLocationInterval() + "分");

        PreferenceScreen locationFormatPreference = (PreferenceScreen)findPreference("setting_location_format");
        StringBuilder locationFormatSummary = new StringBuilder();
        if (mPreferenceUtil.isLatitudeEnabled()) {
            locationFormatSummary.append(getString(R.string.setting_location_format_latitude_title));
        }
        if (mPreferenceUtil.isLongitudeEnabled()) {
            if (locationFormatSummary.length() != 0) {
                locationFormatSummary.append(",");
            }
            locationFormatSummary.append(getString(R.string.setting_location_format_longitude_title));
        }
        if (mPreferenceUtil.isAltitudeEnabled()) {
            if (locationFormatSummary.length() != 0) {
                locationFormatSummary.append(",");
            }
            locationFormatSummary.append(getString(R.string.setting_location_format_altitude_title));
        }
        if (mPreferenceUtil.isAccuracyEnabled()) {
            if (locationFormatSummary.length() != 0) {
                locationFormatSummary.append(",");
            }
            locationFormatSummary.append(getString(R.string.setting_location_format_accuracy_title));
        }
        if (locationFormatSummary.length() == 0) {
            locationFormatSummary.append(getString(R.string.setting_location_format_output_null));
        }
        locationFormatPreference.setSummary(locationFormatSummary.toString());

        Preference attendanceDirPreference = (Preference)findPreference("setting_attendance_dir");
        attendanceDirPreference.setSummary(mPreferenceUtil.getAttendanceDir());

        EditTextPreference attendanceNamePreference = (EditTextPreference)findPreference("setting_attendance_name");
        attendanceNamePreference.setSummary(mPreferenceUtil.getAttendanceName());

        getListView().invalidateViews();
    }
}