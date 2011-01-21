package pl.skyman.autobuser;

import java.util.LinkedList;
import java.util.Queue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;

public class Widget2ReceiverUpdateService extends Service implements Runnable {
	//public static final String[] confs = new String[] {"stop1", "stop2", "stop3"};
	//public static final int[] names = new int[] {R.id.stop1_name, R.id.stop2_name, R.id.stop3_name};
	//public static final int[] datas = new int[] {R.id.stop1_data, R.id.stop2_data, R.id.stop3_data};
	private static Object sLock = new Object();
	private static boolean sThreadRunning = false;
	private boolean updated;
	private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();
	
	public void requestUpdates(int[] ids) {
		synchronized (sLock) {
            for (int appWidgetId : ids) {
                sAppWidgetIds.add(appWidgetId);
            }
        }
	}
	
    private static boolean hasMoreUpdates() {
        synchronized (sLock) {
            boolean hasMore = !sAppWidgetIds.isEmpty();
            if (!hasMore) {
                sThreadRunning = false;
            }
            return hasMore;
        }
    }
    
    private static int getNextUpdate() {
        synchronized (sLock) {
            if (sAppWidgetIds.peek() == null) {
                return AppWidgetManager.INVALID_APPWIDGET_ID;
            } else {
                return sAppWidgetIds.poll();
            }
        }
    }
    @Override
    public void onStart(Intent in, int startId) {
    	AppWidgetManager manager = AppWidgetManager.getInstance(this);
		requestUpdates(manager.getAppWidgetIds(new ComponentName(this, MultiLineWidget.class)));
		requestUpdates(manager.getAppWidgetIds(new ComponentName(this, MultiStop.class)));
		synchronized (sLock) {
            if (!sThreadRunning) {
                sThreadRunning = true;
                new Thread(this).start();
            }
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // We don't need to bind to this service
        return null;
    }
	public void run() {
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		while(hasMoreUpdates()) {
			int widgetId = getNextUpdate();
			switch(manager.getAppWidgetInfo(widgetId).initialLayout) {
			case R.layout.widget:
				manager.updateAppWidget(widgetId, MultiStop.getRemoteView(widgetId, this));
				break;
			case R.layout.widget2:
				manager.updateAppWidget(widgetId, MultiLineWidget.getRemoteView(widgetId, this));
				break;
		}
		}
		Time time = new Time();
	    time.set(System.currentTimeMillis() + DateUtils.MINUTE_IN_MILLIS);
        time.second = 0;

        long nextUpdate = time.toMillis(false);
        Intent updateIntent = new Intent();
        updateIntent.setClass(this, Widget2ReceiverUpdateService.class);

        PendingIntent pIntent = PendingIntent.getService(this, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdate, pIntent);
		stopSelf();
		sThreadRunning = false;
	}
}