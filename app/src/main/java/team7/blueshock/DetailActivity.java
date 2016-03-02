package team7.blueshock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
    }

    public void retBtnClick( View V ){
        restoreMain(false);
    }

    public void restoreMain(boolean p) {
        Bundle xtra = getIntent().getExtras();
        int val = 0;
        boolean[] b = new boolean[]{ false, false, false };
        Intent i = new Intent(this, MainActivity.class);

        if(xtra.containsKey("SVAL")) val = xtra.getInt("SVAL");
        if(xtra.containsKey("AXIS")) b = xtra.getBooleanArray("AXIS");
        i.putExtra("PAIRED", p);
        i.putExtra("SVAL", val);
        i.putExtra("AXIS", b);

        startActivity(i);
        finish();
    }

}
