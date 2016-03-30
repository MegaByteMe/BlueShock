package team7.blueshock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class Developer extends AppCompatActivity {

    AlertDialog.Builder alertDialogSEV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        alertDialogSEV = new AlertDialog.Builder(this);
        alertDialogSEV.setTitle("Shock Event!");
        alertDialogSEV.setMessage("A Shock Event meeting the set threshold has occurred.");
        alertDialogSEV.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialogSEV.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialogSEV.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialogSEV.create();

    }

    // Force Shock Event
    public void button( View V ) {
        alertDialogSEV.show();
    }

    // Open DetailsActivity
    public void button2( View V ) {
        // TODO Remove for final version
        Bundle xtra = getIntent().getExtras();
        Intent i = new Intent(this, DetailActivity.class);
        startActivity(i);
        finish();
    }

}
