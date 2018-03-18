package com.ui_enhancer.screen_lock;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DeviceAdmin extends DeviceAdminReceiver {

	@Override
	public void onEnabled(Context context, Intent intent) {
		// TODO Auto-generated method stub
		super.onEnabled(context, intent);
		Toast.makeText(
				context,
				"Device Admin added.\nGoto Settings > Security > Device Administrators to enable / disable",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public CharSequence onDisableRequested(Context context, Intent intent) {
		// TODO Auto-generated method stub
		return "Denying ADMISTRATIVE PRIVILEGES would not allow DOUBLE TAP TO LOCK functionality. Continue?";
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		// TODO Auto-generated method stub
		super.onDisabled(context, intent);
		Toast.makeText(
				context,
				"ADMINISTRATIVE PRIVILEGES denied. DOUBLE TAP TO LOCK disabled.",
				Toast.LENGTH_SHORT).show();
	}

}