package com.ui_enhancer.services;

import com.ui_enhancer.activities.MasterActivity;
import com.y2k.uienhancer.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class OverlayService extends Service {

	private LinearLayout		overView;
	public static int			brightness;
	private WindowManager		windowManager;
	public static boolean		isRunning					= false;
	private static final int	seek_max_brightness_int		= 255;
	private static final float	seek_max_brightness_float	= 255.0f;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		try {
			brightness = intent.getExtras().getInt("brightness");
			Log.d("OverlayService", "Bundle retrieved");
		} catch (Exception exception) {
			brightness = seek_max_brightness_int / 2;
			Log.d("OverlayService", exception.toString());
		}

		overView = new LinearLayout(this);
		overView.setBackgroundColor(0xFF000000); // The black color

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, Gravity.START,
				Gravity.TOP, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 0
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
				PixelFormat.TRANSLUCENT);
		params.alpha = ((seek_max_brightness_int - brightness) / seek_max_brightness_float);
		Log.d("OverlayService", "alpha = " + params.alpha);

		// Turn off soft-key buttons backlight
		try {
			WindowManager.LayoutParams.class.getField("buttonBrightness").set(
					params, Integer.valueOf(0));
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// create screen filter
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		windowManager.addView(overView, params);
		Log.d("OverlayService", "View Overlayed");

		// update flags and create notification for foregroundService
		isRunning = true;
		makeForeground();

		return super.onStartCommand(intent, flags, startId);
	}

	private void makeForeground() {
		// TODO Auto-generated method stub
		Notification notification = new Notification(R.drawable.ic_filter,
				"Brightness Filter Enabled", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MasterActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "Brightness Filter",
				"Current Setting: " + brightness, pendingIntent);
		startForeground(786420, notification);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (overView != null) {
			windowManager.removeView(overView);
			Log.d("OverlayService", "overLay removed");
		} else {
			Log.d("OverlayService", "overlay already removed");
		}

		isRunning = false;
	}
}
