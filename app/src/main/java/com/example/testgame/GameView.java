package com.example.testgame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Random;

public class GameView extends View {

    int dWidth, dHeight;
    Bitmap trash, hand, plastic, background, heartImage;
    Handler handler;
    Runnable runnable;

    long UPDATE_MILLIS = 30;
    int handX, handY;
    int plasticX, plasticY;
    Random random;
    boolean plasticAnimation = false;
    int points = 0;
    float TEXT_SIZE = 50;
    Paint textPaint;
    int life = 4;
    Context context;
    int handSpeed;
    int trashX, trashY;
    MediaPlayer mpPoint, mpWhoosh, mpPop;

    // SharedPreferences for storing high score
    SharedPreferences sharedPreferences;
    int highScore;

    public GameView(Context context) {
        super(context);
        this.context = context;
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dWidth = size.x;
        dHeight = size.y;

        // Initialize SharedPreferences to store high score
        sharedPreferences = context.getSharedPreferences("GamePreferences", Context.MODE_PRIVATE);
        highScore = sharedPreferences.getInt("highScore", 0);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        background = Bitmap.createScaledBitmap(background, dWidth, dHeight, false);

        heartImage = BitmapFactory.decodeResource(getResources(), R.drawable.heart);
        heartImage = Bitmap.createScaledBitmap(heartImage, 100, 100, false);

        trash = BitmapFactory.decodeResource(getResources(), R.drawable.trash);
        hand = BitmapFactory.decodeResource(getResources(), R.drawable.hand);
        plastic = BitmapFactory.decodeResource(getResources(), R.drawable.plastic);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        };
        random = new Random();
        handX = dWidth + random.nextInt(300);
        handY = random.nextInt(600);
        plasticX = handX;
        plasticY = handY + hand.getHeight() - 30;
        textPaint = new Paint();
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(0xFFFFFFFF); // Set score text color to white
        handSpeed = 21 + random.nextInt(10);
        trashX = dWidth / 2 - trash.getWidth() / 2;
        trashY = dHeight - trash.getHeight();
        mpPoint = MediaPlayer.create(context, R.raw.point);
        mpWhoosh = MediaPlayer.create(context, R.raw.whoosh);
        mpPop = MediaPlayer.create(context, R.raw.pop);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(background, 0, 0, null);

        if (!plasticAnimation) {
            handX -= handSpeed;
            plasticX -= handSpeed;
        }
        if (handX <= -hand.getWidth()) {
            if (mpWhoosh != null) {
                mpWhoosh.start();
            }
            handX = dWidth + random.nextInt(300);
            plasticX = handX;
            handY = random.nextInt(600);
            plasticY = handY + hand.getHeight() - 30;
            handSpeed = 21 + random.nextInt(10);
            trashX = hand.getWidth() + random.nextInt(dWidth - 2 * hand.getWidth());
            life--;
            if (life == 0) {
                // Update the high score when the game ends
                updateHighScore();  // Update the high score before navigating
                handler.post(() -> {
                    Intent intent = new Intent(context, GameOver.class);
                    intent.putExtra("points", points);
                    context.startActivity(intent);
                    ((Activity) context).finish();
                });
            }
        }
        if (plasticAnimation) {
            plasticY += 40;
        }
        if (plasticAnimation && (plasticX + plastic.getWidth() >= trashX) && (plasticX <= trashX + trash.getWidth()) && (plasticY + plastic.getHeight() >= (dHeight - trash.getHeight())) && plasticY <= dHeight) {
            if (mpPoint != null) {
                mpPoint.start();
            }
            handX = dWidth + random.nextInt(300);
            plasticX = handX;
            handY = random.nextInt(600);
            plasticY = handY + hand.getHeight() - 30;
            handSpeed = 21 + random.nextInt(10);
            points++;
            trashX = hand.getWidth() + random.nextInt(dWidth - 2 * hand.getWidth());
            plasticAnimation = false;
        }
        if (plasticAnimation && (plasticY + plastic.getHeight()) >= dHeight) {
            if (mpPop != null) {
                mpPop.start();
            }
            life--;
            if (life == 0) {
                // Update the high score when the game ends
                updateHighScore();  // Update the high score before navigating
                Intent intent = new Intent(context, GameOver.class);
                intent.putExtra("points", points);
                context.startActivity(intent);
                ((Activity) context).finish();
            }
            handX = dWidth + random.nextInt(300);
            plasticX = handX;
            handY = random.nextInt(600);
            plasticY = handY + hand.getHeight() - 30;
            trashX = hand.getWidth() + random.nextInt(dWidth - 2 * hand.getWidth());
            plasticAnimation = false;
        }
        canvas.drawBitmap(trash, trashX, trashY, null);
        canvas.drawBitmap(hand, handX, handY, null);
        canvas.drawBitmap(plastic, plasticX, plasticY, null);
        canvas.drawText("Score: " + points, 20, TEXT_SIZE, textPaint);
        canvas.drawText("High Score: " + highScore, 20, TEXT_SIZE * 2, textPaint); // Position the high score below current score

        for (int i = 0; i < life; i++) {
            canvas.drawBitmap(heartImage, dWidth - 120 - (i * 90), 30, null); // Closer spacing for hearts
        }

        if (life != 0)
            handler.postDelayed(runnable, UPDATE_MILLIS);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!plasticAnimation && (touchX >= handX && touchX <= (handX + hand.getWidth()) && touchY >= handY && touchY <= (handY + hand.getHeight()))) {
                plasticAnimation = true;
            }
        }
        return true;
    }

    // Method to update the high score if needed
    public void updateHighScore() {
        if (points > highScore) {
            highScore = points;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("highScore", highScore);
            editor.apply();
        }
    }
}
