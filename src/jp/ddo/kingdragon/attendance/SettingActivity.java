package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
    private static final int DIALOG_ILLEGAL_FILE_NAME      = 0;
    private static final int DIALOG_EDIT_PASSWORD          = 1;
    private static final int DIALOG_OLD_PASSWORD_NOT_MATCH = 2;
    private static final int DIALOG_NEW_PASSWORD_NOT_MATCH = 3;
    private static final int DIALOG_LONG_PASSWORD          = 4;

    // 変数の宣言
    /**
     * 旧パスワード用のEditText
     */
    private EditText editTextForOldPassword;
    /**
     * 新パスワード用のEditText
     */
    private EditText editTextForNewPassword;
    /**
     * 新パスワード(確認)用のEditText
     */
    private EditText editTextForNewPasswordConf;

    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);

        mPreferenceUtil = new PreferenceUtil(SettingActivity.this);

        final EditTextPreference locationIntervalPref = (EditTextPreference)findPreference("setting_location_interval");
        locationIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean retBool = true;

                String newLocationInterval = (String)newValue;
                if (newLocationInterval.length() == 0) {
                    // 値が空だった場合初期値をセットする
                    mPreferenceUtil.removeLocationInterval();
                    locationIntervalPref.setText(String.valueOf(PreferenceUtil.DEFAULT_LOCATION_INTERVAL));
                    retBool = false;
                }

                return retBool;
            }
        });

        Preference attendanceDirPref = (Preference)findPreference("setting_attendance_dir");
        attendanceDirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent mIntent = new Intent(SettingActivity.this, FileChooseActivity.class);
                mIntent.putExtra("initDirPath", mPreferenceUtil.getAttendanceDir(PreferenceUtil.DEFAULT_ATTENDANCE_DIR));
                mIntent.putExtra("filter", ".*");
                mIntent.putExtra("dirMode", true);
                startActivityForResult(mIntent, SettingActivity.REQUEST_CHANGE_ATTENDANCE_DIR);

                return true;
            }
        });

        final EditTextPreference attendanceNamePref = (EditTextPreference)findPreference("setting_attendance_name");
        attendanceNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
                    mPreferenceUtil.removeAttendanceName();
                    attendanceNamePref.setText(PreferenceUtil.DEFAULT_ATTENDANCE_NAME);
                    retBool = false;
                }

                return retBool;
            }
        });

        Preference passwordPref = (Preference)findPreference("setting_password");
        passwordPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(SettingActivity.DIALOG_EDIT_PASSWORD);

                return true;
            }
        });

        final EditTextPreference serverAddressPref = (EditTextPreference)findPreference("setting_server_address");
        serverAddressPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean retBool = true;

                String newAddress = (String)newValue;
                if (newAddress.length() == 0) {
                    // 値が空だった場合初期値をセットする
                    mPreferenceUtil.removeServerAddress();
                    serverAddressPref.setText(PreferenceUtil.DEFAULT_SERVER_ADDRESS);
                    retBool = false;
                }

                return retBool;
            }
        });

        /**
         * 端末のIPアドレスを取得する
         * 参考:自分のIPアドレスを取得する - マイペースなプログラミング日記
         *      http://d.hatena.ne.jp/d-kami/20100803/1280819590
         */
        String ipAddress = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (ipAddress == null && netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = netInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (ipAddress == null && addresses.hasMoreElements()) {
                    String address = addresses.nextElement().getHostAddress();
                    if (address.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}") && !address.equals("0.0.0.0") && !address.equals("127.0.0.1")) {
                        ipAddress = address;
                    }
                }
            }
        }
        catch (SocketException e) {
            Log.e("onCreate", e.getMessage(), e);
        }
        if (ipAddress == null) {
            ipAddress = "0.0.0.0";
        }
        Preference ipAddressPreference = (Preference)findPreference("setting_ip_address");
        ipAddressPreference.setSummary(ipAddress);

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
            case SettingActivity.DIALOG_EDIT_PASSWORD: {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setTitle(R.string.setting_password_title);

                LayoutInflater inflater = LayoutInflater.from(SettingActivity.this);
                View mView = inflater.inflate(R.layout.setting_edit_password, null);
                editTextForOldPassword     = (EditText)mView.findViewById(R.id.setting_old_password);
                editTextForNewPassword     = (EditText)mView.findViewById(R.id.setting_new_password);
                editTextForNewPasswordConf = (EditText)mView.findViewById(R.id.setting_new_password_conf);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldPassword     = editTextForOldPassword.getEditableText().toString();
                        String newPassword     = editTextForNewPassword.getEditableText().toString();
                        String newPasswordConf = editTextForNewPasswordConf.getEditableText().toString();

                        if (oldPassword.equals(mPreferenceUtil.getPassword(PreferenceUtil.DEFAULT_PASSWORD))) {
                            if (newPassword.equals(newPasswordConf)) {
                                if (newPassword.length() <= 16) {
                                    if (newPassword.length() != 0) {
                                        mPreferenceUtil.putPassword(newPassword);
                                    }
                                    else {
                                        mPreferenceUtil.removePassword();
                                    }
                                    editTextForOldPassword.setText("");
                                    editTextForNewPassword.setText("");
                                    editTextForNewPasswordConf.setText("");
                                }
                                else {
                                    showDialog(SettingActivity.DIALOG_LONG_PASSWORD);
                                }
                            }
                            else {
                                showDialog(SettingActivity.DIALOG_NEW_PASSWORD_NOT_MATCH);
                            }
                        }
                        else {
                            showDialog(SettingActivity.DIALOG_OLD_PASSWORD_NOT_MATCH);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        editTextForOldPassword.setText("");
                        editTextForNewPassword.setText("");
                        editTextForNewPasswordConf.setText("");
                    }
                });
                retDialog = builder.create();

                break;
            }
            case SettingActivity.DIALOG_OLD_PASSWORD_NOT_MATCH:
            case SettingActivity.DIALOG_NEW_PASSWORD_NOT_MATCH:
            case SettingActivity.DIALOG_LONG_PASSWORD: {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setTitle(R.string.error);
                builder.setMessage("");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showDialog(SettingActivity.DIALOG_EDIT_PASSWORD);
                    }
                });

                break;
            }
        }

        return retDialog;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog mAlertDialog = null;
        if (dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch (id) {
            case SettingActivity.DIALOG_OLD_PASSWORD_NOT_MATCH: {
                mAlertDialog.setMessage(getString(R.string.error_old_password_not_match));

                break;
            }
            case SettingActivity.DIALOG_NEW_PASSWORD_NOT_MATCH: {
                mAlertDialog.setMessage(getString(R.string.error_new_password_not_match));

                break;
            }
            case SettingActivity.DIALOG_LONG_PASSWORD: {
                mAlertDialog.setMessage(getString(R.string.error_long_password));

                break;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateUi();
    }

    /**
     * 各項目の表示を更新する
     */
    public void updateUi() {
        ListPreference locationProviderPref = (ListPreference)findPreference("setting_location_provider");
        locationProviderPref.setSummary(locationProviderPref.getEntry());

        EditTextPreference locationIntervalPref = (EditTextPreference)findPreference("setting_location_interval");
        locationIntervalPref.setSummary(mPreferenceUtil.getLocationInterval(PreferenceUtil.DEFAULT_LOCATION_INTERVAL) + "分");

        PreferenceScreen locationFormatPref = (PreferenceScreen)findPreference("setting_location_format");
        StringBuilder locationFormatSummary = new StringBuilder();
        if (mPreferenceUtil.isLatitudeEnabled(false)) {
            locationFormatSummary.append(getString(R.string.setting_location_format_latitude_title));
        }
        if (mPreferenceUtil.isLongitudeEnabled(false)) {
            if (locationFormatSummary.length() != 0) {
                locationFormatSummary.append(",");
            }
            locationFormatSummary.append(getString(R.string.setting_location_format_longitude_title));
        }
        if (mPreferenceUtil.isAccuracyEnabled(false)) {
            if (locationFormatSummary.length() != 0) {
                locationFormatSummary.append(",");
            }
            locationFormatSummary.append(getString(R.string.setting_location_format_accuracy_title));
        }
        if (locationFormatSummary.length() == 0) {
            locationFormatSummary.append(getString(R.string.setting_location_format_output_null));
        }
        locationFormatPref.setSummary(locationFormatSummary.toString());

        Preference attendanceDirPref = (Preference)findPreference("setting_attendance_dir");
        attendanceDirPref.setSummary(mPreferenceUtil.getAttendanceDir(PreferenceUtil.DEFAULT_ATTENDANCE_DIR));

        EditTextPreference attendanceNamePref = (EditTextPreference)findPreference("setting_attendance_name");
        attendanceNamePref.setSummary(mPreferenceUtil.getAttendanceName(PreferenceUtil.DEFAULT_ATTENDANCE_NAME));

        Preference passwordPref = (Preference)findPreference("setting_password");
        if (!mPreferenceUtil.getPassword(PreferenceUtil.DEFAULT_PASSWORD).equals(PreferenceUtil.DEFAULT_PASSWORD)) {
            passwordPref.setSummary(R.string.setting_password_registered);
        }
        else {
            passwordPref.setSummary(R.string.setting_password_not_registered);
        }
        
        EditTextPreference serverAddressPref = (EditTextPreference)findPreference("setting_server_address");
        serverAddressPref.setSummary(mPreferenceUtil.getServerAddress(PreferenceUtil.DEFAULT_SERVER_ADDRESS));

        getListView().invalidateViews();
    }
}