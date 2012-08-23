package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

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