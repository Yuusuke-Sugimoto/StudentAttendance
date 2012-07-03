package jp.ddo.kingdragon.attendance;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * 設定画面
 * @author 杉本祐介
 */
public class SettingActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);
    }
}