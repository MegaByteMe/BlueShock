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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class Circle extends View {
    private final float x, y;
    private final int radius;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Circle(Context context, float x, float y, int radius) {
        super(context);
        mPaint.setColor(Color.GREEN);
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(x, y, radius, mPaint);
    }
}
