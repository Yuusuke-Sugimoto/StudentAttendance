package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * スプラッシュ画面
 * 参考:Androidアプリでスプラッシュ画面を表示させる方法 - MIRAI THE FUTURE
 *      http://d.hatena.ne.jp/yamamotodaisaku/20100126/1264504434
 * @author 杉本祐介
 */
public class SplashActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mIntent = new Intent(SplashActivity.this, StudentAttendanceActivity.class);
                startActivity(mIntent);
                
                finish();
            }
        }, 1000);
    }
}