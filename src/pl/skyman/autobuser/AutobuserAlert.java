package pl.skyman.autobuser;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class AutobuserAlert extends Activity {
	public static final String SQL_TABLE_NAME = "alarms";
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_MINUTES = "minutes";
	protected static final String SQL_KEY_DAY = "day";
	public static final String SQL_KEY_STOP = "stop";
	public static final String SQL_KEY_LINE = "line";
	public static final String SQL_KEY_REPEAT = "repeat";
	protected static final String SQL_KEY_TIME = "time";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_ID + " integer primary key autoincrement, " +
														SQL_KEY_TIME + " numeric not null, " +
														SQL_KEY_DAY + " numeric not null, " +
														SQL_KEY_STOP + " text not null, " +
														SQL_KEY_LINE + " text not null, " +
														SQL_KEY_MINUTES + " numeric not null, " +
														SQL_KEY_REPEAT + " integer not null);";
	private static final int FINISHED = 0;
	private static final int RUNNING = 1;
	private long time;
	private int day;
	private int minutes;
	private boolean repeat;
	private String stopname;
	private String linename;
	private int state;
	private long id;
	private AlarmPlayer alarm;
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String ignored) {
			// The user might already be in a call when the alarm fires. When
			// we register onCallStateChanged, we get the initial in-call state
			// which kills the alarm. Check against the initial call state so
			// we don't kill the alarm during a call.
			if (state != TelephonyManager.CALL_STATE_IDLE
					&& state != mInitialCallState) {
				alarm.stop(AutobuserAlert.this);
			}
		}
	};
	private TelephonyManager mTelephonyManager;
	private int mInitialCallState;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AutobuserAlertWakeLock.acquire(this);
		state = RUNNING;
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		alarm = new AlarmPlayer(this);

		id = getIntent().getLongExtra(AutobuserAlarmReceiver.ID, 0);
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ALERT, new String [] {SQL_KEY_TIME, SQL_KEY_DAY, SQL_KEY_STOP, SQL_KEY_LINE, SQL_KEY_MINUTES, SQL_KEY_REPEAT}, SQL_KEY_ID+"="+id, null, null);
				//SQL_TABLE_NAME, new String[] {SQL_KEY_TIME, SQL_KEY_DAY, SQL_KEY_STOPID, SQL_KEY_LINEID, SQL_KEY_MINUTES, SQL_KEY_REPEAT}, SQL_KEY_ID+"="+getIntent().getLongExtra(AutobuserAlarmReceiver.ID, 0), null, null, null, null);
		if(!c.moveToFirst()) return;
		
		time = c.getLong(0);
		day = c.getInt(1);
		stopname = c.getString(2);
		linename = c.getString(3);
		minutes = c.getInt(4);
		repeat = c.getInt(5) == 1;
		c.close();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alert);
		setTitle(R.string.reminder);
		
		((Button)findViewById(R.id.AutobuserAlertDismiss)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				setNewAndDismiss();
			}
		});
		((Button)findViewById(R.id.AutobuserAlertSnooze)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				showSnoozeDialog();
			}
		});
		
		int hour = minutes/60;
		int min = minutes%60;
				
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		
		if(cal.getTimeInMillis()-System.currentTimeMillis()<1000*60*10)
			((Button)findViewById(R.id.AutobuserAlertSnooze)).setEnabled(false);
		((TextView)findViewById(R.id.AlarmText)).setText(String.format(getResources().getString(R.string.reminderMessageFormat), hour+":"+((min<10?"0":"")+min), linename, stopname));
		//getWindow().addFlags(WindowManager.LayoutParams.);
		alarm.play(this);
		mInitialCallState = mTelephonyManager.getCallState();
	}
	
	
	protected void showSnoozeDialog() {
		alarm.stop(this);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, minutes/60);
		cal.set(Calendar.MINUTE, minutes%60);
		
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);
		long limit = ((cal.getTimeInMillis()-System.currentTimeMillis())/1000/60);
		for(int i = 10; i+1 < limit; i+=10)
		{
			adapter.add(String.format(getResources().getString(R.string.SnoozeDialog_XMinutes), i));
		}
		new AlertDialog.Builder(this).setAdapter(adapter, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				snooze(Integer.parseInt(adapter.getItem(which).split(" ")[0]));
			}
			
		})
		.setTitle(R.string.Snooze_RepeatAlarmIn)
		.create()
		.show();
	}


	@Override
	protected void onStop() {
		if(state == RUNNING)
			snooze(10);
		
		setNextAlarm(this);
		
		AutobuserAlertWakeLock.release();
		super.onStop();
	}

	public static final void setNextAlarm(Context context) {
		context.getContentResolver().delete(DatabaseProvider.CONTENT_URI_ALERT, SQL_KEY_TIME+"<"+System.currentTimeMillis(), null);
		Cursor c = context.getContentResolver().query(DatabaseProvider.CONTENT_URI_ALERT, new String[] {SQL_KEY_ID, SQL_KEY_TIME}, null, null, SQL_KEY_TIME+" ASC");
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(AutobuserAlarmReceiver.ACTION_AUTOBUSER_ALARM);
		if(!c.moveToFirst())
		{
			c.close();
			alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
			return;
		}
		intent.putExtra(AutobuserAlarmReceiver.ID, c.getLong(0));
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		long time = c.getLong(1);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
		c.close();
	}


	protected void setNewAndDismiss() {
		alarm.stop(this);
		if(repeat)
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(time);
			cal.add(Calendar.DAY_OF_MONTH, -(cal.get(Calendar.DAY_OF_WEEK)-day));
			ContentValues values = new ContentValues();
			values.put(AutobuserAlert.SQL_KEY_TIME, cal.getTimeInMillis());
			getContentResolver().update(DatabaseProvider.CONTENT_URI_ALERT, values, SQL_KEY_ID+"="+id, null);
		}
		state = FINISHED;
		AutobuserAlertWakeLock.release();
		finish();
	}
	protected void snooze(int i) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, minutes/60);
		cal.set(Calendar.MINUTE, (minutes%60)-i);
		
		ContentValues values = new ContentValues();
		values.put(AutobuserAlert.SQL_KEY_MINUTES, minutes);
		values.put(AutobuserAlert.SQL_KEY_REPEAT, false);
		values.put(AutobuserAlert.SQL_KEY_TIME, cal.getTimeInMillis());
		values.put(AutobuserAlert.SQL_KEY_DAY, day);
		values.put(AutobuserAlert.SQL_KEY_STOP, stopname);
		values.put(AutobuserAlert.SQL_KEY_LINE, linename);
		
		getContentResolver().insert(DatabaseProvider.CONTENT_URI_ALERT, values);
	
		state = FINISHED;
		AutobuserAlertWakeLock.release();
		finish();
	}
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return true;//super.dispatchKeyEvent(event);
	}
	
}
