package pl.skyman.autobuser;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StopsListAdapter extends BaseAdapter {

	private Context context;
	private List<MyStop> stops;

    public StopsListAdapter(Context cx, List<MyStop> stops)
    {
    	context = cx;
    	this.stops = stops;
    }
    
	public int getCount() {
		return stops.size();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int i) {
		return i;
	}

	public View getView(int i, View arg1, ViewGroup arg2) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate( R.layout.mystop, null );
		((TextView)v.findViewById(R.id.title)).setText(stops.get(i).toString());
		String deps = stops.get(i).getTimetable().getNearestDepartureTimeToString(3);
		if(deps.equals(""))
			deps = context.getResources().getString(R.string.noDeparturesToday);
		else
			deps = context.getResources().getString(R.string.departureIn)+deps;
		((TextView)v.findViewById(R.id.departures)).setText(Html.fromHtml(deps));
		((GeoView)v.findViewById(R.id.geo)).setLocation(stops.get(i).getLocation());
		
		return v;
	}
}