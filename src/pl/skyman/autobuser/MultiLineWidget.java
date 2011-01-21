package pl.skyman.autobuser;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

public class MultiLineWidget extends AppWidgetProvider {
	
	public static final String SQL_TABLE_NAME = "widgets";
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_ICON = "icon";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_STOPS = "stops";
	
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_ID + " integer, " +
														SQL_KEY_ICON + " integer, " +
														SQL_KEY_NAME + " text not null, " +
														SQL_KEY_STOPS + " text not null, " +
														"CONSTRAINT U_Widget2 UNIQUE ("+SQL_KEY_ID+"));";
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	context.startService(new Intent(context, Widget2ReceiverUpdateService.class));
    }


	public static RemoteViews getRemoteView(int widgetId, Context context) {
		RemoteViews updateViews = new RemoteViews("pl.skyman.autobuser", R.layout.widget2);

		Cursor c = context.getContentResolver().query(DatabaseProvider.CONTENT_URI_MULTILINEWIDGET, new String[] {MultiLineWidget.SQL_KEY_ICON, MultiLineWidget.SQL_KEY_NAME, MultiLineWidget.SQL_KEY_STOPS}, MultiLineWidget.SQL_KEY_ID+"="+widgetId, null, null);
		if(c.moveToNext()) {
    		int icon = c.getInt(0);
    		String name = c.getString(1);
    		String[] stops = c.getString(2).split(" ");
    		c.close();
    		//startMan
    		//c.close();
    		LinkedList<WDeparture> departures = new LinkedList<WDeparture>();
    		LinkedList<Departure> lDepartures;
    		String[] stop;
    		int size;
    		String sname = "";
    		for(int j = 0; j < stops.length; j++) {
    			stop = stops[j].split("\\|");
    			if(stop.length != 2)
    				continue;
    			Cursor c1 = context.getContentResolver().query(DatabaseProvider.CONTENT_URI, new String[] {DatabaseProvider.LINE_NAME}, DatabaseProvider.LINE_ID+"="+stop[1], null, null);
    			if(c1.moveToNext())
    				sname = c1.getString(0);
    			c1.close();
    			long stopid = Long.parseLong(stop[0]);
    			long lineid = Long.parseLong(stop[1]);
    			lDepartures = new Timetable( stopid, lineid, context.getContentResolver().query(DatabaseProvider.CONTENT_URI, Timetable.PROJECTION, DatabaseProvider.STOP_ID+"="+stopid+" AND "+DatabaseProvider.LINE_ID+"="+lineid, null, null) ).getNearestDepartures(6);
    			size = lDepartures.size();
    			for(int k = 0; k < size; k++) {
    				departures.add(new WDeparture(lDepartures.get(k), sname));
    			}
    		}
    		Collections.sort(departures, new Comparator<WDeparture>() {
    			public int compare(WDeparture d1, WDeparture d2) {
    				return d1.minute>d2.minute?1:-1;
    			}
    		});
    		
    		String deps = "";
    		size = Math.min(6, departures.size());
    		int departuretime;
    		char note;
    		WDeparture dep;
    		for(int j = 0; j < size; j++) {
    				dep = departures.get(j);
    				note = dep.modifier;
    				departuretime = dep.minute;
    				sname = dep.name;
    				if(departuretime < 60)
    					deps += (dep.isLowFloor?" <u>":" ")+(departuretime)+"min"+(dep.isLowFloor?"</u>":"");
    				else
    					deps += (dep.isLowFloor?" <u>":" ")+(departuretime/60)+"h"+(departuretime%60<10?"0":"")+(departuretime%60)+"m"+(dep.isLowFloor?"</u>":"");
    				if(note >= 'a')
    					deps += "<b>"+note+"</b>";
    				if(j+1 < size)
    					if(departures.get(j+1).name != sname)
    						deps += " ("+sname+") ";
    					else
    						deps += ",";
    				else
    					deps += " ("+sname+") ";
    		}
    		updateViews.setViewVisibility(R.id.name, name.equals("")?View.GONE:View.VISIBLE);
	    	updateViews.setImageViewResource(R.id.icon, icon);
    		updateViews.setTextViewText(R.id.name, name);
    		updateViews.setTextViewText(R.id.deps, Html.fromHtml(deps));
		}
		//c.close();
		//updateViews.setTextViewText(R.id.name, widgetId+"");
		Intent intent = new Intent(context, MultiLineWidgetSettings.class);
		intent.setData(Uri.parse(widgetId+""));
		intent.putExtra("widgetId", widgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.icon, pendingIntent);
        return updateViews;
	}
}