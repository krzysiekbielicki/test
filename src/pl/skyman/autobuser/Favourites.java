package pl.skyman.autobuser;

import java.util.Collections;
import java.util.LinkedList;

import pl.skyman.autobuser.MyPlace.Place;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Favourites extends ListActivity implements AutobuserActivity{
	private LinkedList<MyStop> myStops;
	private Location mLocation;
	private SharedPreferences settings;
	private float[] mOrientation;
	private LinkedList<MyPlace.Place> myPlaces;
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.favourites);
    	settings = getSharedPreferences(Autobuser.PREFS_NAME, 0);
		ListView list = getListView();
		list.setItemsCanFocus(true);
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int i,	long id) {
				MyStop stop = myStops.get(i); 
				Intent intent = new Intent(Favourites.this, TimetableActivity.class);
				intent.putExtra("lineid", stop.getLineId());
				intent.putExtra("stopid", stop.getStopId());
				((Autobuser)getParent()).startActivity(intent);
			}
		});
		getListView().setFocusable(true);
		registerForContextMenu(list);
    }
	@Override
	protected void onResume() {
		super.onResume();
		getParent().setTitle(R.string.favourites);
		if(mLocation == null)
			mLocation = new Location(((Autobuser)getParent()).getLocation());
		else
			mLocation.set(((Autobuser)getParent()).getLocation());
    	refreshMyStopsList();
    	refreshMyPlaces();
    	//redrawGeoViews();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
	

	private void refreshMyPlaces() {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_MYPLACE, new String[] {MyPlace.SQL_KEY_NAME, MyPlace.SQL_KEY_VALUE}, null, null, MyPlace.SQL_KEY_FAVOURITE+" DESC, "+MyPlace.SQL_KEY_NAME+" ASC");
        //db.close();
		myPlaces = new LinkedList<MyPlace.Place>();
		while(c.moveToNext())
        {
        	myPlaces.add(new MyPlace.Place(c.getString(0), c.getString(1)));
        	if(myPlaces.size() >= 5) break;
        }
		c.close();
        myPlaces.add(new MyPlace.Place("...", "@more"));
        ((GridView)findViewById(R.id.myplaces)).setAdapter(new MyPlacesAdapter(this, myPlaces));
        ((GridView)findViewById(R.id.myplaces)).setFocusable(false);
	}
	
	private class MyPlacesAdapter extends BaseAdapter{

		private LinkedList<Place> places;
		private Context context;

		public MyPlacesAdapter(Context context, LinkedList<MyPlace.Place> places) {
			this.context = context;
			this.places = places;
		}
		public int getCount() {
			return places.size();
		}

		public Place getItem(int i) {
			return places.get(i);
		}

		public long getItemId(int i) {
			return i;
		}

		public View getView(final int i, View arg1, ViewGroup arg2) {
			Button v = new Button(context);
			Place item = getItem(i);
			v.setText(item.getName());
			if(item.getValue().equals("@more"))
				v.setWidth(20);
			v.setOnClickListener(new OnClickListener(){
				public void onClick(View arg0) {
					String value = getItem(i).getValue();
					if(value.equals("@more")){
						Intent in = new Intent(Favourites.this, MyPlace.class);
						((Autobuser)getParent()).startActivity(in);
					} else {
						Intent in = new Intent(Favourites.this, Route.class);
						in.putExtra("start", "@GPS");
						in.putExtra("destination", value);
						((Autobuser)getParent()).startActivity(in);
					}
				}
			});
			return v;
		}
		
	}
	
	protected void findZiL() {
		Intent intent = new Intent(Favourites.this, Search.class);
		intent.putExtra("q", ((AutoCompleteTextView) findViewById(R.id.atv)).getText().toString());
		startActivity(intent);
	}
	
	private void refreshMyStopsList() {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI, 
				new String[] {
				DatabaseProvider.MYSTOP_NAME, 
				DatabaseProvider.STOP_ID, 
				DatabaseProvider.STOP_NAME,
				DatabaseProvider.STOP_NO,
				DatabaseProvider.STOP_LAT,
				DatabaseProvider.STOP_LON,
				DatabaseProvider.LINE_ID,
				DatabaseProvider.LINE_NAME,
				DatabaseProvider.FIRST_STOP_NAME,
				DatabaseProvider.FIRST_STOP_NO,
				DatabaseProvider.FIRST_STOP_LAT,
				DatabaseProvider.FIRST_STOP_LON,
				DatabaseProvider.LAST_STOP_NAME,
				DatabaseProvider.LAST_STOP_NO,
				DatabaseProvider.LAST_STOP_LAT,
				DatabaseProvider.LAST_STOP_LON
				}, null, null, null);
        //db.close();
		myStops = new LinkedList<MyStop>();
        MyStop stop;
        long stopid, lineid;
        while(c.moveToNext())
        {
        	stopid = c.getLong(1);
			lineid = c.getLong(6);
        	stop = new MyStop(c.getString(0), 
        			new Stop(stopid, c.getString(2), c.getInt(3), c.getDouble(4), c.getDouble(5)), 
        			new Line(lineid,
        				c.getString(7),
        				new Stop(c.getString(8), c.getInt(9), c.getDouble(10), c.getDouble(11)), 
        				new Stop(c.getString(12), c.getInt(13), c.getDouble(14), c.getDouble(15))), 
        			new Timetable(stopid, lineid, managedQuery(DatabaseProvider.CONTENT_URI, Timetable.PROJECTION, DatabaseProvider.STOP_ID+"="+stopid+" AND "+DatabaseProvider.LINE_ID+"="+lineid, null, null)));
        	stop.setDistanceTo(mLocation);
        	myStops.add(stop);
        }
        c.close();
        int sortMyStopsBy = Integer.parseInt(settings.getString("SortMyStopsBy", "0"));
        switch(sortMyStopsBy) {
	        case Autobuser.SORT_BY_NAME:
	        	Collections.sort(myStops, new StopsNameComparator());
	        	break;
	        case Autobuser.SORT_BY_DISTANCE:
	        	Collections.sort(myStops, new StopsNameComparator());
	        	Collections.sort(myStops, new StopsDistanceComparator());
	        	break;
        }
        setListAdapter(new StopsListAdapter(this, myStops));
	}
	
	protected void refreshTimes() {
		ListView list = getListView();
		TextView item;
		try {
		int size = myStops.size();
		for(int i = 0; i < size; i++)
		{
			item = ((TextView)((View)list.getChildAt(i)).findViewById(R.id.departures));
			if(item != null &&!( Timetable.isSwieto() ^ (myStops.get(i).getTimetable().getType()==Timetable.TYPE_S) )) {
				String deps = myStops.get(i).getTimetable().getNearestDepartureTimeToString(3);
				if(deps.equals(""))
					deps = getResources().getString(R.string.noDeparturesToday);
				else
					deps = getResources().getString(R.string.departureIn)+deps;
				item.setText(Html.fromHtml(deps));
			}
		}
		} catch (Exception e) {}
	}

	protected void redrawGeoViews() {
		ListView list = getListView();
		int size = list.getChildCount();
		for(int i = 0; i < size; i++)
		{
			GeoView item = ((GeoView)((View)list.getChildAt(i)).findViewById(R.id.geo));
			if(item != null)
				item.updateGeoData(mOrientation, mLocation);
		}
	}

	public void locationChanges(Location newLocation) {
		mLocation = newLocation;
		redrawGeoViews();
	}

	public void minuteChanges() {
		refreshTimes();
	}

	public void orientationChanges(float[] newOrientation) {
		mOrientation = newOrientation;
		redrawGeoViews();
	}
}
