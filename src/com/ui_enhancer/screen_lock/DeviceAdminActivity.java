package com.ui_enhancer.screen_lock;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.y2k.uienhancer.R;

public class DeviceAdminActivity extends Activity implements OnClickListener {

 
	private Button				button_allow;
	private Button				button_deny;
	private static final int	RESULT_ENABLE	= 1;
	private DevicePolicyManager	devicePolicyManager;
	private ComponentName		componentName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_admin);

		createObjects();
		mapViews();
		addListeners();
	}

	private void createObjects() {
		try {
			devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			componentName = new ComponentName(getApplicationContext(), DeviceAdmin.class);
		} catch (Exception e) {
			Log.e("DeviceAdminActivity", e.toString());
		}
	}

	private void mapViews() {
		button_allow = (Button) findViewById(R.id.admin_button_allow);
		button_deny = (Button) findViewById(R.id.admin_button_deny);
	}

	private void addListeners() {
		button_allow.setOnClickListener(this);
		button_deny.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		try {
			switch (v.getId()) {
				case R.id.admin_button_allow:
					Intent intent = new Intent(
							DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
					intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
							componentName);
					intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
							"ADMINISTRSTOR PERMISSIONS required to enable DOUBLE TAP TO LOCK");
					startActivityForResult(intent, RESULT_ENABLE);
					Log.i("DeviceAdminActivity", "Admin Rights requested");
					finish();
					break;

				case R.id.admin_button_deny:
					devicePolicyManager.removeActiveAdmin(componentName);
					Log.i("DeviceAdminActivity", "Admin Rights denied");
					finish();
					break;

				default:
					break;
			}
		} catch (Exception e) {
			Log.e("onClick", e.toString());
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
			case RESULT_ENABLE:
				if (resultCode == RESULT_OK) {
					Log.i("DeviceAdminActivity", "Admin Permission Granted");
				} else {
					Log.i("DeviceAdminActivity", "Admin Permission Denied");
				}
				break;

			default:
				break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

}
