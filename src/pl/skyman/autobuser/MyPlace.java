package pl.skyman.autobuser;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MergeCursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MyPlace extends ListActivity implements AutobuserActivity {

	public static final String SQL_TABLE_NAME = "myplaces";
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_VALUE = "value";
	public static final String SQL_KEY_FAVOURITE = "fav";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
													SQL_TABLE_NAME + " (" + 
													SQL_KEY_ID + " integer primary key autoincrement, " +
													SQL_KEY_NAME + " text not null, " +
													SQL_KEY_VALUE + " text not null, " +
													SQL_KEY_FAVOURITE + " boolean default false, " +
													"CONSTRAINT U_MyStop UNIQUE ("+SQL_KEY_NAME+", "+SQL_KEY_VALUE+"));";
	private String lastValue = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myplaces);
		((Button)findViewById(R.id.add)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				showAddDialog();
			}
		});
		getListView().setFocusable(false);
		getListView().setItemsCanFocus(true);
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	//	itemPos = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		menu.add(R.string.seeOnMap);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//Waypoint w = ((DirectionsAdapter)((ListView)findViewById(android.R.id.list)).getAdapter()).getItem( itemPos );
		Intent i = new Intent(MyPlace.this, AutobuserMap.class);
		//i.putExtra("latE6", (int) (w.lat * 1E6));
		//i.putExtra("lonE6", (int) (w.lon * 1E6));
		//i.putExtra("no", w.stopno+"");
		getParent().startActivity(i);
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshMyPlacesList();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
		
	private void refreshMyPlacesList() {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_MYPLACE, new String[] {SQL_KEY_ID, SQL_KEY_NAME, SQL_KEY_VALUE, SQL_KEY_FAVOURITE}, null, null, SQL_KEY_NAME+" ASC");
		startManagingCursor(c);
		setListAdapter(new PlacesAdapter(this, c));
	}
	
	private class PlacesAdapter extends BaseAdapter {

		private Cursor cursor;

		public PlacesAdapter(Context context, Cursor c) {
			this.cursor = c;
		}
		
		public int getCount() {
			return cursor.getCount();
		}

		public Cursor getItem(int i) {
			cursor.moveToPosition(i);
			return cursor;
		}

		public long getItemId(int i) {
			return getItem(i).getLong(0);
		}

		public View getView(final int i, View arg1, ViewGroup arg2) {
			View v = getLayoutInflater().inflate(R.layout.placerecord, null);
			Cursor item = getItem(i);
			((ImageButton)v.findViewById(R.id.button)).setImageDrawable(getResources().getDrawable(item.getInt(3) == 0?android.R.drawable.btn_star_big_off:android.R.drawable.btn_star_big_on));
			((ImageButton)v.findViewById(R.id.button)).setOnClickListener(new OnClickListener(){
				public void onClick(View view) {
					toggleFavourite(i, (ImageButton) view);
				}
			});
			((LinearLayout)v.findViewById(R.id.header)).setOnClickListener(new OnClickListener(){
				public void onClick(View arg0) {
					Intent in = new Intent(MyPlace.this, Route.class);
					in.putExtra("start", "@GPS");
					in.putExtra("destination", getItem(i).getString(2));
					((Autobuser)getParent()).startActivity(in);
				}});
			((LinearLayout)v.findViewById(R.id.header)).setOnLongClickListener(new OnLongClickListener(){
				public boolean onLongClick(View arg0) {
					showMenu(i);
					return true;
				}});
			
			((TextView)v.findViewById(android.R.id.text1)).setText(item.getString(1));
			((TextView)v.findViewById(android.R.id.text2)).setText(item.getString(2));
			return v;
		}		
	}
	
	private void showMenu(final int i) {
		//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.addAlarmMinutes, android.R.layout.select_dialog_item);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);
		adapter.add(getResources().getString(R.string.deleteFromFavourites));
		
		new AlertDialog.Builder(this)
		.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case 0:
					getContentResolver().delete(DatabaseProvider.CONTENT_URI_MYPLACE, SQL_KEY_ID+"="+((Cursor)getListAdapter().getItem(i)).getLong(0), null);
					refreshMyPlacesList();
					break;
				}
			}
		})
		.create().show();
	}

	protected void toggleFavourite(int i, ImageButton v) {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_MYPLACE, new String[] {SQL_KEY_FAVOURITE}, SQL_KEY_ID+"="+((Cursor)getListAdapter().getItem(i)).getLong(0), null, null);
		if(c.moveToFirst() && c.getInt(0) == 0) {
			c.close();
			c = getContentResolver().query(DatabaseProvider.CONTENT_URI_MYPLACE, new String[] {"COUNT("+SQL_KEY_ID+")"}, SQL_KEY_FAVOURITE+"=1", null, null);
			if(c.moveToFirst() && c.getInt(0) == 5) {
				((Autobuser)getParent()).toast(R.string.favMaxExceeded, Toast.LENGTH_SHORT);
			} else {
				ContentValues values = new ContentValues();
				values.put(SQL_KEY_FAVOURITE, 1);
				getContentResolver().update(DatabaseProvider.CONTENT_URI_MYPLACE, values, SQL_KEY_ID+"="+((Cursor)getListAdapter().getItem(i)).getLong(0), null);
				v.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_on));
			}
			c.close();
		} else {
			c.close();
			ContentValues values = new ContentValues();
			values.put(SQL_KEY_FAVOURITE, 0);
			getContentResolver().update(DatabaseProvider.CONTENT_URI_MYPLACE, values, SQL_KEY_ID+"="+((Cursor)getListAdapter().getItem(i)).getLong(0), null);
			v.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_off));
		}
		//refreshMyPlacesList();
	}
	
	private void showAddDialog() {
		final View v = getLayoutInflater().inflate(R.layout.addplace, null);
		((ImageButton)v.findViewById(R.id.button)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				Location mLocation = ((Autobuser)getParent()).getLocation();
				((AutoCompleteTextView)v.findViewById(R.id.atv)).setText(mLocation.getLatitude()+", "+mLocation.getLongitude());
			}
		});
		AutoCompleteTextView atv = (AutoCompleteTextView) v.findViewById(R.id.atv);
		
		Cursor places = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, null, null, null);
		//.rawQuery("SELECT "+ZiL.SQL_KEY_ID+","+ZiL.SQL_KEY_NAME+",'' FROM "+ZiL.SQL_TABLE_NAME+" LIMIT 1;", null);//MyPlace.SQL_TABLE_NAME, new String[] {MyPlace.SQL_KEY_ID, MyPlace.SQL_KEY_NAME, MyPlace.SQL_KEY_VALUE}, null, null, null, null, null);
        startManagingCursor(places);
        
        Cursor contacts = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
        startManagingCursor(contacts);
        DestinationListAdapter placesAdapter = new DestinationListAdapter(this, new MergeCursor(new Cursor[] {contacts, places}));

		atv.setAdapter(placesAdapter);
		atv.setText(lastValue);
		new AlertDialog.Builder(this)
		.setView(v)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(((EditText)v.findViewById(R.id.text)).getText().toString().equals("")) {
					toast(R.string.nameCannotBeEmpty);
					lastValue = ((EditText)v.findViewById(R.id.atv)).getText().toString();
					showAddDialog();
					return;
				}
				ContentValues values = new ContentValues();
				values.put(SQL_KEY_NAME, ((EditText)v.findViewById(R.id.text)).getText().toString());
				values.put(SQL_KEY_VALUE, ((EditText)v.findViewById(R.id.atv)).getText().toString());
				getContentResolver().insert(DatabaseProvider.CONTENT_URI_MYPLACE, values);
				lastValue = "";
				refreshMyPlacesList();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		})
		.create().show();
	}
	
	protected void toast(int resId) {
		Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
	}

	private class DestinationListAdapter extends BaseAdapter implements Filterable, CursorFilterClient{

		private Cursor places;
		private LayoutInflater inflater;
		private DestinationFilter mCursorFilter;
		
		public DestinationListAdapter(Context context, Cursor places) {
			this.places = places;
			this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return places.getCount();
		}

		public Cursor getItem(int i) {
			places.moveToPosition(i);
			return places;
		}

		public long getItemId(int i) {
			places.moveToPosition(i);
			return places.getInt(1);
		}

		public int getItemViewType(int i) {
			places.moveToPosition(i);
			return R.layout.dropdown_item_2lines;
		}

		public View getView(int i, View arg1, ViewGroup arg2) {
			View v = inflater.inflate( getItemViewType(i), null );
			Cursor place = getItem(i);
			((TextView)v.findViewById(android.R.id.text1)).setText(place.getString(1));
			
			if(place.getColumnCount() == 2)
				((TextView)v.findViewById(R.id.text2)).setVisibility(TextView.GONE);
			else
				((TextView)v.findViewById(R.id.text2)).setText(place.getString(2).replaceAll("\n", " "));
			return v;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isEmpty() {
			return places.getCount()==0;
		}

		public void registerDataSetObserver(DataSetObserver arg0) {
			super.registerDataSetObserver(arg0);
		}

		public void unregisterDataSetObserver(DataSetObserver arg0) {
			super.unregisterDataSetObserver(arg0);
		}

		public Filter getFilter() {
			if (mCursorFilter == null) {
				 mCursorFilter = new DestinationFilter(this);
			}
			return (Filter) mCursorFilter;
		}

		public void changeCursor(Cursor cursor) {
			//if (cursor == places) {
	        //    return;
	        //}
	        if (places != null) {
	        	//places.unregisterContentObserver(mChangeObserver);
	        	//places.unregisterDataSetObserver(mDataSetObserver);
	        	places.close();
	        }
	        places = cursor;
	        if (cursor != null) {
	            //cursor.registerContentObserver(mChangeObserver);
	            //cursor.registerDataSetObserver(mDataSetObserver);
	            //mRowIDColumn = cursor.getColumnIndexOrThrow("_id");
	            //mDataValid = true;
	            // notify the observers about the new cursor
	        	//cursor.close();
	            notifyDataSetChanged();
	            //notifyDataSetInvalidated();
	        } else {
	            //mRowIDColumn = -1;
	            //mDataValid = false;
	            // notify the observers about the lack of a data set
	            notifyDataSetInvalidated();
	        }
	     //   text.setAdapter(new DestinationListAdapter(context, cursor));
	        //text.clearListSelection();
		}

		public CharSequence convertToString(Cursor cursor) {
			if(cursor == null)
				return "";
			if(cursor.getColumnCount() == 2)
				return cursor.getString(1).replaceAll("\n", " ");
			
			return cursor.getString(2).replaceAll("\n", " ");
		}

		public Cursor getCursor() {
			return places;
		}
	}
	private class DestinationFilter extends Filter {
		
		private CursorFilterClient client;

		public DestinationFilter(CursorFilterClient client) {
			this.client = client;
		}
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			String like = "";
            if (constraint != null) {
            	like+=constraint;
            }
            Cursor cursor = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, "UPPER("+ZiL.SQL_KEY_NAME+") LIKE '"+like.toUpperCase()+"%'", null, ZiL.SQL_KEY_NAME+" ASC");
            //Cursor cursor = db.rawQuery("SELECT "+ZiL.SQL_KEY_ID+","+ZiL.SQL_KEY_NAME+",'' FROM "+ZiL.SQL_TABLE_NAME+" WHERE  LIMIT 25;", null);//MyPlace.SQL_TABLE_NAME, new String[] {MyPlace.SQL_KEY_ID, MyPlace.SQL_KEY_NAME, MyPlace.SQL_KEY_VALUE}, null, null, null, null, null);
            Cursor contacts = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '"+like.toUpperCase()+"%' OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '% "+like.toUpperCase()+"%') AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            //Cursor cursor = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '"+like.toUpperCase()+"%' OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '% "+like.toUpperCase()+"%') AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            Cursor mergedCursor = new MergeCursor(new Cursor[] {cursor, contacts});
            //Cursor mergedCursor = cursor;
			FilterResults results = new FilterResults();
			if (mergedCursor != null) {
				results.count = mergedCursor.getCount();
				results.values = mergedCursor;
			} else {
				results.count = 0;
				results.values = null;
			}
			
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			 Cursor oldCursor = client.getCursor();
		        
		        if (results.values != null && results.values != oldCursor)
		        	client.changeCursor((Cursor) results.values);
		}
		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return client.convertToString((Cursor) resultValue);
		}
		
		
        		
	}
	interface CursorFilterClient {
        CharSequence convertToString(Cursor cursor);
        Cursor getCursor();
        void changeCursor(Cursor cursor);
    }

	public void locationChanges(Location newLocation) {
	}

	public void minuteChanges() {
	}

	public void orientationChanges(float[] newOrientation) {
	}
	
	public static class Place {
		private String name;
		private String value;
		public Place( String name, String value) {
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}
}
