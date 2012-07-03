package jp.ddo.kingdragon.attendance;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 設定内容を管理するクラス
 * @author 杉本祐介
 */
public class PreferenceUtility {
    /**
     * コンストラクタ<br />
     * インスタンスが生成されないようにprivate宣言しておく
     */
    private PreferenceUtility() {}

    /**
     * "位置情報を付加する"が有効かどうかを調べる
     * @param inContext SharedPreferences取得用のコンテキスト
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public static boolean isLocationEnable(Context inContext) {
        return PreferenceUtility.getBoolean("setting_add_location", false, inContext);
    }

    /**
     * StringをSharedPreferencesに保存する
     * @param key 保存する値のキー
     * @param value 保存する値
     * @param inContext SharedPreferences取得用のコンテキスト
     */
    public static void setString(String key, String value, Context inContext) {
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
    public static void setInt(String key, int value, Context inContext) {
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
    public static void setBoolean(String key, boolean value, Context inContext) {
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
}