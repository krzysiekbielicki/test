package pl.skyman.autobuser;

import java.util.Calendar;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAlerts extends ListActivity{
		
	private static final int DELETE_ALERT = 0;
	protected ZiLStop stop;
	private CursorAdapter acAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.manageAlerts);
		setContentView(R.layout.managealerts);
		
		acAdapter = new CursorAdapter(this, managedQuery(DatabaseProvider.CONTENT_URI_ALERT, 
				new String[] {AutobuserAlert.SQL_KEY_ID, AutobuserAlert.SQL_KEY_TIME, AutobuserAlert.SQL_KEY_DAY, 
				AutobuserAlert.SQL_KEY_MINUTES, AutobuserAlert.SQL_KEY_REPEAT, AutobuserAlert.SQL_KEY_STOP,
				AutobuserAlert.SQL_KEY_LINE}, null, null, AutobuserAlert.SQL_KEY_TIME+" ASC")) {
		
			@Override
			public View newView(Context context, Cursor c, ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v = inflater.inflate( R.layout.alertitem, null );
				
				((TextView)v.findViewById(R.id.AlertItem_FromTo)).setText(c.getString(5));
				((TextView)v.findViewById(R.id.AlertItem_Linia)).setText(c.getString(6));
				int minutes = c.getInt(3);
				int min = (minutes%60);
				((TextView)v.findViewById(R.id.AlertItem_Departure)).setText((minutes/60)+":"+(min<10?"0"+min:min));
				boolean repeat = c.getInt(4)==1;
				
				((TextView)v.findViewById(R.id.AlertItem_DayRepeat)).setText((repeat?getResources().getString( (c.getInt(2)==1||c.getInt(2)==4||c.getInt(2)==7)?R.string.every2:R.string.every1 )+" ":"")+getResources().getStringArray(R.array.daysOfWeek)[c.getInt(2)-1]);
				
				long time = c.getLong(1);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(time);
				cal.set(Calendar.HOUR_OF_DAY, minutes/60);
				cal.set(Calendar.MINUTE, minutes%60);
				time = (cal.getTimeInMillis()-time)/60/1000;
				
				((TextView)v.findViewById(R.id.AlertItem_Before)).setText(String.format(getResources().getString(R.string.remindBefore), time));
				
				return v;
			}
			
			@Override
			public void bindView(View arg0, Context arg1, Cursor arg2) {
			}
		};
		setListAdapter(acAdapter);
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ALERT, 0, R.string.deleteAlert);
	}
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		switch (aItem.getItemId()) {
			case DELETE_ALERT:
				long id = acAdapter.getItemId(((AdapterContextMenuInfo)aItem.getMenuInfo()).position);
				getContentResolver().delete(DatabaseProvider.CONTENT_URI_ALERT, AutobuserAlert.SQL_KEY_ID+"="+id, null);
				refreshList();
				AutobuserAlert.setNextAlarm(ManageAlerts.this);
				return true; /* true means: "we handled the event". */  
	     }
	     return false;
	}
	
	

	private void refreshList() {
		acAdapter.changeCursor(managedQuery(DatabaseProvider.CONTENT_URI_ALERT, 
				new String[] {AutobuserAlert.SQL_KEY_ID, AutobuserAlert.SQL_KEY_TIME, AutobuserAlert.SQL_KEY_DAY, 
				AutobuserAlert.SQL_KEY_MINUTES, AutobuserAlert.SQL_KEY_REPEAT, AutobuserAlert.SQL_KEY_STOP,
				AutobuserAlert.SQL_KEY_LINE}, null, null, AutobuserAlert.SQL_KEY_TIME+" ASC"));
		setListAdapter(acAdapter);
	}

	public void locationChanges(Location newLocation) {
	}

	public void minuteChanges() {
	}

	public void orientationChanges(float[] newOrientation) {
	} 
}