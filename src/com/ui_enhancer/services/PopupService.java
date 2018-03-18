package com.ui_enhancer.services;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ui_enhancer.activities.MasterActivity;
import com.ui_enhancer.screen_lock.LockScreenActivity;
import com.y2k.uienhancer.R;

public class PopupService extends Service implements OnTouchListener {

	private class BatteryLevelChangeReceiver extends BroadcastReceiver {

		private int	level;

		@Override
		public void onReceive(Context context, Intent intent_masterActivity) {
			// TODO Auto-generated method stub

			try {
				level = intent_masterActivity.getIntExtra(
						BatteryManager.EXTRA_LEVEL, 0);
			} catch (Exception e) {
				level = 0;
				Log.d("BoradcastReciever", e.toString());
			} finally {
				textView.setText(level + "%");
			}
		}

	}

	private class DelayProducer extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(MAX_TAP_DELAY);
			} catch (InterruptedException e) {
				Log.d("run()", e.toString());
			} catch (Exception e) {
				Log.d("run()", e.toString());
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			if (isSingleTap) {
				Log.d("Thread", "----THREAD "
						+ Thread.currentThread().getName()
						+ "  EXPIRED---- Performing Single Click".toUpperCase());
				performSingleClick();
			} else {
				Log.d("Thread", "----THREAD "
						+ Thread.currentThread().getName()
						+ "  EXPIRED---- No click performed".toUpperCase());
			}

			isFirstTap = true;
			isSingleTap = true;
			// logInfo("after thread finish");
		}
	}

	private class ViewOverlayingDelayProducer extends
			AsyncTask<Void, Void, Void> {

		private boolean	dummyViewAdded;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			dummyViewAdded = toggleDummyView(true);
			if (dummyViewAdded) {
				Log.i("viewOverlay",
						"dummyView wasn't present, has just been added");
			} else {
				Log.i("viewOverlay", "dummyView not added, already present");
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if (dummyViewAdded) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.d("DelayProducer", e.toString());
				} catch (Exception e) {
					// TODO: handle exception
					Log.d("DelayProducer", e.toString());
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			try {
				showStatusBar.invoke(statusBarService);
				Log.i("viewOverlay", "statusBar has been pulled");
			} catch (Exception exception) {
				Log.e("pullStatusBar()", exception.toString());
			}
		}
	}

	// objects for creating popup
	private WindowManager				windowManager;
	private WindowManager.LayoutParams	layoutParams;

	// objects for pulling down status bar
	private Object						statusBarService;
	private Method						showStatusBar;
	private boolean						dummyViewPresent;
	private WindowManager.LayoutParams	layouParams_dummyView;
	private LinearLayout				dummyView;
	private Intent						intent_masterActivity;

	// objects to change popup background upon tap
	private ImageView					imageView;
	private Bitmap						bitmap_normal;
	private Bitmap						bitmap_pressed;

	// objects for showing battery level
	private TextView					textView;
	private BatteryLevelChangeReceiver	levelChangeReceiver;

	// objects for enabling movement of popup
	private int							initialX;
	private int							initialY;
	private float						initialTouchX;
	private float						initialTouchY;

	private static final String			LOG_TAG					= "PopupService";
	private View						popup;
	public static boolean				isRunning				= false;

	// objects for enabling Double-Tap-To-Lock
	private Intent						intent_lockScreen;
	// private DevicePolicyManager devicePolicyManager;
	// private ComponentName componentName;

	// objects for handling touch events of popup
	private boolean						isFirstTap;
	private boolean						isSingleTap;
	private boolean						move;
	private boolean						doubleClickSecondTapDown;
	private boolean						doubleClickSecondTapUp;
	private long						downTime;
	private final int					MAX_CLICK_MOVE_DISTANCE	= 30;
	private final long					MAX_TAP_DELAY			= 200;
	private final long					MAX_CLICK_DURATION		= 400;

	@Override
	public IBinder onBind(Intent intent_masterActivity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent_masterActivity, int flags,
			int startId) {
		// TODO Auto-generated method stub

		makeForeground();
		createPopup();
		createObjects();
		createStatusBarPuller();
		addListeners();

		return super.onStartCommand(intent_masterActivity, flags, startId);
	}

	private void makeForeground() {
		// TODO Auto-generated method stub
		Notification notification = new Notification(R.drawable.ic_popup,
				"UI Assist Enabled", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MasterActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),
				"Current Setting: Enabled", pendingIntent);
		startForeground(420786, notification);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (popup != null) {
			try {
				windowManager.removeView(popup);
				toggleDummyView(false);
			} catch (Exception e) {
				Log.d("onDestroy()", e.toString());
			}
		}

		isRunning = false;
		this.unregisterReceiver(levelChangeReceiver);
	}

	private void addListeners() {
		// TODO Auto-generated method stub
		popup.setOnTouchListener(this);
		this.registerReceiver(this.levelChangeReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
	}

	private void createPopup() {
		LayoutInflater layoutInflater;

		layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		popup = layoutInflater.inflate(R.layout.popup_head, null);
		textView = (TextView) popup.findViewById(R.id.popup_textView_battery);
		imageView = (ImageView) popup.findViewById(R.id.popup_imageView);

		layoutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.CENTER | Gravity.CENTER_VERTICAL
				| Gravity.RIGHT | Gravity.END;

		windowManager.addView(popup, layoutParams);
	}

	private void createObjects() {
		isRunning = true;
		isFirstTap = true;
		isSingleTap = true;
		doubleClickSecondTapDown = false;
		doubleClickSecondTapUp = false;
		move = false;

		dummyViewPresent = false;
		levelChangeReceiver = new BatteryLevelChangeReceiver();

		bitmap_normal = BitmapFactory.decodeResource(getResources(),
				R.drawable.popup_background_normal);
		bitmap_pressed = BitmapFactory.decodeResource(getResources(),
				R.drawable.popup_background_pressed);
		// drawable_normal = getDrawable(R.drawable.popup_background_normal);
		// drawable_pressed = getDrawable(R.drawable.popup_background_pressed);

		intent_masterActivity = new Intent(getApplicationContext(),
				MasterActivity.class);
		intent_masterActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		try {
			intent_lockScreen = new Intent(getApplicationContext(),
					LockScreenActivity.class);
			intent_lockScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			// devicePolicyManager = (DevicePolicyManager)
			// getSystemService(Context.DEVICE_POLICY_SERVICE);
			// componentName = new ComponentName(getApplicationContext(),
			// DeviceAdmin.class);
		} catch (Exception e) {
			Log.e("deviceLock", e.toString());
		}
	}

	private void createStatusBarPuller() {
		Class<?> statusBarManager;

		statusBarManager = null;
		statusBarService = getSystemService("statusbar");
		showStatusBar = null;
		dummyViewPresent = false;

		try {
			statusBarManager = Class.forName("android.app.StatusBarManager");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			Log.d("createStatusBar()", e1.toString());
		}

		try {
			showStatusBar = statusBarManager
					.getMethod("expandNotificationsPanel");
		} catch (NoSuchMethodException exception) {
			try {
				showStatusBar = statusBarManager.getMethod("expand");
				Log.d("createStatusBar()", exception.toString());
			} catch (Exception e) {
				// TODO: handle exception
				Log.d("createStatusBar()", e.toString());
			}
		} catch (Exception e) {
			// TODO: handle exception
			Log.d("createStatusBar()", e.toString());
		}

		// Create dummy view to force show Status Bar in fullScreen apps
		dummyView = new LinearLayout(this);

		layouParams_dummyView = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, Gravity.START,
				Gravity.TOP, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 0
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
				PixelFormat.TRANSPARENT
						| WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		layouParams_dummyView.flags = 0
				| WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
				| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

		dummyView.setAlpha(0);
		layouParams_dummyView.alpha = 0.0f;
	}

	private void logInfo(String string) {
		Log.d("onTouch()", string.toUpperCase());
		Log.d("onTouch()", "isFirstTap  = " + isFirstTap + "\nisSingleTap = "
				+ isSingleTap);
	}

	private void logSeperator() {
		Log.d("onTouch()", "===================");
	}

	private void performSingleClick() {
		Log.d(LOG_TAG, "----Single Click Performed----".toUpperCase());
		// Toast.makeText(getApplicationContext(), "Single Click",
		// Toast.LENGTH_SHORT).show();
		pullStatusBar();
	}

	private void performDoubleClick() {
		Log.d(LOG_TAG, "----Double Click Performed----".toUpperCase());
		// Toast.makeText(getApplicationContext(), "Double Click",
		// Toast.LENGTH_SHORT).show();
		/*
		 * try { if (devicePolicyManager.isAdminActive(componentName)) {
		 * devicePolicyManager.lockNow(); Log.i("doubleClick",
		 * "Device Lock performed"); } else { Log.i("doubleClick",
		 * "Device Lock not performed, Permission denied"); } } catch (Exception
		 * e) { Log.e("doubleClick", e.toString()); }
		 */
		try {
			Log.i(LOG_TAG, "Attempting to start LockScreenActivity");
			startActivity(intent_lockScreen);
			Log.i(LOG_TAG, "LockScreenActivity started");
		} catch (Exception e) {
			Log.e(LOG_TAG,
					"Error in starting LockSreenActivity: " + e.toString());
		}

		Log.i(LOG_TAG, "Double Click completed");
	}

	private void performLongClick() {
		Log.d(LOG_TAG, "----Long Click Performed----".toUpperCase());
		// Toast.makeText(getApplicationContext(), "Long Click",
		// Toast.LENGTH_SHORT).show();

		toggleDummyView(false);
		startActivity(intent_masterActivity);
	}

	private void pullStatusBar() {
		(new ViewOverlayingDelayProducer()).execute((Void[]) null);
		/*
		 * toggleDummyView(true);
		 * 
		 * try { showStatusBar.invoke(statusBarService); } catch (Exception
		 * exception) { Log.d("pullStatusBar()", exception.toString()); }
		 */
	}

	private void toggleDummyView() {
		if (dummyViewPresent) {
			windowManager.removeView(dummyView);
			dummyViewPresent = false;
			Log.d(LOG_TAG, "Dummy View removed");
		} else {
			windowManager.addView(dummyView, layouParams_dummyView);
			dummyViewPresent = true;
			Log.d(LOG_TAG, "Dummy View added");
		}
	}

	private boolean toggleDummyView(boolean makeAvailable) {
		if (makeAvailable && !dummyViewPresent) {
			windowManager.addView(dummyView, layouParams_dummyView);
			Log.i(LOG_TAG, "Dummy View added");
			dummyViewPresent = true;
			return true;
		} else if (!makeAvailable && dummyViewPresent) {
			windowManager.removeView(dummyView);
			dummyViewPresent = false;
			Log.i(LOG_TAG, "Dummy View removed");
			return false;
		}
		Log.i(LOG_TAG, "dummyView neither added nor removed");
		return false;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub

		// logInfo("Before action\n");

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				move = false;
				imageView.setImageBitmap(bitmap_pressed);
				if (isFirstTap) {
					// get down time to filter longClick event
					downTime = System.currentTimeMillis();

					// update flags filter doubleTap events
					isFirstTap = true; // redundant
					isSingleTap = true;
					doubleClickSecondTapDown = false;
					doubleClickSecondTapUp = false;

					// get Values for motion event
					initialX = layoutParams.x;
					initialY = layoutParams.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
				} else {
					isFirstTap = false; // redundant
					isSingleTap = false;
					doubleClickSecondTapDown = true;
					doubleClickSecondTapUp = false;
					performDoubleClick();
				}
				// logInfo("after action down");
				// logSeperator();
				return true;

			case MotionEvent.ACTION_UP:
				imageView.setImageBitmap(bitmap_normal);
				if (!move) {
					if (isFirstTap) {
						if ((System.currentTimeMillis() - downTime) >= MAX_CLICK_DURATION) {
							if ((doubleClickSecondTapDown && doubleClickSecondTapUp)
									|| (!doubleClickSecondTapDown)) {
								// perform longClick event
								isSingleTap = true;
								isFirstTap = true;
								performLongClick();
							}
						} else {
							if ((doubleClickSecondTapDown && doubleClickSecondTapUp)
									|| (!doubleClickSecondTapDown)) {
								// update flags to indicate this is first tap
								// and another tap maybe possible, start a
								// Thread
								isFirstTap = false;
								(new DelayProducer()).execute((Void[]) null);
								/*
								 * thread = new Thread(new Runnable() {
								 * 
								 * @Override public void run() { // TODO
								 * Auto-generated method stub try {
								 * Thread.sleep(MAX_TAP_DELAY); } catch
								 * (InterruptedException e) { Log.d("run()",
								 * e.toString()); } catch (Exception e) {
								 * Log.d("run()", e.toString()); } finally { if
								 * (isSingleTap) { Log.d("Thread", "----THREAD "
								 * + Thread.currentThread() .getName() +
								 * "  EXPIRED---- Performing Single Click"
								 * .toUpperCase()); performSingleClick(); } else
								 * { Log.d("Thread", "----THREAD " +
								 * Thread.currentThread() .getName() +
								 * "  EXPIRED---- No click performed"
								 * .toUpperCase()); } isFirstTap = true;
								 * isSingleTap = true;
								 * logInfo("after thread finish"); } } });
								 * thread.start();
								 */
							}
						}
					} else {
						// isSingleTap = true; // redundant
						// isFirstTap = true; // updated to ensure correct
						// behavior
						// of subsequent clicks
						doubleClickSecondTapUp = true;
					}
					// logInfo("after action up");
					// logSeperator();
					return true;
				}

			case MotionEvent.ACTION_MOVE:
				// update move flag
				move = true;

				// logSeperator();
				// Log.d("Move", "x = " + event.getX());
				// Log.d("Move", "y = " + event.getY());
				// Log.d("Move", "rawX = " + event.getRawX());
				// Log.d("Move", "rawY = " + event.getRawY());
				// logSeperator();

				if ((event.getX() <= MAX_CLICK_MOVE_DISTANCE)
						&& (event.getY() <= MAX_CLICK_MOVE_DISTANCE)) {
					move = false;
				}

				// move the popup
				layoutParams.x = initialX
						- (int) (event.getRawX() - initialTouchX);
				layoutParams.y = initialY
						+ (int) (event.getRawY() - initialTouchY);
				windowManager.updateViewLayout(popup, layoutParams);

				// logInfo("after action move");
				// logSeperator();
				return true;
		}

		return false;
	}

}
