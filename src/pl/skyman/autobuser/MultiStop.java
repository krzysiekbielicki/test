package pl.skyman.autobuser;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

public class MultiStop extends AppWidgetProvider {
	public static final String[] confs = new String[] {"stop1", "stop2", "stop3"};
	public static final int[] names = new int[] {R.id.stop1_name, R.id.stop2_name, R.id.stop3_name};
	public static final int[] datas = new int[] {R.id.stop1_data, R.id.stop2_data, R.id.stop3_data};
	//public static final String ACTION_UPDATE_ALL = "pl.skyman.autobuser.";
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	context.startService(new Intent(context, Widget2ReceiverUpdateService.class));
    }
    
    public static RemoteViews getRemoteView(int widgetId, Context context) {
    	RemoteViews updateViews = new RemoteViews("pl.skyman.autobuser", R.layout.widget);
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	
    	String tmp, name;
    	String[] tmp2;
    	int j = 0; 
    	for(int i = 0; i< confs.length; i++) {
    		tmp = settings.getString(confs[i], null);
    		name = settings.getString(confs[i]+"Name", "");
    		if(tmp != null && !name.equals("")) {
    			updateViews.setTextViewText(names[i], name);
    			tmp2 = tmp.split(",");
    			long stopid = Long.parseLong(tmp2[0]);
    			long lineid = Long.parseLong(tmp2[1]);
    			String deps = new Timetable( stopid, lineid, context.getContentResolver().query(DatabaseProvider.CONTENT_URI, Timetable.PROJECTION, DatabaseProvider.STOP_ID+"="+stopid+" AND "+DatabaseProvider.LINE_ID+"="+lineid, null, null) ).getNearestDepartureTimeToString(2);
    			if(deps.equals(""))
    				deps = context.getResources().getString(R.string.noDeparturesToday);
    			updateViews.setTextViewText(datas[i], Html.fromHtml(deps) );
    			updateViews.setViewVisibility(names[i], View.VISIBLE);
    			updateViews.setViewVisibility(datas[i], View.VISIBLE);
    		} else {
    			updateViews.setViewVisibility(names[i], View.GONE);
    			updateViews.setViewVisibility(datas[i], View.GONE);
    			j++;
    		}
    	}
    	if(j == 3) {
    		updateViews.setViewVisibility(R.id.mesage, View.VISIBLE);
    		updateViews.setViewVisibility(R.id.stops, View.GONE);
    		updateViews.setTextViewText(R.id.mesage, context.getResources().getString(R.string.widgetNotSet));
    	} else {
    		updateViews.setViewVisibility(R.id.stops, View.VISIBLE);
    		updateViews.setViewVisibility(R.id.mesage, View.GONE);
    	}
    	Intent intent = new Intent(context, Autobuser.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get the layout for the App Widget and attach an on-click listener to the button
        updateViews.setOnClickPendingIntent(R.id.stops, pendingIntent);
        return updateViews;
    }
}