package pl.skyman.autobuser;

import android.content.Context;
import android.text.Html;
import android.widget.Button;

public class DepartureMinuteView extends Button {

	private int minutes;
	
	public DepartureMinuteView(final Context context, Departure dep, boolean highlight) {
		super(context, null);
		this.minutes = dep.minute;
		setTextColor(getResources().getColor(R.color.white));
		setHeight(40);
		setWidth(40);
		int minute = minutes%60;
		setText(Html.fromHtml((dep.isLowFloor?"<u>":"")+(minute<10?"0":"")+minute+(dep.isLowFloor?"</u>":"")+dep.modifier));
		if(highlight)
			setBackgroundColor(getResources().getColor(R.color.red));
		else
			setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.list_selector_background));
	}

	public int getMinutes()
	{
		return minutes;
	}
}