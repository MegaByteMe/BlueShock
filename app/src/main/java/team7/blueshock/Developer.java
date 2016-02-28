package team7.blueshock;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class Developer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);
    }

    // Open DetailsActivity
    public void button3( View V ) {
        // TODO Remove for final version
        Bundle xtra = getIntent().getExtras();
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtras(xtra);
        startActivity(i);
        finish();
    }

    // Clear Preferences
    public void button4( View V ) {
        // TODO Remove for final version
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("PAIRED", false);
        i.putExtra("AXIS", new boolean[]{false, false, false});
        i.putExtra("SVAL", 0);
        Log.d("Blue", "Preferences cleared!");
        startActivity(i);
        finish();
    }

    // End Application
    public void button5( View V ) {
        System.exit(0);
    }

}
