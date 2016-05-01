/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:1

Notes:

*/

package team7.blueshock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Splash screen timer
        final int SPLASH_TIME_OUT = 3000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

}
