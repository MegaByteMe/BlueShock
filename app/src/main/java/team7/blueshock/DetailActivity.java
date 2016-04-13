package team7.blueshock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;

public class DetailActivity extends AppCompatActivity {
    CustomDrawableView mCustomDrawableView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mCustomDrawableView = new CustomDrawableView(this);
        //setContentView(mCustomDrawableView);

        ShapeDrawable myDrawable;
        int x = 5;
        int y = 5;
        int width = 10;
        int height = 10;

        myDrawable = new ShapeDrawable(new OvalShape());
        myDrawable.getPaint().setColor(0xff74ac23);
        myDrawable.setBounds(x, y, x + width, y + height);

        Canvas grid = new Canvas(Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888));
        grid.drawColor(Color.BLUE);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        grid.drawCircle(10/2, 10/2, 10/2, paint);
        grid.drawText("Hello world", 25, 25, paint);
        grid.save();
    }

    public void retBtnClick( View V ){
        finish();
    }

    public class CustomDrawableView extends View {
        private ShapeDrawable mDrawable;

        public CustomDrawableView(Context con) {
            super(con);

            int x = 5;
            int y = 5;
            int width = 10;
            int height = 10;

            mDrawable = new ShapeDrawable(new OvalShape());
            mDrawable.getPaint().setColor(0xff74ac23);
            mDrawable.setBounds(x, y, x + width, y + height);
        }

        @Override
        protected void onDraw(Canvas can) {
            mDrawable.draw(can);


        }
    }
}
