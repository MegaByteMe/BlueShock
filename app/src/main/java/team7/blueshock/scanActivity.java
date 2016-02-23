package team7.blueshock;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

public class scanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }

    public void cancelBtnClick( View V ) {
        //finish();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void selBtnClick( View V ) {
        // TODO setting pairing in this method is undesirable - this is for UI testing only
        //Intent intent = new Intent(this, MainActivity.class);
        //startActivity(intent);

        SharedPreferences preferences =getSharedPreferences("PAIRED", MainActivity.MODE_APPEND);
        android.content.SharedPreferences.Editor editor = preferences.edit();

        Log.d("Blue", preferences.getAll().toString());

        editor.putBoolean("PAIRED", true);
        editor.apply();

        finish();
    }

    //TODO need discoevery and pairing code
    public void findBLE() {
    }
}
