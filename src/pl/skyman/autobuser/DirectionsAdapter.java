package pl.skyman.autobuser;

import java.util.LinkedList;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

public class DirectionsAdapter implements ListAdapter {

	private Context context;
	private LinkedList<Waypoint> points;
	private Route callback;

	public DirectionsAdapter(Context context, LinkedList<Waypoint> points, Route callback) {
		this.context = context;
		this.points = points;
		this.callback = callback;
	}

	public boolean areAllItemsEnabled() {
		return true;
	}

	public boolean isEnabled(int i) {
		return true;
	}

	public int getCount() {
		return points.size();
	}

	public Waypoint getItem(int i) {
		return points.get(i);
	}

	public long getItemId(int i) {
		return i;
	}

	public int getItemViewType(int i) {
		return R.layout.directionrecord;
	}

	public View getView(final int i, View parentview, ViewGroup group) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Waypoint point = getItem(i);
		if(point.stopno.equals("@HEAD")) {
			View v = inflater.inflate( R.layout.routeheader, null );
			((TextView)v.findViewById(android.R.id.text1)).setText(String.format(context.getResources().getString(R.string.lineRoute), point.stopname));
			((TextView)v.findViewById(android.R.id.text2)).setText(point.linename);
			((TextView)v.findViewById(R.id.text3)).setText(" ("+point.travelTime+"min)");
			((ImageButton)v.findViewById(R.id.map)).setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					callback.showMap(i);
				}
			});
			return v;
		}
		View v = inflater.inflate( getItemViewType(i), null );
		String s;
		if(point.end) {
			if(point.stopno.equals("@GPS")) {
				((TextView)v.findViewById(R.id.directionrecord_type)).setVisibility(View.GONE);
				((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(context.getString(R.string.gpsPosition));
			} else if(point.stopno.equals("@ADR")) {
				((TextView)v.findViewById(R.id.directionrecord_type)).setText(context.getString(R.string.address));
				((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(point.stopname);
			} else {
				((TextView)v.findViewById(R.id.directionrecord_type)).setText(context.getString(R.string.stop));
				((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(point.stopname);
			}
			s = context.getString(R.string.directionrecord_end);
		} else {
			((TextView)v.findViewById(R.id.directionrecord_type)).setText(context.getString(R.string.stop));
			if(point.stopno.equals("@GPS")) {
				if(!point.start) {
					((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(point.stopname);
					s =  String.format(context.getString(R.string.directionrecord_getoff), (point.departure/60)+":"+(point.departure%60<10?"0"+point.departure%60:point.departure%60))+", "+String.format(context.getString(R.string.directionrecord_walk), point.travelTime);
				} else {
					((TextView)v.findViewById(R.id.directionrecord_type)).setVisibility(View.GONE);
					((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(context.getString(R.string.gpsPosition));
					s = String.format(context.getString(R.string.directionrecord_walk), point.travelTime);
				}
			} else if(point.stopno.equals("@ADR")) {
				((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(point.stopname);
				if(!point.start) {
					s =  String.format(context.getString(R.string.directionrecord_getoff), (point.departure/60)+":"+(point.departure%60<10?"0"+point.departure%60:point.departure%60))+", "+String.format(context.getString(R.string.directionrecord_walk), point.travelTime);
				} else {
					((TextView)v.findViewById(R.id.directionrecord_type)).setText(context.getString(R.string.address));
					s = String.format(context.getString(R.string.directionrecord_walk), point.travelTime);
				}
			}else {
				((TextView)v.findViewById(R.id.directionrecord_stopname)).setText(point.stopname+(point.stopno.equals("0")?"":" "+point.stopno));
				int minutes = (point.departure%60);
				s = String.format(context.getString(point.start?R.string.directionrecord_geton:R.string.directionrecord_change), (point.departure/60)+":"+(minutes<10?"0"+minutes:minutes));
				if(point.stopno.equals("0"))
					if(point.stopsNumber == 1)
						s+=" "+String.format(context.getString(R.string.directionrecord_underground1), point.direction, point.stopsNumber);
					else if(point.stopsNumber > 1 && point.stopsNumber < 5)
						s+=" "+String.format(context.getString(R.string.directionrecord_underground234), point.direction, point.stopsNumber);
					else
						s+=" "+String.format(context.getString(R.string.directionrecord_underground5up), point.direction, point.stopsNumber);
				else {
					((TextView)v.findViewById(R.id.directionrecord_line)).setText(" "+point.linename);
					((TextView)v.findViewById(R.id.directionrecord_time)).setText(" ("+point.travelTime+"min)");
				}
			}
		}
		((TextView)v.findViewById(R.id.directionrecord_string)).setText(s);
		return v;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public boolean hasStableIds() {
		return false;
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	public void registerDataSetObserver(DataSetObserver arg0) {
	}

	public void unregisterDataSetObserver(DataSetObserver arg0) {
	}

}
