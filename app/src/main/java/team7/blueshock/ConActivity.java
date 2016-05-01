/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:2
Change:1

Notes:

*/

package team7.blueshock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ConActivity extends AppCompatActivity {

    private TextView barText;
    private SeekBar sBar;
    private CheckBox xBox;
    private CheckBox yBox;
    private CheckBox zBox;

    private BlueShockConfig config;

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

        if(getIntent().hasExtra("CONFIG")) {
            config = getIntent().getParcelableExtra("CONFIG");
        }
        else config = new BlueShockConfig();

        sBar.setProgress(config.getShockThreshold());
        //barText.setText(Integer.toString(config.getShockThreshold()));
        barText.setText(String.format(Locale.US, String.valueOf(config.getShockThreshold())));

        fixBoxs(new boolean[]{ config.isxBoxSet(), config.isyBoxSet(), config.iszBoxSet() });

        xBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //lockBox();
                if(b) {
                    yBox.setChecked(false);
                    zBox.setChecked(false);
                }
            }
        });

        yBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    xBox.setChecked(false);
                    zBox.setChecked(false);
                }
            }
        });

        zBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    xBox.setChecked(false);
                    yBox.setChecked(false);
                }
            }
        });

        // UI Operation - Setup listener for user modifying the seek bar
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Auto Generated Stub
                //barText.setText(Integer.toString(progress));
                barText.setText(String.format(Locale.US, String.valueOf(progress)));
                config.setShockThreshold(progress);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void setBtnClick( View V) {
        if(sBar.getProgress() > 0 && (xBox.isChecked() | yBox.isChecked() | zBox.isChecked()) ) {

            config.setxBoxSet(xBox.isChecked());
            config.setyBoxSet(yBox.isChecked());
            config.setzBoxSet(zBox.isChecked());
            config.setSETUP(true);

            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("CONFIG", config);
            setResult(RESULT_OK, i);
            finish();
        }
        else Toast.makeText(this, "Please select configuration values", Toast.LENGTH_LONG).show();
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
