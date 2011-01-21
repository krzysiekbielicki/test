package pl.skyman.autobuser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutobuserAlarmReceiver extends BroadcastReceiver {
	public static final String ACTION_AUTOBUSER_ALARM = "pl.skyman.autobuser.ACTION_AUTOBUSER_ALARM";
	protected static final String ID = "ID";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(action.equals(ACTION_AUTOBUSER_ALARM)) {
			AutobuserAlertWakeLock.acquire(context);
			Intent fireAlarm = new Intent(context, AutobuserAlert.class);
			fireAlarm.putExtra(AutobuserAlarmReceiver.ID, intent.getLongExtra(ID, 0));
			fireAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			context.startActivity(fireAlarm);
		} else if(action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
			AutobuserAlert.setNextAlarm(context);
		}
	}
}
