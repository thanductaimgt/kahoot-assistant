package com.mgt.quiz_assistant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawView extends View {

    Point[] points = new Point[4];

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    int groupId = -1;
    public ArrayList<ColorBall> colorballs = new ArrayList<>();
    // array that holds the balls
    private int balID = 0;
    // variable to know what ball is being dragged
    Paint paint;
    Canvas canvas;
    private Rect rect = new Rect();
    private int touchToLeft = 0;
    private int touchToTop = 0;
    private int touchToRight = 0;
    private int touchToBottom = 0;
    private boolean isSelect = false;

    public DrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    public void setRect(Rect rect) {
        points[0] = new Point();
        points[1] = new Point();
        points[2] = new Point();
        points[3] = new Point();

        points[0].x = rect.left;
        points[0].y = rect.top;
        points[1].x = rect.left;
        points[1].y = rect.bottom;
        points[2].x = rect.right;
        points[2].y = rect.bottom;
        points[3].x = rect.right;
        points[3].y = rect.top;

        for (Point pt : points) {
            colorballs.add(new ColorBall(getContext(), android.R.drawable.ic_menu_more, pt));
        }
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if (points[3] == null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = Math.min(left, points[i].x);
            top = Math.min(top, points[i].y);
            right = Math.max(right, points[i].x);
            bottom = Math.max(bottom, points[i].y);
        }
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5);

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#AADB1255"));
        paint.setStrokeWidth(2);
        canvas.drawRect(
                left,
                top,
                right,
                bottom, paint);
        //fill the rectangle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#50ffffff"));
        paint.setStrokeWidth(0);
        canvas.drawRect(
                left,
                top,
                right,
                bottom, paint);

        //draw the corners
//        BitmapDrawable bitmap = new BitmapDrawable();
        // draw the balls on the canvas
        paint.setColor(Color.BLUE);
        paint.setTextSize(18);
        paint.setStrokeWidth(0);
        for (int i = 0; i < colorballs.size(); i++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX() - ball.getWidthOfBall() / 2, ball.getY() - ball.getHeightOfBall() / 2,
                    paint);

//            canvas.drawText("" + (i + 1), ball.getX(), ball.getY(), paint);
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) Math.min(Math.max(event.getX(), 0), getWidth());
        int Y = (int) Math.min(Math.max(event.getY(), 0), getHeight());

        switch (eventaction) {
            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                //resize rectangle
                balID = -1;
                groupId = -1;
                boolean isTouchBall = false;
                for (int i = colorballs.size() - 1; i >= 0; i--) {
                    ColorBall ball = colorballs.get(i);
                    // check if inside the bounds of the ball (circle)
                    // get the center for the ball
                    int centerX = ball.getX();
                    int centerY = ball.getY();
                    paint.setColor(Color.CYAN);
                    // calculate the radius from the touch to the center of the
                    // ball
                    double radCircle = Math
                            .sqrt(((centerX - X) * (centerX - X)) + (centerY - Y)
                                    * (centerY - Y));

                    if (radCircle < ball.getWidthOfBall()) {
                        balID = ball.getID();
                        if (balID == 1 || balID == 3) {
                            groupId = 2;
                        } else {
                            groupId = 1;
                        }
                        invalidate();
                        isTouchBall = true;
                        break;
                    }
                    invalidate();
                }
                rect.left = points[0].x;
                rect.right = points[3].x;
                rect.top = points[0].y;
                rect.bottom = points[1].y;
                rect.sort();
                touchToLeft = X - rect.left;
                touchToTop = Y - rect.top;
                touchToRight = rect.right - X;
                touchToBottom = rect.bottom - Y;
                isSelect = rect.contains(X, Y);

                if (isTouchBall || isSelect) {
                    if (listener != null) {
                        listener.onStartModify();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball
                if (balID > -1) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);

                    paint.setColor(Color.CYAN);
                    if (groupId == 1) {
                        colorballs.get(1).setX(colorballs.get(0).getX());
                        colorballs.get(1).setY(colorballs.get(2).getY());
                        colorballs.get(3).setX(colorballs.get(2).getX());
                        colorballs.get(3).setY(colorballs.get(0).getY());
                    } else {
                        colorballs.get(0).setX(colorballs.get(1).getX());
                        colorballs.get(0).setY(colorballs.get(3).getY());
                        colorballs.get(2).setX(colorballs.get(3).getX());
                        colorballs.get(2).setY(colorballs.get(1).getY());
                    }

                    invalidate();
                } else if (isSelect) {
                    int newX = Math.min(getWidth() - touchToRight, Math.max(touchToLeft, X));
                    int newY = Math.min(getHeight() - touchToBottom, Math.max(touchToTop, Y));

                    colorballs.get(0).setX(newX - touchToLeft);
                    colorballs.get(0).setY(newY - touchToTop);
                    colorballs.get(1).setX(newX - touchToLeft);
                    colorballs.get(1).setY(newY + touchToBottom);
                    colorballs.get(2).setX(newX + touchToRight);
                    colorballs.get(2).setY(newY + touchToBottom);
                    colorballs.get(3).setX(newX + touchToRight);
                    colorballs.get(3).setY(newY - touchToTop);

                    rect.left = points[0].x;
                    rect.right = points[3].x;
                    rect.top = points[0].y;
                    rect.bottom = points[1].y;
                    touchToLeft = X - rect.left;
                    touchToTop = Y - rect.top;
                    touchToRight = rect.right - X;
                    touchToBottom = rect.bottom - Y;
                }

                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping
                if (listener != null) {
                    listener.onStopModify();
                }
                break;
        }
        // redraw the canvas
        invalidate();
        return true;

    }

    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;
        static int count = 0;

        public ColorBall(Context context, int resourceId, Point point) {
            this.id = count++;
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    resourceId);
            mContext = context;
            this.point = point;
        }

        public int getWidthOfBall() {
            return bitmap.getWidth();
        }

        public int getHeightOfBall() {
            return bitmap.getHeight();
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public int getID() {
            return id;
        }

        public void setX(int x) {
            point.x = x;
        }

        public void setY(int y) {
            point.y = y;
        }
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onStartModify();

        void onStopModify();
    }
}