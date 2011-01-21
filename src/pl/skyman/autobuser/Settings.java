package pl.skyman.autobuser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.RemoteViews;
import android.widget.Toast;

public class Settings extends PreferenceActivity{
	private PreferenceManager pm;

	private final Preference.OnPreferenceChangeListener pcl = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference p,	Object value) {
			try{
				ListPreference lp = (ListPreference) p;
				CharSequence[] ev = lp.getEntryValues();
				for(int i = 0; i<ev.length; i++)
					if(ev[i].equals(value)) {
						p.setSummary(lp.getEntries()[i]);
						pm.findPreference(p.getKey()+"Name").setSummary(lp.getEntries()[i]);
						Editor e = settings.edit();
						e.putString(p.getKey()+"Name", lp.getEntries()[i]+"");
						e.commit();
						//p.setSummary(settings.getString("stop2Name", ""));
						return true;
					}
			} catch(ClassCastException e) {
				p.setSummary((String) value);
			}
			refreshWidget();
			return true;
		}
	};
	
	private class RingtoneChangedListener implements AlarmPreference.IRingtoneChangedListener {
        public void onRingtoneChanged(Uri ringtoneUri) {
            saveAlarm();
        }
    }


	private SharedPreferences settings;
	private AlarmPreference mAlarmPref;	@Override
	protected void onResume() {
		super.onResume();
		setTitle(R.string.settings);
		count();
	}

	protected void refreshWidget() {
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.updateAppWidget(new ComponentName(getPackageName(), "WidgetReceiver"), new RemoteViews(getPackageName(), R.layout.widget));
	}

	private void count() {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_SEARCHHISTORY, new String[] {"COUNT (*)"}, null, null, null);
		c.moveToFirst();
		int i = c.getInt(0);
		pm.findPreference("cleanSearches").setSummary(String.format(getResources().getString(R.string.records), i));
		if(i == 0)
			pm.findPreference("cleanSearches").setEnabled(false);
		c.close();
		c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, new String[] {"COUNT (*)"}, null, null, null);
		c.moveToFirst();
		i = c.getInt(0);
		pm.findPreference("cleanRoutes").setSummary(String.format(getResources().getString(R.string.records), i));
		if(i == 0)
			pm.findPreference("cleanRoutes").setEnabled(false);
		c.close();
		c = getContentResolver().query(DatabaseProvider.CONTENT_URI_TIMETABLE, new String[] {"MIN("+Timetable.SQL_KEY_UPDATETIME+")"}, null, null, null);
		c.moveToFirst();
		long l = c.getLong(0);
		if(l > 1) {
			Calendar gc = Calendar.getInstance();
			gc.setTimeInMillis(l * 60 * 1000);
			pm.findPreference("syncTimetables").setSummary(getResources().getString(R.string.lastUpdate)+" "+new SimpleDateFormat("yyyy-MM-dd HH:mm").format(gc.getTime()));
		} else {
			pm.findPreference("syncTimetables").setSummary(getResources().getString(R.string.lastUpdate)+" "+getResources().getString(R.string.never));
		}
		c.close();
	}
	
	private Uri getDefaultAlarm() {
		RingtoneManager ringtoneManager = new RingtoneManager(this);
		ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
		return ringtoneManager.getRingtoneUri(0);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		pm = getPreferenceManager();
		settings = getPreferenceManager().getSharedPreferences();
		mAlarmPref = (AlarmPreference)findPreference("reminderRingtone");
		String ringtone = settings.getString("reminderRingtone", null);
		if(ringtone != null)
			mAlarmPref.mAlert = Uri.parse(ringtone);
		else
			mAlarmPref.mAlert = getDefaultAlarm();
		mAlarmPref.setRingtoneChangedListener(new RingtoneChangedListener());

	}
	//startActivity(new Intent(Autobuser.this, ManageAlerts.class));

	protected void saveAlarm() {
		if (mAlarmPref.mAlert != null) {
            Editor e = settings.edit();
			e.putString("reminderRingtone", mAlarmPref.mAlert.toString());
			e.commit();
        }
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		String pKey = preference.getKey();
		String psKey = preferenceScreen.getKey();
		if(pKey.equals("manage_alerts"))
			startActivity(new Intent(Settings.this, ManageAlerts.class));
		if(pKey == null || psKey == null)
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		
		if(psKey.equals("main")){
			if(pKey.equals("cleanSearches")) {
				getContentResolver().delete(DatabaseProvider.CONTENT_URI_SEARCHHISTORY, null, null);
				count();
			} else if(pKey.equals("cleanRoutes")) {
				getContentResolver().delete(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, null, null);
				count();
			} else if(pKey.equals("syncZiL")) {
				Autobuser.syncZespolyILinie(this);
			} else if(pKey.equals("syncTimetables")) {
				Autobuser.updateTimetables(this, new Handler() {
					@Override
				    public void handleMessage(Message msg) {
						count();
			        }
				});
			}
			else if(pKey.equals("widget")) {
				int i = 0;
				ListPreference lp;
				Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI, new String[]{DatabaseProvider.LINE_ID, DatabaseProvider.STOP_ID, DatabaseProvider.MYSTOP_NAME}, null, null, DatabaseProvider.MYSTOP_NAME);
				String[] entries = new String[c.getCount()];
				String[] entryValues = new String[c.getCount()];
				int[] indexes = new int[] {-1, -1, -1};
				while(c.moveToNext()) {
					entries[i] = c.getString(2);
					entryValues[i] = c.getInt(1)+","+c.getInt(0);
					if(entryValues[i].equals(settings.getString("stop1", "")))
						indexes[0] = i;
					if(entryValues[i].equals(settings.getString("stop2", "")))
						indexes[1] = i;
					if(entryValues[i].equals(settings.getString("stop3", "")))
						indexes[2] = i;
 
					i++;
				}
				c.close();
				
				Preference p;
				p = ((PreferenceScreen)preference).findPreference("stop1Name");
				p.setSummary(settings.getString("stop1Name", ""));
				p.setOnPreferenceChangeListener(pcl);
				lp = (ListPreference)((PreferenceScreen)preference).findPreference("stop1");
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);
				if(indexes[0] >= 0) {
					lp.setValueIndex(indexes[0]);
					lp.setSummary(entries[indexes[0]]);
				}
				lp.setOnPreferenceChangeListener(pcl);
				
				p = ((PreferenceScreen)preference).findPreference("stop2Name");
				p.setSummary(settings.getString("stop2Name", ""));
				p.setOnPreferenceChangeListener(pcl);
				lp = (ListPreference)((PreferenceScreen)preference).findPreference("stop2");
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);
				if(indexes[1] >= 0) {
					lp.setValueIndex(indexes[1]);
					lp.setSummary(entries[indexes[1]]);
				}
				lp.setOnPreferenceChangeListener(pcl);
				
				p = ((PreferenceScreen)preference).findPreference("stop3Name");
				p.setSummary(settings.getString("stop3Name", ""));
				p.setOnPreferenceChangeListener(pcl);
				lp = (ListPreference)((PreferenceScreen)preference).findPreference("stop3");
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);
				if(indexes[2] >= 0) {
					lp.setValueIndex(indexes[2]);
					lp.setSummary(entries[indexes[2]]);
				}
				lp.setOnPreferenceChangeListener(pcl);
			}
			return true;
		} else if(psKey.equals("database")) {
			if(pKey.equals("dbtocard")) {
				if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					Toast.makeText(this, R.string.sdCardNotMounted, Toast.LENGTH_SHORT).show();
					return true;
				}
				File dbFile = new File(Environment.getDataDirectory() + "/data/pl.skyman.autobuser/databases/autobuser");
				File exportDir = new File(Environment.getExternalStorageDirectory(), "autobuser");
				if (!exportDir.exists()) {
					exportDir.mkdirs();
				}
				File file = new File(exportDir, dbFile.getName());
				try {
					file.createNewFile();
					Autobuser.copyFile(dbFile, file);
					Toast.makeText(this, R.string.dbCopiedSuccesfully, Toast.LENGTH_SHORT).show();
					return true;
				} catch (IOException e) {
					Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
					return true;
				}

			} else if(pKey.equals("dbtophone")) {
				if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					Toast.makeText(this, R.string.sdCardNotMounted, Toast.LENGTH_SHORT).show();
					return true;
				}
				File dbFile = new File(Environment.getDataDirectory() + "/data/pl.skyman.autobuser/databases/autobuser");
				File exportDir = new File(Environment.getExternalStorageDirectory(), "autobuser");
				if (!exportDir.exists()) {
					Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
				}
				File file = new File(exportDir, dbFile.getName());
				try {
					//file.createNewFile();
					Autobuser.copyFile(file, dbFile);
					Toast.makeText(this, R.string.dbCopiedSuccesfully, Toast.LENGTH_SHORT).show();
					return true;
				} catch (IOException e) {
					Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
					return true;
				}
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	public void locationChanges(Location newLocation) {
	}

	public void minuteChanges() {
	}

	public void orientationChanges(float[] newOrientation) {
	}

	public void setTitle(String title) {
	}
}
