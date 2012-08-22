package jp.ddo.kingdragon.attendance;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import jp.ddo.kingdragon.attendance.util.PreferenceUtil;

/**
 * 設定画面
 * @author 杉本祐介
 */
public class CameraSettingActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    // 変数の宣言
    /**
     * 他スレッドからのUIの更新に使用
     */
    private Handler mHandler;

    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.camera_preference);

        mHandler = new Handler();

        mPreferenceUtil = new PreferenceUtil(CameraSettingActivity.this);

        // 撮影する画像のサイズを選択できるようにする
        int[][] pictureSizes = mPreferenceUtil.getSupportedPictureSizes();
        int defValueForPictureSize = 0;
        String[] entryLabelsForPictureSize = new String[pictureSizes.length];
        String[] entryValuesForPictureSize = new String[pictureSizes.length];
        for (int i = 0; i < pictureSizes.length; i++) {
            entryLabelsForPictureSize[i] = pictureSizes[i][PreferenceUtil.WIDTH] + "x" + pictureSizes[i][PreferenceUtil.HEIGHT];
            entryValuesForPictureSize[i] = String.valueOf(i);
            if (pictureSizes[i][PreferenceUtil.WIDTH] * pictureSizes[i][PreferenceUtil.HEIGHT]
                <= PreferenceUtil.DEFAULT_SIZE_WIDTH * PreferenceUtil.DEFAULT_SIZE_HEIGHT) {
                defValueForPictureSize = i;
            }
        }

        ListPreference pictureSizePreference = (ListPreference)findPreference("setting_picture_size");
        pictureSizePreference.setDefaultValue(String.valueOf(defValueForPictureSize));
        pictureSizePreference.setSummary(entryLabelsForPictureSize[defValueForPictureSize] + "\n" + getString(R.string.setting_picture_size_summary));
        pictureSizePreference.setEntries(entryLabelsForPictureSize);
        pictureSizePreference.setEntryValues(entryValuesForPictureSize);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(CameraSettingActivity.this);

        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(CameraSettingActivity.this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateUi();
    }

    /**
     * 各項目の表示を更新する
     */
    public void updateUi() {
        ListPreference rotationPref = (ListPreference)findPreference("setting_rotation");
        rotationPref.setSummary(rotationPref.getEntry());

        ListPreference pictureSizePref = (ListPreference)findPreference("setting_picture_size");
        pictureSizePref.setSummary(pictureSizePref.getEntry() + "\n" + getString(R.string.setting_picture_size_summary));

        ListPreference movieQualityPref = (ListPreference)findPreference("setting_movie_quality");
        movieQualityPref.setSummary(movieQualityPref.getEntry());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                getListView().invalidateViews();
            }
        });
    }
}