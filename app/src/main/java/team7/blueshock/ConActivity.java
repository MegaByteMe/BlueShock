package team7.blueshock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class ConActivity extends AppCompatActivity {

    private TextView barText;
    private SeekBar sBar;
    private CheckBox xBox;
    private CheckBox yBox;
    private CheckBox zBox;

    private  int SHKVAL = 0;

    Bundle xtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_con);

        barText = (TextView) findViewById(R.id.threshText);
        sBar = (SeekBar) findViewById(R.id.shockBar); // gShock threshold user input element
        xBox = (CheckBox) findViewById(R.id.xchkBox);
        yBox = (CheckBox) findViewById(R.id.ychkBox);
        zBox = (CheckBox) findViewById(R.id.zchkBox);

        // Init Checkboxes
        xBox.setChecked(false);
        yBox.setChecked(false);
        zBox.setChecked(false);

        // UI Operation - Setup listener for user modifying the seek bar
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Auto Generated Stub
                barText.setText(Integer.toString(progress));
                SHKVAL = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Auto Generated Stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Auto Generated Stub
            }
        });


        xtra = getIntent().getExtras();

        if (getIntent().hasExtra("SVAL")) {
            SHKVAL = xtra.getInt("SVAL");
            sBar.setProgress(xtra.getInt("SVAL"));
        }
        if (getIntent().hasExtra("AXIS")) fixBoxs(xtra.getBooleanArray("AXIS"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().hasExtra("SVAL")) {
            SHKVAL = xtra.getInt("SVAL");
            sBar.setProgress(xtra.getInt("SVAL"));
        }
        if (getIntent().hasExtra("AXIS")) fixBoxs(xtra.getBooleanArray("AXIS"));
    }

    public void setBtnClick( View V) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("AXIS", chkBoxs());
        i.putExtra("SVAL", SHKVAL);
        i.putExtra("SETUP", true);
        setResult(RESULT_OK, i);
        finish();
    }

    private boolean[] chkBoxs() {
        return ( new boolean[]{ xBox.isChecked(), yBox.isChecked(), zBox.isChecked() } );
    }

    private void fixBoxs( boolean[] b) {
        xBox.setChecked(b[0]);
        yBox.setChecked(b[1]);
        zBox.setChecked(b[2]);
        xBox.refreshDrawableState();
        yBox.refreshDrawableState();
        zBox.refreshDrawableState();
    }
}
