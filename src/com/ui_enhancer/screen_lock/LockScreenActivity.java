package com.ui_enhancer.screen_lock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class LockScreenActivity extends Activity {

	private DevicePolicyManager	devicePolicyManager;
	private KeyguardLock		keyguardLock;
	private ComponentName		componentName;
	private static final String	LOG_TAG	= "LockScreenActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		Log.i(LOG_TAG, "LockSreen Activity : onCreate()");
		createObjects();

		if (!checkAndGetAdminPermissions()) {
			lockScreen();
			finishActivity();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		try {
			if (resultCode == -1) {
				devicePolicyManager.lockNow();
			}
			finishActivity();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}

	private void createObjects() {
		try {
			Log.i(LOG_TAG, "Creating Objects");
			devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
			keyguardLock = ((KeyguardManager) getSystemService(KEYGUARD_SERVICE))
					.newKeyguardLock(getPackageName());
			componentName = new ComponentName(this,
					DeviceAdmin.class);
			Log.i(LOG_TAG, "Objects Created");
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}

	private boolean checkAndGetAdminPermissions() {
		Intent intent;

		if (!devicePolicyManager.isAdminActive(componentName)) {
			intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					componentName);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					"ADMINISTRSTOR PERMISSIONS required to enable DOUBLE TAP TO LOCK");
			startActivityForResult(intent, 1);
			return true;
		} else {
			return false;
		}
	}

	private void lockScreen() {
		try {
			if (devicePolicyManager.isAdminActive(componentName)) {
				Log.i(LOG_TAG, "Admin active, attempting to lock");
				if (keyguardLock != null) {
					keyguardLock.disableKeyguard();
				}

				devicePolicyManager.lockNow();

				if (keyguardLock != null) {
					keyguardLock.reenableKeyguard();
				}
			} else {
				Log.i(LOG_TAG, "Admin not active, cannot lock");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}

	private void finishActivity() {
		for (;;) {
			Log.i(LOG_TAG, "Trying to finish LockScreenActivity");
			finish();
			return;
		}
	}

}
