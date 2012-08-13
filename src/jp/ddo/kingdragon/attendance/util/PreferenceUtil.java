package jp.ddo.kingdragon.attendance.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

/**
 * 設定内容を管理するクラス
 * @author 杉本祐介
 */
public class PreferenceUtil {
    // 定数の宣言
    // "位置情報の取得方法"用
    public static final int LOCATION_PROVIDER_NETWORK = 0;
    public static final int LOCATION_PROVIDER_GPS     = 1;

    // 変数の宣言
    /**
     * SharedPreferences取得用のコンテキスト
     */
    private Context mContext;

    // コンストラクタ
    /**
     * インスタンスを生成する
     * @param mContext SharedPreferences取得用のコンテキスト 非null
     */
    public PreferenceUtil(Context inContext) {
        if (inContext != null) {
            mContext = inContext;
        }
        else {
            throw new IllegalArgumentException("PreferenceUtil : inContextの値がnullです。");
        }
    }

    /**
     * "位置情報を付加する"の設定値を変更する
     * @param value 新しい値
     */
    public void putLocationEnabled(boolean value) {
        putBoolean("setting_add_location", false);
    }
    /**
     * "位置情報を付加する"が有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isLocationEnabled() {
        return getBoolean("setting_add_location", false);
    }

    /**
     * "位置情報の取得方法"の設定値を取得する
     * @return "位置情報の取得方法"の設定値 未設定ならば0
     */
    public int getLocationProvider() {
        return Integer.parseInt(getString("setting_location_provider", "0"));
    }

    /**
     * 位置情報の更新間隔を変更する
     * @param locationInterval 位置情報の更新間隔
     */
    public void putLocationInterval(int locationInterval) {
        putString("setting_location_interval", String.valueOf(locationInterval));
    }
    /**
     * 位置情報の更新間隔を取得する
     * @return 位置情報の更新間隔 未設定ならば5
     */
    public int getLocationInterval() {
        return Integer.parseInt(getString("setting_location_interval", "5"));
    }

    /**
     * "緯度"が有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isLatitudeEnabled() {
        return getBoolean("setting_location_format_latitude", false);
    }

    /**
     * "経度"が有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isLongitudeEnabled() {
        return getBoolean("setting_location_format_longitude", false);
    }

    /**
     * "高度"が有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isAltitudeEnabled() {
        return getBoolean("setting_location_format_altitude", false);
    }

    /**
     * "精度"が有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isAccuracyEnabled() {
        return getBoolean("setting_location_format_accuracy", false);
    }

    /**
     * 出席データの保存先を変更する
     * @param attendanceDir 出席データの保存先のパス
     */
    public void putAttendanceDir(String attendanceDir) {
        putString("setting_attendance_dir", attendanceDir);
    }
    /**
     * 出席データの保存先を取得する
     * @return 出席データの保存先 未設定ならば外部SDカードのパス/StudentAttendance
     */
    public String getAttendanceDir() {
        return getString("setting_attendance_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/StudentAttendance");
    }

    /**
     * 出席データの保存名を変更する
     * @param attendanceName 出席データの保存名
     */
    public void putAttendanceName(String attendanceName) {
        putString("setting_attendance_name", attendanceName);
    }
    /**
     * 出席データの保存先を取得する
     * @return 出席データの保存名 未設定ならば"%S_%y%M%d%h%m%s"
     */
    public String getAttendanceName() {
        return getString("setting_attendance_name", "%S_%y%M%d%h%m%s");
    }

    /**
     * 災害モードが有効かどうかを取得する
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isDisasterModeEnabled() {
        return getBoolean("setting_disaster_mode", false);
    }

    /**
     * StringをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     */
    public void putString(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(key, value).commit();
    }
    /**
     * SharedPreferencesからStringを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @return keyに対応する値 なければデフォルト値
     */
    public String getString(String key, String defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        return prefs.getString(key, defValue);
    }

    /**
     * intをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     */
    public void putInt(String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putInt(key, value).commit();
    }
    /**
     * SharedPreferencesからintを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @return keyに対応する値 なければデフォルト値
     */
    public int getInt(String key, int defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        return prefs.getInt(key, defValue);
    }

    /**
     * booleanをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     */
    public void putBoolean(String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putBoolean(key, value).commit();
    }
    /**
     * SharedPreferencesからbooleanを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @return keyに対応する値 なければデフォルト値
     */
    public boolean getBoolean(String key, boolean defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        return prefs.getBoolean(key, defValue);
    }

    /**
     * SharedPreferencesから値を削除する
     * @param key 削除する値のキー
     */
    public void remove(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().remove(key).commit();
    }
}