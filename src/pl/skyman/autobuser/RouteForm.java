package pl.skyman.autobuser;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MergeCursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class RouteForm extends Activity implements AutobuserActivity {
	private TimePickerDialog.OnTimeSetListener mTimeSetListener =
	    new TimePickerDialog.OnTimeSetListener() {
	        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	            time = hourOfDay*60+minute;
	            updateTimeText();
	        }
	    };
	protected static final int TIME_DIALOG_ID = 0;
	int time;
	private AutoCompleteTextView from;
	private AutoCompleteTextView to;
	private Spinner spinner;
	private Button timepicker;
	
	@Override
	protected void onResume() {
		getParent().setTitle(R.string.directions);
		refreshAutocomplete();
		refreshHistory();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.routeform);
		
		Calendar c = Calendar.getInstance();
		time = ((c.get(Calendar.HOUR_OF_DAY)*60)+c.get(Calendar.MINUTE)+5)%1440;
		
		from = (AutoCompleteTextView)findViewById(R.id.setRoute_From_text);
		to = (AutoCompleteTextView)findViewById(R.id.setRoute_To_text);
		spinner = (Spinner)findViewById(R.id.spinner);
		timepicker = (Button)findViewById(R.id.time);
		updateTimeText();
		timepicker.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				showDialog(TIME_DIALOG_ID);
			}
		});
		((Button)findViewById(R.id.setRoute_ok)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				findRoute();	
			}
        });
		((ImageButton)findViewById(R.id.setRoute_from)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				showButtonMenu(R.id.setRoute_from);
			}
        });
		((ImageButton)findViewById(R.id.setRoute_to)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				showButtonMenu(R.id.setRoute_to);
			}
        });
		((ListView)findViewById(R.id.history)).setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,		long id) {
				Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, new String[] {Route.SQL_KEY_FROM, Route.SQL_KEY_TO}, Route.SQL_KEY_ID+"="+id, null, null);
				if(!c.moveToFirst()) {
					c.close();
					return;
				}
				from.setText(c.getString(0));
				to.setText(c.getString(1));
				c.close();
			}
			
        });
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.departureTimeSpinner));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		((Spinner)findViewById(R.id.spinner)).setAdapter(adapter);
		from.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					int pos = from.getListSelection();
					Cursor c;
					if(from.getAdapter().isEmpty())
						return true;
					if(pos == ListView.INVALID_POSITION)
						if(from.getAdapter().getCount() > 0)
							c = (Cursor)from.getAdapter().getItem(0);
						else {
							findRoute();
							return true;
						}
					else
						c = (Cursor)from.getAdapter().getItem(pos);
					String s = c.getString(2);
					if(s.equals(""))
						from.setText(c.getString(1));
					else
						from.setText(s);
					findRoute();
					return true;
				}
				return false;
			}
		});
		to.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					int pos = to.getListSelection();
					Cursor c;
					if(pos == ListView.INVALID_POSITION)
						if(to.getAdapter().getCount() > 0)
							c = (Cursor)to.getAdapter().getItem(0);
						else {
							findRoute();
							return true;
						}
					else
						c = (Cursor)to.getAdapter().getItem(pos);
					String s = c.getString(2);
					if(s.equals(""))
						to.setText(c.getString(1));
					else
						to.setText(s);
					findRoute();
					return true;
				}
				return false;
			}
		});
	}
	
	protected void findRoute() {
		String start = from.getText().toString();
		String destination = to.getText().toString();
		if(start.equals(""))
			from.requestFocus();
		else if(destination.equals(""))
			to.requestFocus();
		else {
			hideIME();
			Intent i = new Intent(RouteForm.this, Route.class);
			i.putExtra("start", start);
			i.putExtra("destination", destination);
			i.putExtra("odjazd", spinner.getSelectedItemPosition() == 0);
			i.putExtra("time", time);
			getParent().startActivity(i);
		}
	}

	protected void hideIME() {
		if(this.getCurrentFocus() != null)
			((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private void showButtonMenu(final int source) {
		//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.addAlarmMinutes, android.R.layout.select_dialog_item);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);
		//adapter.add(getResources().getString(favourite?R.string.deleteFromFavourites:R.string.addToFavourites));
		adapter.add(getResources().getString(R.string.swapDirections));
		adapter.add(getResources().getString(R.string.gpsPosition));
		adapter.add(getResources().getString(R.string.contacts));
		final Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_MYPLACE, new String[] {MyPlace.SQL_KEY_NAME, MyPlace.SQL_KEY_VALUE}, null, null, MyPlace.SQL_KEY_FAVOURITE+" DESC, "+MyPlace.SQL_KEY_NAME+" ASC");
		startManagingCursor(c);
		while(c.moveToNext())
			adapter.add(c.getString(0));
		//adapter.add(getResources().getString(R.string.seeOnMap));
		//adapter.add(getResources().getString(R.string.updateTimetable));
		new AlertDialog.Builder(this)
		.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0) {
					String tmp = from.getText().toString();
					from.setText(to.getText().toString());
					to.setText(tmp);
				} else if(which == 1) {
					if(source == R.id.setRoute_to) {
						to.setText(getResources().getString(R.string.gpsPosition)+"@GPS");
						if(from.getText().toString().indexOf("@GPS")>=0) {
							from.setText("");
							from.requestFocus();
						}
					} else {
						from.setText(getResources().getString(R.string.gpsPosition)+"@GPS");
						if(to.getText().toString().indexOf("@GPS")>=0) {
							to.setText("");
							to.requestFocus();
						}
					}
				} else if(which == 2) {
					if(source == R.id.setRoute_to) {
						to.setHint(R.string.typeContactName);
						to.requestFocus();
					} else {
						from.setHint(R.string.typeContactName);
						from.requestFocus();
					}
				} else {
					c.moveToPosition(which - 3);
					if(source == R.id.setRoute_to)
						to.setText(c.getString(1));
					else
						from.setText(c.getString(1));
				}
			}
		})
		.create().show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
	
	protected void updateTimeText() {
		timepicker.setText((time/60)+":"+(time%60<10?"0"+time%60:time%60));

	}

	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case TIME_DIALOG_ID:
	        return new TimePickerDialog(this, mTimeSetListener, time/60, time%60, true);
	    }
	    return null;
	}
	
	private void refreshHistory() {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, new String[] {Route.SQL_KEY_ID, Route.SQL_KEY_FROM, Route.SQL_KEY_TO}, null, null, Search.SQL_KEY_TIME+" DESC");
		startManagingCursor(c);
		if(c.moveToFirst()) {
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.routehistoryrecord, c, new String[] {Route.SQL_KEY_FROM, Route.SQL_KEY_TO}, new int[] {android.R.id.text1, android.R.id.text2});
	        ((ListView)findViewById(R.id.history)).setAdapter(adapter);
	        ((View)findViewById(R.id.history)).setVisibility(View.VISIBLE);
	        ((View)findViewById(R.id.empty)).setVisibility(View.GONE);
        } else {
        	((View)findViewById(R.id.history)).setVisibility(View.GONE);
	        ((View)findViewById(R.id.empty)).setVisibility(View.VISIBLE);
        }
	}
	
	private void refreshAutocomplete() {
		Cursor places = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, null, null, null);
		startManagingCursor(places);
        
        Cursor contacts = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
        startManagingCursor(contacts);
        DestinationListAdapter placesAdapter = new DestinationListAdapter(this, new MergeCursor(new Cursor[] {contacts, places}));
		from.setAdapter(placesAdapter);
		to.setAdapter(placesAdapter);
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
				((TextView)v.findViewById(R.id.text2)).setText(place.getString(2).replaceAll("\n", " ").trim());
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
            like = like.toUpperCase();
            Cursor cursor = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, "UPPER("+ZiL.SQL_KEY_NAME+") LIKE '"+like.toUpperCase()+"%'", null, ZiL.SQL_KEY_NAME+" ASC");
            //Cursor cursor = db.rawQuery("SELECT "+ZiL.SQL_KEY_ID+","+ZiL.SQL_KEY_NAME+",'' FROM "+ZiL.SQL_TABLE_NAME+" WHERE  LIMIT 25;", null);//MyPlace.SQL_TABLE_NAME, new String[] {MyPlace.SQL_KEY_ID, MyPlace.SQL_KEY_NAME, MyPlace.SQL_KEY_VALUE}, null, null, null, null, null);
            Cursor contacts = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '"+like.toUpperCase()+"%' OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '% "+like.toUpperCase()+"%') AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            
            //Cursor cursor = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, "UPPER("+ZiL.SQL_KEY_NAME+") LIKE '"+like.toUpperCase()+"%'", null, ZiL.SQL_KEY_NAME+" ASC");
            //Cursor contacts = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('"+like+"%') OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('% "+like+"%')) AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            //Cursor cursor = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '"+like.toUpperCase()+"%' OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE '% "+like.toUpperCase()+"%') AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            Cursor mergedCursor = new MergeCursor(new Cursor[] {contacts, cursor});
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

}
