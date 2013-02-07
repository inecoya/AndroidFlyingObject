package jp.qee.necoyanagi.FlyingObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import jp.qee.necoyanagi.FlyingObject.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.os.Vibrator;

public class FlyingActivity extends Activity {

	// ビュー
	private MainView MainView;
	private FrameLayout frameLayout;
	Handler handler = new Handler();

	// 変数
	private float scale;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // アプリ名非表示
        scale = getResources().getDisplayMetrics().density;

        frameLayout = new FrameLayout(this.getApplicationContext());
        MainView = new MainView(this);
        frameLayout.addView(MainView);
        setContentView(frameLayout);

        // Timer の設定をする
        Timer timer = new Timer(false);
        timer.schedule(new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                    	MainView.invalidate();
                    }
                });
            }
        },0, 10);
    }

    public boolean onTouchEvent(MotionEvent event) {

    	// Viewを複数レイヤーにした場合、
    	// Viewではタッチイベントがとれないため、Activityから実行

    	MainView.onTouch(event);

    	return true;
    }

	@Override
    public void onDestroy() {
    	super.onDestroy();
	}


	/**
	 * ビュークラス
	 *
	 */
    public class MainView extends View {
    	// パラメータ
    	private static final int totalStage = 5;
    	private static final int limitMistake = 5;
    	private static final long limitTime = 10000;
    	private static final int minDisplayArea = 50;
    	private static final int minWallArea = 120;
    	private static final int moveSpeed = 8;
    	private static final float quizeImageSize = 200;
    	private static final float selectImageSize = 150;

    	// 画面レイアウト
    	private int deviceWidth = 0;
    	private int deviceHeight = 0;
    	private int centerX = 0;
    	private int centerY = 0;
    	private long time = 0;
    	private String packageName = "";

    	private boolean isPlay = false;
    	private boolean countDown = false;
    	private boolean gameOver = false;
    	private boolean gameClear = false;
    	private boolean timeOver = false;
    	private int stage = 1;
    	private int mistakeCount = 0;
    	private int collectCount = 0;
    	private int imageLocation = 0;
    	private int collectNum = 0;
    	private int collectIndex = 0;
    	private int maxDisplayArea = 0;
    	private int displayRange = 0;
    	private Bitmap gameImage;
    	private Bitmap[] quizImages;
    	private Bitmap wallImage;
    	private Rect[] rectQuizImages;

    	// 画像
    	private Bitmap backGround;

		/**
		 * コンストラクタ
		 *
		 */
		public MainView(Context context) {
			super(context);
			// TODO 自動生成されたコンストラクター・スタブ

            // ウインドウのサイズ
    		//setClickable(true);
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            deviceWidth = display.getWidth();
            deviceHeight = display.getHeight();
            centerX = deviceWidth / 2;
            centerY = deviceHeight / 2;
            maxDisplayArea = deviceWidth - minWallArea;
            displayRange = (maxDisplayArea - minDisplayArea) / totalStage;
            packageName = context.getPackageName();

            // レイアウト
            backGround = BitmapFactory.decodeResource(getResources(), R.drawable.background);
            wallImage = BitmapFactory.decodeResource(getResources(), R.drawable.wall);
            quizImages = new Bitmap[4];
            rectQuizImages = new Rect[4];

            collectNum = getRandom(10, 1);

            setImage(collectNum);
            setQuizImage();
		}

		/**
		 * クイズ画像セット
		 *
		 */
		private void setImage(int number) {
            int resId = getResources().getIdentifier("character" + String.valueOf(number), "drawable", packageName);
        	Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        	int orgWidth = bitmap.getWidth();
        	int orgHeight = bitmap.getHeight();

        	float width = quizeImageSize;
        	float height = quizeImageSize;

        	Matrix matrix = new Matrix();
        	matrix.postScale(width / orgWidth, height / orgHeight);

            gameImage = Bitmap.createBitmap(bitmap, 0, 0, orgWidth, orgHeight, matrix, true);
		}

		/**
		 * 選択肢セット
		 *
		 */
		private void setQuizImage() {
			ArrayList<Integer> usedNum = new ArrayList<Integer>();

			for (;;) {
				if (usedNum.size() >= 4) {
					if (usedNum.indexOf(collectNum) != -1) {
						break;
					} else {
						usedNum = new ArrayList<Integer>();
					}
				}

				for (int i=1; i<=4; i++) {

					Integer number = 0;

					for(;;) {
						number = this.getRandom(10, 1);

						if (usedNum.indexOf(number) == -1) {
							usedNum.add(number);
							break;
						}
					}

					if (collectNum == number) {
						collectIndex = i-1;
					}

		            int resId = getResources().getIdentifier("character" + String.valueOf(number), "drawable", packageName);
		        	Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
		        	int orgWidth = bitmap.getWidth();
		        	int orgHeight = bitmap.getHeight();

		        	float width = selectImageSize;
		        	float height = selectImageSize;

		        	Matrix matrix = new Matrix();
		        	matrix.postScale(width / orgWidth, height / orgHeight);

		        	quizImages[i-1] = Bitmap.createBitmap(bitmap, 0, 0, orgWidth, orgHeight, matrix, true);
				}
			}
		}

		/**
		 * 描画イベント
		 *
		 */
		protected void onDraw(Canvas canvas) {
			Paint paint = new Paint();
			Paint font = new Paint();

			paint.setAntiAlias(true);
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(35 * scale);

            font.setColor(Color.LTGRAY);
            font.setTextSize(18 * scale);

			canvas.drawBitmap(backGround, 0, 0, paint);


			if (gameClear) {
				canvas.drawText("GAME CLEAR!!", centerX - (110 * scale), centerY - (120 * scale), paint);
			} else {
				if (gameOver) {
					canvas.drawText("GAME OVER...", centerX - (110 * scale), centerY - (120 * scale), paint);
				} else {
					if (timeOver) {
						canvas.drawText("TIME OVER...", centerX - (110 * scale), centerY - (120 * scale), paint);
					} else {

						if (countDown) {
							String cnt = String.valueOf(((limitTime - (System.currentTimeMillis() - time)) / 1000) + 1);
							canvas.drawText(cnt, centerX - (paint.measureText(cnt)/2), ((370) * scale), font);

							if (System.currentTimeMillis() > time + limitTime) {
								timeOver = true;
							}
						}
						if (!isPlay && !countDown) {
							//canvas.drawBitmap(gameImage, centerX, centerY - (150 * scale), paint);
							if (stage == 1) {
								canvas.drawText("GAME START!!", centerX - (110 * scale), centerY - (120 * scale), paint);
							} else {
								canvas.drawText("NEXT STAGE", centerX - (105 * scale), centerY - (120 * scale), paint);
							}
						}
						if (isPlay && !countDown) {
							// クイズ画像
							int displayArea = maxDisplayArea - (displayRange * (stage - 1));
							canvas.drawBitmap(gameImage, deviceWidth - imageLocation, centerY - (180 * scale), paint);
							imageLocation += moveSpeed;

							// ウォール
							int sideArea = (deviceWidth - displayArea) / 2;
							canvas.drawBitmap(wallImage, sideArea - wallImage.getWidth(), 0 - (wallImage.getHeight() - backGround.getHeight()), paint);
							canvas.drawBitmap(wallImage, deviceWidth - sideArea, 0 - (wallImage.getHeight() - backGround.getHeight()), paint);
						}
						if (isPlay && countDown) {
							canvas.drawBitmap(quizImages[0], ((deviceWidth/4)*0), deviceHeight - (180 * scale), paint);
							canvas.drawBitmap(quizImages[1], ((deviceWidth/4)*1), deviceHeight - (180 * scale), paint);
							canvas.drawBitmap(quizImages[2], ((deviceWidth/4)*2), deviceHeight - (180 * scale), paint);
							canvas.drawBitmap(quizImages[3], ((deviceWidth/4)*3), deviceHeight - (180 * scale), paint);

							rectQuizImages[0] = new Rect((int) ((deviceWidth/4)*0), (int) (deviceHeight - (180 * scale)), (int) ((deviceWidth/4)*0)+quizImages[0].getWidth(), (int) (deviceHeight - (180 * scale))+quizImages[0].getHeight());
							rectQuizImages[1] = new Rect((int) ((deviceWidth/4)*1), (int) (deviceHeight - (180 * scale)), (int) ((deviceWidth/4)*1)+quizImages[1].getWidth(), (int) (deviceHeight - (180 * scale))+quizImages[1].getHeight());
							rectQuizImages[2] = new Rect((int) ((deviceWidth/4)*2), (int) (deviceHeight - (180 * scale)), (int) ((deviceWidth/4)*2)+quizImages[2].getWidth(), (int) (deviceHeight - (180 * scale))+quizImages[2].getHeight());
							rectQuizImages[3] = new Rect((int) ((deviceWidth/4)*3), (int) (deviceHeight - (180 * scale)), (int) ((deviceWidth/4)*3)+quizImages[3].getWidth(), (int) (deviceHeight - (180 * scale))+quizImages[3].getHeight());
						}
						if ((deviceWidth - imageLocation) < -500) {
							//Log.i("debug", "finish");
							imageLocation = 0;
							countDown = true;
							time = System.currentTimeMillis();
						}

					}
				}
			}

		}

		/**
		 * タッチイベント
		 *
		 */
		public boolean onTouch(MotionEvent e) {

			switch( e.getAction() & MotionEvent.ACTION_MASK){
				case MotionEvent.ACTION_UP:

					if (gameClear) {
						initGame();
					} else {
						if (gameOver) {
							initGame();
						} else {
							if (timeOver) {
								initGame();
							} else{

								if (!isPlay && !countDown) {
						            collectNum = getRandom(4, 1);
						            setImage(collectNum);
						            setQuizImage();
									isPlay = true;
								}

								if (isPlay && countDown) {
									for (int i=0; i<4; i++) {
										if (rectQuizImages[i].left <= e.getX() && e.getX() <= rectQuizImages[i].right) {
											if (rectQuizImages[i].top <= e.getY() && e.getY() <= rectQuizImages[i].bottom) {
												if (i == collectIndex) {
													collectCount += 1;
													stage += 1;

													Toast.makeText(getApplicationContext(), "○", Toast.LENGTH_SHORT).show();

													if (stage > totalStage) {
														gameClear = true;
													} else {
											            collectNum = getRandom(4, 1);
											            setImage(collectNum);
											            setQuizImage();
														isPlay = true;
														countDown = false;
														time = 0;
													}
												} else {
													mistakeCount += 1;

													Toast.makeText(getApplicationContext(), "×", Toast.LENGTH_SHORT).show();
													((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(100);

													if (limitMistake == mistakeCount) {
														gameOver = true;
													}
												}
											}
										}
									}
								}

							}
						}
					}

					break;
			}

			return true;
		}

		/**
		 * パラメータ初期化
		 *
		 */
		public void initGame() {
	    	isPlay = false;
	    	countDown = false;
	    	gameOver = false;
	    	gameClear = false;
	    	timeOver = false;
	    	stage = 1;
	    	mistakeCount = 0;
	    	collectCount = 0;
	    	imageLocation = 0;
	    	time = 0;
		}

		/**
		 * ランダム値を返す
		 *
		 * @param max
		 * @param min
		 * @return
		 */
		private int getRandom(int max, int min) {
			return (int)Math.floor(Math.random() * (max - min + 1)) + min;
	    }

    }
}