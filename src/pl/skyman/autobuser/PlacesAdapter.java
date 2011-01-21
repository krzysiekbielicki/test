package pl.skyman.autobuser;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;


public class PlacesAdapter implements SpinnerAdapter {

	private Context context;
	private Cursor cursor;

	public PlacesAdapter(Context context, Cursor c) {
		this.context = context;
		this.cursor = c;
	}

	public View getDropDownView(int arg0, View arg1, ViewGroup arg2) {
		return null;
	}

	public int getCount() {
		return cursor.getCount()+1;
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	public int getItemViewType(int arg0) {
		return android.R.layout.simple_spinner_dropdown_item;
	}

	public View getView(int i, View arg1, ViewGroup arg2) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate( getItemViewType(i), null );
		TextView tv = (TextView) v.findViewById(android.R.id.text1);
		if(i == 0)
			tv.setText(R.string.setDestination_spinnerHint);
		else {
			cursor.moveToPosition(i-1);
			tv.setText(cursor.getString(0));
		}
		return v;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public boolean hasStableIds() {
		return false;
	}

	public boolean isEmpty() {
		return false;
	}

	public void registerDataSetObserver(DataSetObserver arg0) {
	}

	public void unregisterDataSetObserver(DataSetObserver arg0) {
	}


}
