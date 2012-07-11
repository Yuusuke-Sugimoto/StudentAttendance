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
    /**
     * コンストラクタ<br />
     * インスタンスが生成されないようにprivate宣言しておく
     */
    private PreferenceUtil() {}

    /**
     * "位置情報を付加する"の値を変更する
     * @param value 新しい値
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putLocationEnabled(boolean value, Context inContext) {
        PreferenceUtil.putBoolean("setting_add_location", false, inContext);
    }
    /**
     * "位置情報を付加する"が有効かどうかを返す
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public static boolean isLocationEnabled(Context inContext) {
        return PreferenceUtil.getBoolean("setting_add_location", false, inContext);
    }

    /**
     * 位置情報の更新間隔を変更する
     * @param locationInterval 位置情報の更新間隔
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putLocationInterval(int locationInterval, Context inContext) {
        PreferenceUtil.putString("setting_location_interval", String.valueOf(locationInterval), inContext);
    }
    /**
     * 位置情報の更新間隔を返す
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return 位置情報の更新間隔 未設定ならば5
     */
    public static int getLocationInterval(Context inContext) {
        return Integer.parseInt(PreferenceUtil.getString("setting_location_interval", "5", inContext));
    }

    /**
     * 出席データの保存先を変更する
     * @param attendanceDir 出席データの保存先のパス
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putAttendanceDir(String attendanceDir, Context inContext) {
        PreferenceUtil.putString("setting_attendance_dir", attendanceDir, inContext);
    }
    /**
     * 出席データの保存先を返す
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return 出席データの保存先 未設定ならば外部SDカードのパス/StudentAttendance
     */
    public static String getAttendanceDir(Context inContext) {
        return PreferenceUtil.getString("setting_attendance_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/StudentAttendance", inContext);
    }

    /**
     * 出席データの保存名を変更する
     * @param attendanceName 出席データの保存名
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putAttendanceName(String attendanceName, Context inContext) {
        PreferenceUtil.putString("setting_attendance_name", attendanceName, inContext);
    }
    /**
     * 出席データの保存先を返す
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return 出席データの保存名 未設定ならば"%S_%y%M%d%h%m%s"
     */
    public static String getAttendanceName(Context inContext) {
        return PreferenceUtil.getString("setting_attendance_name", "%S_%y%M%d%h%m%s", inContext);
    }

    /**
     * StringをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putString(String key, String value, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);
        prefs.edit().putString(key, value).commit();
    }
    /**
     * SharedPreferencesからStringを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return keyに対応する値 なければデフォルト値
     */
    public static String getString(String key, String defValue, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);

        return prefs.getString(key, defValue);
    }

    /**
     * intをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putInt(String key, int value, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);
        prefs.edit().putInt(key, value).commit();
    }
    /**
     * SharedPreferencesからintを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return keyに対応する値 なければデフォルト値
     */
    public static int getInt(String key, int defValue, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);

        return prefs.getInt(key, defValue);
    }

    /**
     * booleanをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void putBoolean(String key, boolean value, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);
        prefs.edit().putBoolean(key, value).commit();
    }
    /**
     * SharedPreferencesからbooleanを読み出す
     * @param key 読み出す値のキー
     * @param defValue デフォルト値
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return keyに対応する値 なければデフォルト値
     */
    public static boolean getBoolean(String key, boolean defValue, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);

        return prefs.getBoolean(key, defValue);
    }

    /**
     * SharedPreferencesから値を削除する
     * @param key 削除する値のキー
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void remove(String key, Context inContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(inContext);
        prefs.edit().remove(key).commit();
    }
}