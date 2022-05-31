package com.example.webrtcclientmodule;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class OnSwipeListener extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //return super.onFling(e1, e2, velocityX, velocityY);
        float x1=e1.getX();
        float y1=e1.getY();
        float x2=e2.getX();
        float y2=e2.getY();
        Direction direction=getDirection(x1,y1,x2,y2);
        return onSwipe(direction);
    }

    public boolean onSwipe(Direction direction){
        return false;
    }
    public Direction getDirection(float x1,float y1,float x2,float y2){
        double angle=getAngle(x1,y1,x2,y2);
        return Direction.fromAngle(angle);
    }
    public double getAngle(float x1,float y1,float x2,float y2){
        double rad=Math.atan2(y1-y2,x2-x1);
        return (rad*180/Math.PI+180)%360;
    }

    public enum Direction{
        left,
        right,
        up,
        down;

        public static Direction fromAngle(double angle){
            if(isInRange(angle,45,135)){
                return Direction.down;
            }else if(isInRange(angle,0,45)||isInRange(angle,315,360)){
                return Direction.left;
            }else if(isInRange(angle,225,315)){
                return Direction.up;
            }else{
                return Direction.right;
            }
        }
        private static boolean isInRange(double angle, float init, float end){
            return (angle>=init) && (angle<end);
        }

    }
}
