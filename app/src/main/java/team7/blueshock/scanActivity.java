package team7.blueshock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class scanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }

    public void scnBtnClick( View V ) {
        Toast.makeText(this, "you pushed my button ", Toast.LENGTH_SHORT).show();
    }

    public void cancelBtnClick( View V ) {
        finish();
    }
}
