/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:1

Notes:

*/

package team7.blueshock;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {
    private ShockEvent shocking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        final int yOffset = 620;
        final int radius = 3;

        // Get Display Bounds
        Display mDisplay = getWindowManager().getDefaultDisplay();
        Point mDisplaySize = new Point();
        mDisplay.getSize(mDisplaySize);
        final int maxX = mDisplaySize.x;
        final int maxY = mDisplaySize.y;

        TextView shkDispID = (TextView) findViewById(R.id.seventText);
        TextView dateDisp = (TextView) findViewById(R.id.dateText);
        TextView threshDisp = (TextView) findViewById(R.id.threshText);
        TextView axisDisp = (TextView) findViewById(R.id.axisText);

        if(getIntent().hasExtra("EVENT")) {
            shocking = getIntent().getParcelableExtra("EVENT");
        }
        else finishActivity(RESULT_CANCELED);

        if(getIntent().hasExtra("DATA")) shocking.setxData(getIntent().getFloatArrayExtra("DATA"));

        shkDispID.setText(Integer.toString(shocking.getId()));
        dateDisp.setText(shocking.getDateTime());
        threshDisp.setText(Integer.toString(shocking.getsThreshold()));

        String S = "";
        if(shocking.getAxisBox() == 4) S = "X";
        if(shocking.getAxisBox() == 2) S = S + " Y";
        if(shocking.getAxisBox() == 1) S = S + " Z";
        axisDisp.setText(S);

        //int[] accelData = shocking.pullData();
        float[] accelData = getIntent().getFloatArrayExtra("DATA");

        Log.d("DETAIL", "Total data size is " + accelData.length);
        Log.d("DETAIL", "axisConfig is " + shocking.getAxisBox());

        int[] x = new int[33];

        for(int i = 0; i < accelData.length; i++) {
            x[i] = 20 + i * 13;
            double time = accelData[i] * 0.78;
            accelData[i] = (float) time;

            drawMe(this, x[i], yOffset - accelData[i], radius);

            if((i&1)!=0) {
                drawLines(this, x[i-1], x[i], yOffset-accelData[i-1], yOffset-accelData[i], Color.RED);
            }
            else if(i>0) drawLines(this, x[i-1], x[i], yOffset-accelData[i-1], yOffset-accelData[i], Color.RED);
        }
        drawLines(this, 20, 450, yOffset-shocking.getsThreshold(), yOffset-shocking.getsThreshold(), Color.CYAN);
        // Horizontal Bar
        drawLines(this, 20, 450, yOffset, yOffset, Color.BLUE);
        // Vertical Bar
        drawLines(this, 20, 20, 500, yOffset+100, Color.BLUE);
    }

    private void drawLines(Context context, float x1, float y1, float x2, float y2, int color) {
        View liner = new Lines(this, x1, y1, x2, y2, color);
        liner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addContentView(liner, new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void drawMe(Context context, float x, float y, int radius) {
        View circle = new Circle(this, x, y, radius);
        circle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addContentView(circle, new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void retBtnClick( View V ){
        //Intent i = new Intent(this, MainActivity.class);
        setResult(RESULT_OK);
        finish();
    }
}

