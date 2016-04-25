/*
Low Power Wireless Shock Detection System
Developed by Team7

Codename: BlueShock
Revision:1
Change:1

Notes:
    Ideas for improved appearance
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);

*/

package team7.blueshock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class Lines extends View {
    private final float x1, x2, y1, y2;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Lines(Context context, float x1, float x2, float y1, float y2, int color) {
        super(context);
        mPaint.setColor(color);
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLine(x1, y1, x2, y2, mPaint);
    }
}
