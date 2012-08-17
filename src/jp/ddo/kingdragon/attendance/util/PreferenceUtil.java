package jp.ddo.kingdragon.attendance.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
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
    // "撮影時の向き"の値
    public static final int ROTATION_AUTO         = 0;
    public static final int ROTATION_USER         = 1;
    public static final int ROTATION_PORTRAIT     = 2;
    public static final int ROTATION_LANDSCAPE    = 3;
    public static final int ROTATION_NR_LANDSCAPE = 4;
    // 画像のサイズ
    public static final int DEFAULT_SIZE_WIDTH  = 2048;
    public static final int DEFAULT_SIZE_HEIGHT = 1536;
    // 使用可能な画像サイズの添字
    public static final int WIDTH  = 0;
    public static final int HEIGHT = 1;

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
     * @return 出席データの保存先 未設定ならば外部SDカードのパス/StudentAttendance/AttendanceData
     */
    public String getAttendanceDir() {
        return getString("setting_attendance_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/StudentAttendance/AttendanceData");
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
     * 対応している画像サイズの一覧を保存したかどうかの状態を保存する
     * @param isSaved 保存したかどうか
     */
    public void putSupportedPictureSizesSaved(boolean isSaved) {
        putBoolean("SupportedPictureSizeSaved", isSaved);
    }
    /**
     * 対応している画像サイズの一覧を保存したかどうかを取得する
     * @return 保存済みならばtrue そうでなければfalse
     */
    public boolean isSupportedPictureSizesSaved() {
        return getBoolean("SupportedPictureSizeSaved", false);
    }
    
    /**
     * 対応しているプレビューサイズの一覧を保存したかどうかの状態を保存する
     * @param isSaved 保存したかどうか
     */
    public void putSupportedPreviewSizesSaved(boolean isSaved) {
        putBoolean("SupportedPreviewSizeSaved", isSaved);
    }
    /**
     * 対応しているプレビューサイズの一覧を保存したかどうかを取得する
     * @return 保存済みならばtrue そうでなければfalse
     */
    public boolean isSupportedPreviewSizesSaved() {
        return getBoolean("SupportedPreviewSizeSaved", false);
    }

    /**
     * "撮影時の向き"の設定値を取得する
     * @return "撮影時の向き"の設定値 未設定ならば0
     */
    public int getRotationSetting() {
        return Integer.parseInt(getString("setting_rotation", "0"));
    }
    
    /**
     * "撮影する画像のサイズ"の設定値を取得する
     * @return "撮影する画像のサイズ"の設定値 未設定ならば0
     */
    public int getSelectedPictureSize() {
        return Integer.parseInt(getString("setting_picture_size", "0"));
    }

    /**
     * "撮影時にAFを行う"が有効かどうかを調べる
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isTakeAutoFocusEnable() {
        return getBoolean("setting_use_take_autofocus", false);
    }

    /**
     * "タップ時にAFを行う"が有効かどうかを調べる
     * @return 有効ならばtrue 無効または未設定ならばfalse
     */
    public boolean isTapAutoFocusEnable() {
        return getBoolean("setting_use_tap_autofocus", false);
    }
    
    /**
     * 使用可能な画像サイズの一覧を保存する
     * @param pictureSizes 使用可能な画像サイズの一覧
     */
    public void putSupportedPictureSizes(Camera.Size[] pictureSizes) {
        putInt("NumOfSizes", pictureSizes.length);
        for (int i = 0; i < pictureSizes.length; i++) {
            putInt("Size" + i + "_Width", pictureSizes[i].width);
            putInt("Size" + i + "_Height", pictureSizes[i].height);
        }
    }
    /**
     * 画像サイズを取得する
     * @param index 画像サイズの添字
     * @return 画像サイズ
     */
    public int[] getSupportedPictureSize(int index) {
        int[] pictureSize = new int[2];
        pictureSize[WIDTH]  = getInt("Size" + index + "_Width", 0);
        pictureSize[HEIGHT] = getInt("Size" + index + "_Height", 0);
        
        return pictureSize;
    }
    /**
     * 使用可能な画像サイズの一覧を取得する
     * @return 使用可能な画像サイズの一覧
     */
    public int[][] getSupportedPictureSizes() {
        int numOfSizes = getInt("NumOfSizes", 0);
        int[][] pictureSizes = new int[numOfSizes][2];
        for (int i = 0; i < pictureSizes.length; i++) {
            pictureSizes[i][WIDTH]  = getInt("Size" + i + "_Width", 0);
            pictureSizes[i][HEIGHT] = getInt("Size" + i + "_Height", 0);
        }
        
        return pictureSizes;
    }
    
    /**
     * 使用可能なプレビューサイズの一覧を保存する
     * @param previewSizes 使用可能なプレビューサイズの一覧
     */
    public void putSupportedPreviewSizes(Camera.Size[] previewSizes) {
        putInt("NumOfPreviewSizes", previewSizes.length);
        for (int i = 0; i < previewSizes.length; i++) {
            putInt("PreviewSize" + i + "_Width", previewSizes[i].width);
            putInt("PreviewSize" + i + "_Height", previewSizes[i].height);
        }
    }
    /**
     * プレビューサイズを取得する
     * @param index プレビューサイズの添字
     * @return プレビューサイズ
     */
    public int[] getSupportedPreviewSize(int index) {
        int[] previewSize = new int[2];
        previewSize[WIDTH]  = getInt("PreviewSize" + index + "_Width", 0);
        previewSize[HEIGHT] = getInt("PreviewSize" + index + "_Height", 0);
        
        return previewSize;
    }
    /**
     * 使用可能なプレビューサイズの一覧を取得する
     * @return 使用可能なプレビューサイズの一覧
     */
    public int[][] getSupportedPreviewSizes() {
        int numOfPreviewSizes = getInt("NumOfPreviewSizes", 0);
        int[][] previewSizes = new int[numOfPreviewSizes][2];
        for (int i = 0; i < previewSizes.length; i++) {
            previewSizes[i][WIDTH]  = getInt("PreviewSize" + i + "_Width", 0);
            previewSizes[i][HEIGHT] = getInt("PreviewSize" + i + "_Height", 0);
        }
        
        return previewSizes;
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