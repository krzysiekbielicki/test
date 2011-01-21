package pl.skyman.autobuser;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MultiLineWidgetSettings extends ListActivity {

	private int widgetId;
	private boolean isNew = true;
	private int icon;
	private Cursor d;
	private String stops;
	private StopsAdapter adapter;
	private AlertDialog ad;
	public static int[] icons = {R.drawable.metro, R.drawable.praca, R.drawable.dom, R.drawable.icon};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget2settings);
		onNewIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		widgetId = intent.getIntExtra("widgetId", 0);
		Cursor c = managedQuery(DatabaseProvider.CONTENT_URI_MULTILINEWIDGET, new String[] {MultiLineWidget.SQL_KEY_ICON, MultiLineWidget.SQL_KEY_NAME, MultiLineWidget.SQL_KEY_STOPS}, MultiLineWidget.SQL_KEY_ID+"="+widgetId, null, null);
		icon = R.drawable.icon;
		String name = "";
		stops = "";
		
		if(c.moveToNext()) {
			isNew = false;
			icon = c.getInt(0);
			name = c.getString(1);
			stops = c.getString(2);
		}
		d = managedQuery(DatabaseProvider.CONTENT_URI, new String[] {"("+DatabaseProvider.STOP_ID+" || '|' || "+DatabaseProvider.LINE_ID+") AS _id", DatabaseProvider.MYSTOP_NAME}, null, null, DatabaseProvider.MYSTOP_NAME);
		adapter = new StopsAdapter(this, d, stops );
		setListAdapter(adapter);
		
		//c.close();
		ImageButton button = ((ImageButton)findViewById(R.id.icon));
		button.setImageDrawable(getResources().getDrawable(icon));
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showIconsDialog();
			}
		});
		((EditText)findViewById(R.id.name)).setText(name);
	}
	
	private void showIconsDialog() {
		ad = new AlertDialog.Builder(this).setView(getIconsView()).create();
		ad.show();
	}
	
	private View getIconsView() {
		GridView v = new GridView(this);
		v.setNumColumns(3);
		v.setAdapter(new IconAdapter(this));
		return v;
	}

	protected void setIcon(int ico) {
		icon = ico;
		((ImageButton)findViewById(R.id.icon)).setImageDrawable(getResources().getDrawable(icon));
		ad.dismiss();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
			ContentValues values = new ContentValues();
			values.put(MultiLineWidget.SQL_KEY_ID, widgetId);
			values.put(MultiLineWidget.SQL_KEY_ICON, icon);
			values.put(MultiLineWidget.SQL_KEY_NAME, ((EditText)findViewById(R.id.name)).getText().toString());
			values.put(MultiLineWidget.SQL_KEY_STOPS, adapter.stops);
			if(isNew)
				getContentResolver().insert(DatabaseProvider.CONTENT_URI_MULTILINEWIDGET, values);
			else
				getContentResolver().update(DatabaseProvider.CONTENT_URI_MULTILINEWIDGET, values, MultiLineWidget.SQL_KEY_ID+"="+widgetId, null);
			Intent updateIntent = new Intent();
	        updateIntent.setClass(this, Widget2ReceiverUpdateService.class);
			startService(updateIntent);
			//AppWidgetManager awm = AppWidgetManager.getInstance(this);
			//awm.updateAppWidget(new ComponentName(this, Widget2.class), new RemoteViews(getPackageName(), R.layout.widget2));
		}
		return super.onKeyDown(keyCode, event);
	}
	
	class StopsAdapter implements ListAdapter {

		private Context cx;
		private Cursor c;
		private String stops;

		public StopsAdapter(Context cx, Cursor c, String stops) {
			this.cx = cx;
			this.c = c;
			this.stops = stops;
		}
		
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int position) {
			return true;
		}

		public int getCount() {
			return c.getCount();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = ((LayoutInflater)cx.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.checked_text_view, null);
			CheckBox ckb = (CheckBox)v.findViewById(android.R.id.text1);
			c.moveToPosition(position);
			final String id = c.getString(0);
			String name = c.getString(1);
			ckb.setText(name);
			ckb.setChecked(stops.indexOf(id)>=0);
			ckb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked) {
						if(stops.indexOf(id)<0)
							stops += " "+id;
					} else {
						stops = stops.replaceAll(id, "");
					}
					stops = stops.replaceAll("  ", "");
				}
			});
			return v;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return false;
		}

		public void registerDataSetObserver(DataSetObserver observer) {}

		public void unregisterDataSetObserver(DataSetObserver observer) {}
		
	}
	
	class IconAdapter implements ListAdapter {

		private Context cx;
		
		public IconAdapter(Context cx) {
			this.cx = cx;
		}
		
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int position) {
			return true;
		}

		public int getCount() {
			return icons.length;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ImageView v = new ImageView(cx);
			v.setImageDrawable(getResources().getDrawable(icons[position]));
			v.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setIcon(icons[position]);
				}
			});
			return v;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return false;
		}

		public void registerDataSetObserver(DataSetObserver observer) {}

		public void unregisterDataSetObserver(DataSetObserver observer) {}
		
	}
}
