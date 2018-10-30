package com.google.sample.eddystonevalidator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by ymc12 on 2018-04-27.
 */

public class PositionCanvas extends View{
    ArrayList<PositionSet> positionSetList;
    float cx, cy;
    float x, y;
    String name;
    public PositionCanvas(Context context){
        super(context);
    }
    public PositionCanvas(Context context, AttributeSet attr){
        super(context, attr);
    }
    @Override
    protected void onDraw(Canvas canvas){
        cx = canvas.getWidth();
        cy = canvas.getHeight();
        /*
        if(positionSetList==null)
            positionSetList = new ArrayList<>();

        Paint paint_d = new Paint();
        paint_d.setColor(Color.BLACK);
        paint_d.setTextSize(30);

        if(positionSetList.size()>0) {
            for (int i = 0; i < positionSetList.size(); i++) {
                String name = positionSetList.get(i).getName();
                float XLength = (float)positionSetList.get(i).getPosX()*canvas.getWidth()/800;
                float YLength = (float)positionSetList.get(i).getPosY()*canvas.getHeight()/800;
                canvas.drawText(name, XLength, YLength, paint_d);
            }
        }
        */
        if(name != null) {
            Paint paint_d = new Paint();
            paint_d.setColor(Color.BLACK);
            paint_d.setTextSize(30);

            canvas.drawText(name, x, y, paint_d);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP){
            x = event.getX();
            y = event.getY();
            name = "sibal";
            invalidate();
        }
        return true;
    }

    protected void setPosition(ArrayList<PositionSet> positionset){
        positionSetList = positionset;
        invalidate();
    }
    protected void setSoloPosition(PositionSet set){
        x = (float)set.getPosX();
        y = (float)set.getPosY();
        name = set.getName();
        invalidate();
    }
    public float getX(){
        return this.x*800/cx;
    }
    public float getY(){
        return this.y*800/cy;
    }
}
