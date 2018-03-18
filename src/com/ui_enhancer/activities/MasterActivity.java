package com.ui_enhancer.activities;

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ui_enhancer.screen_lock.DeviceAdminActivity;
import com.ui_enhancer.services.OverlayService;
import com.ui_enhancer.services.PopupService;
import com.y2k.uienhancer.R;

public class MasterActivity extends Activity implements
		OnCheckedChangeListener, OnSeekBarChangeListener,
		android.widget.RadioGroup.OnCheckedChangeListener {

	// widgets and views
	private ToggleButton		toggleButton_popup;
	private ToggleButton		toggleButton_filter;
	private SeekBar				seekBar_system;
	private SeekBar				seekBar_filter;
	private SeekBar				seekBar_volume;
	private TextView			textView_system;
	private TextView			textView_filter;
	private TextView			textView_volume;
	private LinearLayout		linearLayout;
	private CheckBox			checkBox;
	private RadioGroup			radioGroup_brightness;
	private RadioGroup			radioGroup_sound;

	// settings control
	private Window				window;
	private LayoutParams		layoutParams;
	private ContentResolver		contentResolver;
	private AudioManager		audioManager;
	private Intent				intent;

	// settings parameter constants
	private static final int	seek_max_brightness_int		= 255;
	private static final float	seek_max_brightness_float	= 255.0f;
	private static final int	seek_min_brightness_int		= 20;
	private static final int	seek_max_volume				= 7;

	// flags for event-handling
	private boolean				flag_silent;
	private boolean				flag_brightness_changed;
	private boolean				flag_filter_changed;
	private boolean				flag_volume_changed;
	private boolean				flag_brightness_profile_changed;
	private boolean				flag_sound_profile_changed;

	private final static String	LOG_TAG						= "MasterActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_master);

		checkAndGetAdmistrativePermission();
		mapViews();
		createObects();
		getAndSetAttributes();
		initializeFlags();
		addListeners();
		//stopFilterService();
		refreshTogglePopupButton();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		stopFilterService();
	}
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		// Toast.makeText(getApplicationContext(), "onRestart",
		// Toast.LENGTH_SHORT)
		// .show();
		getAndSetAttributes();
		stopFilterService();
		refreshTogglePopupButton();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			flag_volume_changed = true;
			flag_sound_profile_changed = true;
			setSystemVolume(audioManager
					.getStreamVolume(AudioManager.STREAM_RING));
			setSystemSoundProfile(getSoundRadioButtonId(audioManager
					.getRingerMode()));
			flag_volume_changed = false;
			flag_sound_profile_changed = false;

			return false;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			flag_volume_changed = true;
			flag_sound_profile_changed = true;
			setSystemVolume(Math.max(1,
					audioManager.getStreamVolume(AudioManager.STREAM_RING)));
			setSystemSoundProfile(getSoundRadioButtonId(audioManager
					.getRingerMode()));
			flag_volume_changed = false;
			flag_sound_profile_changed = false;

			return false;
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus) {
			getAndSetAttributes();
		}
	}

	private int getSoundRadioButtonId(int ringerMode) {
		switch (ringerMode) {
			case AudioManager.RINGER_MODE_NORMAL:
				return R.id.master_radioButton_sound_ring;

			case AudioManager.RINGER_MODE_VIBRATE:
				return R.id.master_radioButton_sound_vibrate;

			case AudioManager.RINGER_MODE_SILENT:
				return R.id.master_radioButton_sound_silent;

			default:
				break;
		}

		return -999;
	}

	private int getBrightnessRadioButtonId(int brightness) {
		switch (brightness) {
			case seek_min_brightness_int:
				return R.id.master_radioButton_brightness_low;

			case (seek_max_brightness_int / 2):
				return R.id.master_radioButton_brightness_medium;

			case seek_max_brightness_int:
				return R.id.master_radioButton_brightness_full;

			default:
				break;
		}

		return -999;
	}

	private void mapViews() {
		toggleButton_popup = (ToggleButton) findViewById(R.id.master_toggleButton_popup);
		toggleButton_filter = (ToggleButton) findViewById(R.id.master_toggleButton_filter);
		radioGroup_brightness = (RadioGroup) findViewById(R.id.master_radioGroup_brightness);
		radioGroup_sound = (RadioGroup) findViewById(R.id.master_radioGroup_sound);
		seekBar_system = (SeekBar) findViewById(R.id.master_seekBar_system);
		seekBar_filter = (SeekBar) findViewById(R.id.master_seekBar_filter);
		seekBar_volume = (SeekBar) findViewById(R.id.master_seekBar_volume);
		textView_system = (TextView) findViewById(R.id.master_textView_system);
		textView_filter = (TextView) findViewById(R.id.master_textView_filter);
		textView_volume = (TextView) findViewById(R.id.master_textView_volume);
		linearLayout = (LinearLayout) findViewById(R.id.master_linearLayout);
		checkBox = (CheckBox) findViewById(R.id.master_checkBox);
	}

	private void addListeners() {
		toggleButton_popup.setOnCheckedChangeListener(this);
		toggleButton_filter.setOnCheckedChangeListener(this);
		seekBar_system.setOnSeekBarChangeListener(this);
		seekBar_filter.setOnSeekBarChangeListener(this);
		seekBar_volume.setOnSeekBarChangeListener(this);
		checkBox.setOnCheckedChangeListener(this);
		radioGroup_brightness.setOnCheckedChangeListener(this);
		radioGroup_sound.setOnCheckedChangeListener(this);
	}

	private void createObects() {
		contentResolver = getContentResolver();
		window = getWindow();
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		layoutParams = window.getAttributes();
	}

	private void initializeFlags() {
		flag_brightness_changed = false;
		flag_brightness_profile_changed = false;
		flag_filter_changed = false;
		flag_silent = false;
		flag_volume_changed = false;
		flag_sound_profile_changed = false;
	}

	private void getAndSetAttributes() {
		int currentBrightness;
		int currentVolume;
		int currentRingerMode;

		currentBrightness = seek_max_brightness_int / 2;
		currentVolume = seek_max_volume / 2;
		currentRingerMode = AudioManager.RINGER_MODE_NORMAL;

		try {
			currentBrightness = android.provider.Settings.System.getInt(
					contentResolver,
					android.provider.Settings.System.SCREEN_BRIGHTNESS);
			currentVolume = audioManager
					.getStreamVolume(AudioManager.STREAM_RING);
			currentRingerMode = audioManager.getRingerMode();
		} catch (Exception e) {
			logInfo("getAndSetAttributes()", e.toString());
		} finally {
			setSystemBrightness(currentBrightness);

			flag_brightness_changed = true;
			flag_brightness_profile_changed = true;
			flag_volume_changed = true;
			flag_sound_profile_changed = true;

			switch (currentBrightness) {
				case seek_min_brightness_int:
					setSystemBrightnessProfile(R.id.master_radioButton_brightness_low);
					break;

				case (seek_max_brightness_int / 2):
					setSystemBrightnessProfile(R.id.master_radioButton_brightness_medium);
					break;

				case seek_max_brightness_int:
					setSystemBrightnessProfile(R.id.master_radioButton_brightness_full);
					break;

				default:
					break;
			}

			switch (currentRingerMode) {
				case AudioManager.RINGER_MODE_NORMAL:
					// setSystemSoundProfile(R.id.master_radioButton_sound_ring);
					radioGroup_sound
							.check(getSoundRadioButtonId(currentRingerMode));
					break;

				case AudioManager.RINGER_MODE_VIBRATE:
					setSystemSoundProfile(R.id.master_radioButton_sound_vibrate);
					break;

				case AudioManager.RINGER_MODE_SILENT:
					setSystemSoundProfile(R.id.master_radioButton_sound_silent);
					break;

				default:
					break;
			}

			// if (currentRingerMode != AudioManager.RINGER_MODE_SILENT) {
			// setSystemVolume(currentVolume);
			seekBar_volume.setProgress(currentVolume);
			textView_volume.setText(Integer.toString(currentVolume));
			// }

			if (currentRingerMode == AudioManager.RINGER_MODE_SILENT) {
				seekBar_volume.setEnabled(false);
			}

			flag_brightness_changed = false;
			flag_brightness_profile_changed = false;
			flag_volume_changed = false;
			flag_sound_profile_changed = false;
		}
	}

	private void checkAndGetAdmistrativePermission() {
		/*
		// objects to request Administrative Permissions
		SharedPreferences sharedPreferences;
		Editor editor;
		Intent intent;
		String preferenceName = "AdminPermission";
		String preferenceKey = "LockPermissionAsked";

		sharedPreferences = getSharedPreferences(preferenceName, MODE_PRIVATE);

		if (!sharedPreferences.getBoolean(preferenceKey, false)) {
			// Permissions haven't been requested. Request LockScreen
			// permissions
			intent = new Intent(getApplicationContext(),
					DeviceAdminActivity.class);
			startActivity(intent);
			editor = sharedPreferences.edit();
			editor.putBoolean(preferenceKey, true);
			editor.commit();
		} else {
			// Permissions have already been requested in the past. Nothing to
			// be done
		}
		*/
	}

	private void stopFilterService() {
		if (OverlayService.isRunning) {
			setFilterBrightness(OverlayService.brightness);
			toggleFilter(false, OverlayService.brightness);
		} else {
			setFilterBrightness(seek_max_brightness_int);
			toggleFilter(false, seek_max_brightness_int);
		}
		// togglePopup(false);
	}

	private void refreshTogglePopupButton() {
		// TODO Auto-generated method stub
		togglePopupButton(PopupService.isRunning);
	}

	private void logInfo(String tag, String message) {
		Log.d(tag, message);
	}

	private void setSystemBrightness(int brightness) {
		// if (!flag_brightness_changed) {
		// flag_brightness_changed = true;
		android.provider.Settings.System.putInt(contentResolver,
				android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);
		layoutParams.screenBrightness = (brightness / seek_max_brightness_float);
		window.setAttributes(layoutParams);
		seekBar_system.setProgress(brightness);
		// flag_brightness_changed = false;
		// } else {
		// flag_brightness_changed = false;
		// }

		textView_system.setText(Integer.toString(brightness));

		/*
		 * if (!flag_brightness_profile_changed) { switch (brightness) { case
		 * seek_min_brightness_int: flag_brightness_profile_changed = true;
		 * radioGroup_brightness .check(R.id.master_radioButton_brightness_low);
		 * break;
		 * 
		 * case (seek_max_brightness_int / 2): flag_brightness_profile_changed =
		 * true; radioGroup_brightness
		 * .check(R.id.master_radioButton_brightness_medium); break;
		 * 
		 * case seek_max_brightness_int: flag_brightness_profile_changed = true;
		 * radioGroup_brightness
		 * .check(R.id.master_radioButton_brightness_full); break;
		 * 
		 * default: flag_brightness_profile_changed = true;
		 * radioGroup_brightness.clearCheck(); break; } } else {
		 * flag_brightness_profile_changed = false; }
		 */
	}

	private void setSystemBrightnessProfile(int checkedId) {
		// if (!flag_brightness_profile_changed) {
		// flag_brightness_profile_changed = true;
		if (checkedId != -999) {
			radioGroup_brightness.check(checkedId);
		} else {
			radioGroup_brightness.clearCheck();
		}
		// } else {
		// flag_brightness_profile_changed = false;
		// }

		// if (!flag_brightness_changed) {
		switch (checkedId) {
			case R.id.master_radioButton_brightness_low:
				// flag_brightness_changed = true;
				setSystemBrightness(seek_min_brightness_int);
				break;

			case R.id.master_radioButton_brightness_medium:
				// flag_brightness_changed = true;
				setSystemBrightness(seek_max_brightness_int / 2);
				break;

			case R.id.master_radioButton_brightness_full:
				// flag_brightness_changed = true;
				setSystemBrightness(seek_max_brightness_int);
				break;

			default:
				break;
		}
		// }
	}

	private void setFilterBrightness(int brightness) {
		seekBar_filter.setProgress(brightness);
		linearLayout.setAlpha((seek_max_brightness_int - brightness)
				/ seek_max_brightness_float);
		textView_filter.setText(Integer.toString(brightness));
	}

	private void toggleFilter(boolean state, int brightness) {
		intent = new Intent(this, OverlayService.class);
		intent.putExtra("brightness", brightness);

		if (state) {
			startService(intent);
			linearLayout.setAlpha(0);
			toggleFilterButton(true);
			//logInfo("toggleFilter()", "OverlayService started");
		} else {
			stopService(intent);
			linearLayout.setAlpha((seek_max_brightness_int - brightness)
					/ seek_max_brightness_float);
			toggleFilterButton(false);
			//logInfo("toggleFilter()", "OverlayService stopped");
		}

		// toggleSoftKeyBacklight(state);
	}

	private void toggleFilter(int brightness) {
		intent = new Intent(this, OverlayService.class);
		intent.putExtra("brightness", brightness);

		if (!stopService(intent)) {
			toggleFilter(true, brightness);
			// toggleSoftKeyBacklight(true);
		} else {
			toggleFilter(false, brightness);
			// toggleSoftKeyBacklight(false);
		}
	}

	private void toggleFilterButton(boolean state) {
		toggleButton_filter.setChecked(state);
	}

	private void togglePopup(boolean state) {
		intent = new Intent(MasterActivity.this, PopupService.class);

		if (state) {
			startService(intent);
			togglePopupButton(true);
			//logInfo("togglePopup()", "PopupService started");
		} else {
			stopService(intent);
			togglePopupButton(false);
			//logInfo("togglePopup()", "PopupService stopped");
		}
	}

	private void togglePopup() {
		intent = new Intent(MasterActivity.this, PopupService.class);

		if (!stopService(intent)) {
			togglePopup(true);
		} else {
			togglePopup(false);
		}
	}

	private void togglePopupButton(boolean state) {
		toggleButton_popup.setChecked(state);
	}

	private void setSystemVolume(int volume) {
		// if (!flag_volume_changed) {
		// flag_volume_changed = true;
		audioManager.setStreamVolume(AudioManager.STREAM_RING, volume,
				AudioManager.FLAG_ALLOW_RINGER_MODES
						| AudioManager.FLAG_PLAY_SOUND);
		seekBar_volume.setProgress(volume);
		textView_volume.setText(Integer.toString(volume));
		// } else {
		// flag_volume_changed = false;
		// }

		/*
		 * textView_volume.setText(Integer.toString(volume));
		 * 
		 * if (!flag_sound_profile_changed) { if (volume == 0) {
		 * setSystemSoundProfile(R.id.master_radioButton_sound_vibrate);
		 * flag_sound_profile_changed = true; } else {
		 * setSystemSoundProfile(R.id.master_radioButton_sound_ring);
		 * flag_sound_profile_changed = true; } } else {
		 * flag_sound_profile_changed = false; }
		 */
	}

	private void setSystemSoundProfile(int checkedId) {
		// if (!flag_sound_profile_changed) {
		// flag_sound_profile_changed = true;
		radioGroup_sound.check(checkedId);

		if (checkedId == R.id.master_radioButton_sound_ring) {
			seekBar_volume.setEnabled(true);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			setSystemVolume(seekBar_volume.getProgress());
		} else if (checkedId == R.id.master_radioButton_sound_vibrate) {
			seekBar_volume.setEnabled(true);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			setSystemVolume(0);
		} else if (checkedId == R.id.master_radioButton_sound_silent) {
			seekBar_volume.setEnabled(false);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		}
		// } else {
		// flag_sound_profile_changed = false;
		// }

		/*
		 * if (!flag_volume_changed) { switch (checkedId) { case
		 * R.id.master_radioButton_sound_ring: seekBar_volume.setEnabled(true);
		 * setSystemVolume(Math.max(seekBar_volume.getProgress(),
		 * (seek_max_volume / 2))); break;
		 * 
		 * case R.id.master_radioButton_sound_vibrate:
		 * seekBar_volume.setEnabled(true); setSystemVolume(0); break;
		 * 
		 * case R.id.master_radioButton_sound_silent:
		 * seekBar_volume.setEnabled(false);
		 * 
		 * default: break; } flag_volume_changed = true; } else {
		 * flag_volume_changed = false; }
		 */
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		switch (group.getId()) {
			case R.id.master_radioGroup_brightness:
				toggleFilter(false, seek_max_brightness_int);
				toggleFilterButton(false);
				setFilterBrightness(seek_max_brightness_int);
				if (!flag_brightness_profile_changed) {
					flag_brightness_profile_changed = true;
					flag_brightness_changed = true;
					setSystemBrightnessProfile(checkedId);
					setSystemBrightness(seekBar_system.getProgress());
					flag_brightness_changed = false;
					flag_brightness_profile_changed = false;
				} else {
					flag_brightness_profile_changed = false;
					flag_brightness_changed = false;
				}
				break;

			case R.id.master_radioGroup_sound:
				if (!flag_sound_profile_changed) {
					flag_sound_profile_changed = true;
					flag_volume_changed = true;
					if (checkedId == R.id.master_radioButton_sound_ring) {
						if (seekBar_volume.getProgress() > 0) {
							setSystemVolume(seekBar_volume.getProgress());
						} else {
							setSystemVolume(seek_max_volume / 2);
						}
					} else if (checkedId == R.id.master_radioButton_sound_vibrate) {
						setSystemVolume(0);
					}
					setSystemSoundProfile(checkedId);
					flag_sound_profile_changed = false;
					flag_volume_changed = false;
				} else {
					flag_sound_profile_changed = false;
					flag_volume_changed = false;
				}
				break;

			default:
				break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		if (fromUser) {
			switch (seekBar.getId()) {
				case R.id.master_seekBar_system:
					if (!flag_brightness_changed) {
						flag_brightness_changed = true;
						setSystemBrightness(Math.max(progress,
								seek_min_brightness_int));
						flag_brightness_changed = false;
					} else {
						flag_brightness_changed = false;
					}
					break;

				case R.id.master_seekBar_filter:
					setFilterBrightness(Math.max(progress,
							seek_min_brightness_int));
					break;

				case R.id.master_seekBar_volume:
					if (!flag_volume_changed) {
						flag_volume_changed = true;
						setSystemVolume(progress);
						flag_volume_changed = false;
					} else {
						flag_volume_changed = false;
					}
					break;

				default:
					break;
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

		switch (seekBar.getId()) {
			case R.id.master_seekBar_system:
				toggleFilter(false, seek_max_brightness_int);
				toggleFilterButton(false);
				setFilterBrightness(seek_max_brightness_int);
				logInfo("onStartTrackingTouch()",
						"System SeekBar touched, Filter removed");
				break;

			case R.id.master_seekBar_filter:
				toggleFilter(false, seek_max_brightness_int);
				toggleFilterButton(false);
				setSystemBrightness(seek_min_brightness_int);
				logInfo("onStartTrackingTouch()", "Filter SeekBar touched");
				break;

			case R.id.master_seekBar_volume:
				logInfo("onStartTrackingTouch()", "Volume SeekBar touched");
				break;

			default:
				break;
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		switch (seekBar.getId()) {
			case R.id.master_seekBar_system:
				if (!flag_brightness_profile_changed) {
					flag_brightness_profile_changed = true;
					setSystemBrightnessProfile(getBrightnessRadioButtonId(seekBar
							.getProgress()));
					/*
					 * if (seekBar.getProgress() == seek_min_brightness_int) {
					 * setSystemBrightnessProfile
					 * (R.id.master_radioButton_brightness_low); //
					 * flag_brightness_profile_changed = true; } else if
					 * (seekBar.getProgress() == (seek_max_brightness_int / 2))
					 * { setSystemBrightnessProfile(R.id.
					 * master_radioButton_brightness_medium); //
					 * flag_brightness_profile_changed = true; } else if
					 * (seekBar.getProgress() == seek_max_brightness_int) {
					 * setSystemBrightnessProfile
					 * (R.id.master_radioButton_brightness_full); //
					 * flag_brightness_profile_changed = true; } else {
					 * radioGroup_brightness.clearCheck(); //
					 * flag_brightness_profile_changed = true; }
					 */
					flag_brightness_profile_changed = false;
				} else {
					flag_brightness_profile_changed = false;
				}
				break;

			case R.id.master_seekBar_filter:
				break;

			case R.id.master_seekBar_volume:
				if (!flag_sound_profile_changed) {
					flag_sound_profile_changed = true;
					if (seekBar.getProgress() == 0) {
						setSystemSoundProfile(R.id.master_radioButton_sound_vibrate); //
						// flag_sound_profile_changed = true;
					} else if (seekBar.getProgress() > 0) {
						setSystemSoundProfile(R.id.master_radioButton_sound_ring);
						// flag_sound_profile_changed = true;
					}
					flag_sound_profile_changed = false;
				} else {
					flag_sound_profile_changed = false;
				}
				break;

			default:
				break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch (buttonView.getId()) {
			case R.id.master_checkBox:
				break;

			case R.id.master_toggleButton_popup:
				togglePopup(isChecked);
				break;

			case R.id.master_toggleButton_filter:
				toggleFilter(isChecked, seekBar_filter.getProgress());
				break;

			default:
				break;
		}
	}

}
