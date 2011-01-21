package pl.skyman.autobuser;

import java.util.LinkedList;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ZiLStopsListAdapter implements ListAdapter {

	private LinkedList<ZiLStop> stops;
	private Context context;
	private int rowResID;
	private boolean zespoly;
	private ZiLListener listener;

	public ZiLStopsListAdapter(Context cx, int rowResID, LinkedList<ZiLStop> stops, boolean zespoly, ZiLListener zill) {
		context = cx;
		this.rowResID = rowResID;
    	this.stops = stops;
    	this.zespoly = zespoly;
    	this.listener = zill;
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int arg0) {
		return true;
	}

	public int getCount() {
		return stops.size();
	}

	public Object getItem(int i) {
		return stops.get(i);
	}

	public long getItemId(int i) {
		return i;
	}

	public int getItemViewType(int arg0) {
		return 0;
	}

	public View getView(final int i, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate( rowResID, null );
		ZiLStop stop = stops.get(i);
		if(zespoly)
			((TextView)v.findViewById(R.id.ZiLhead)).setText(stop.linia);
		else {
			((TextView)v.findViewById(R.id.ZiLhead)).setVisibility(View.GONE);
			((TextView)v.findViewById(R.id.no)).setText((i+1)+"");
		}
		
		Button B1 = ((Button)v.findViewById(R.id.B1));
		Button B2 = ((Button)v.findViewById(R.id.B2));
		B1.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				listener.onClick(i, 0);
			}
		});
		
		B2.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				listener.onClick(i, 1);
			}
		});
		
		if(stop.dir1.kierunek!=null)
			B1.setText(stop.dir1.kierunek);
			//B1.setText("<-"+stop.dir1.id_przystanku);
		else {
			B1.setVisibility(View.INVISIBLE);
		}
		if(stop.dir2.kierunek!=null)
			B2.setText(stop.dir2.kierunek);
			//B2.setText(stop.dir2.id_przystanku+"->");
		else
			B2.setVisibility(View.INVISIBLE);
		return v;
	}

	public int getViewTypeCount() {
		return 2;
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
