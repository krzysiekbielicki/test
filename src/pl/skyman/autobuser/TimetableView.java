package pl.skyman.autobuser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class TimetableView extends TableLayout {
	Context context;
	private Paint borderPaint;
	private Timetable timetable;
	private OnClickListener dmvListener = new OnClickListener(){

		public void onClick(View view) {
			addAlarm((DepartureMinuteView)view);
		}
		
	};
	private LayoutInflater inflater;
	
	//Constructor required for inflation from resource file
	public TimetableView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		borderPaint = new Paint(Paint.DITHER_FLAG);
		borderPaint.setStrokeWidth(.5f);
		borderPaint.setColor(Color.WHITE);
		setFocusable(false);
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void toggleSecondTimetable() {
		timetable.toggleSecondTimetable();
		reinitView();
		invalidate();
	}
	
	public void setType(int type) {
		timetable.setType(type);
		//reinitView();
	}

	public void reinitView() {
		removeAllViewsInLayout();
		LinkedList<Departure> departures = timetable.getDepartures();
		int size = departures.size();
		if(size == 0)
		{
			TextView tv = new TextView(context);
			tv.setHeight(40);
			tv.setGravity(Gravity.CENTER);
			tv.setText(R.string.noTimetableForSelectedDay);
			addView(tv);
			return;
		}
		int lastHour = -1;
		int hour;
		TableRow row = null;
		boolean highlight = !(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S));
		boolean highlightMorning = !(Timetable.isSwietoTomorrow() ^ (timetable.getType()==Timetable.TYPE_S));
		DepartureMinuteView dmv = null;
		Calendar c = Calendar.getInstance();
		int currentMinutes = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
		boolean scrolled = false;
		boolean hl;
		Departure dep;
		for(int i = 0; i< size; i++)
		{
			dep = departures.get(i);
			hour = dep.minute/60;
			if(hour != lastHour)
			{
				lastHour = hour;
				if(row != null) {
					addView(row);
				}
				
				row = new TableRow(context);
				TextView v = (TextView)inflater.inflate( R.layout.departurehourview, null );
				v.setText(hour+"");
				row.addView(v);
			}
			hl = ( (currentMinutes<dep.minute&&currentMinutes+30>dep.minute&&highlight) || (currentMinutes+30>dep.minute+1440 && highlightMorning) );
			dmv = new DepartureMinuteView(context, dep, hl);
			
			if(row.getChildCount() == 8) {
				addView(row);
				row = new TableRow(context);
				View v = new View(context);
				row.addView(v);
			}
			row.addView(dmv);
			if( hl && !scrolled) {
				((ScrollView)getParent()).smoothScrollTo(0, getChildCount()*40-100);
				scrolled = true;
			}
			dmv.setOnClickListener( dmvListener );
		}
		if(row != null)
			addView(row);
		if( !scrolled) {
			((ScrollView)getParent()).smoothScrollTo(0, 0);
		}
		invalidate();
	}

	public void setTimetable(Timetable child) {
		timetable = child;
	}
	
	private void addAlarm(final DepartureMinuteView view) {
		View v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.addalarm, null);
		ArrayAdapter<CharSequence> timeBefore = ArrayAdapter.createFromResource(context, R.array.addAlarmMinutes, android.R.layout.simple_spinner_item);
		timeBefore.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner timeSpinner = ((Spinner)v.findViewById(R.id.AddAlarmTimeSpinner));
		timeSpinner.setAdapter(timeBefore);
		final Spinner daysSpinner = ((Spinner)v.findViewById(R.id.AddAlarmDaySpinner));
		final CheckBox checkbox = ((CheckBox)v.findViewById(R.id.AddAlarmCheckboxRepeat));
		GregorianCalendar calendar = new GregorianCalendar();
		int dow = calendar.get(GregorianCalendar.DAY_OF_WEEK);
		if( ((calendar.get(GregorianCalendar.HOUR_OF_DAY)*60)+calendar.get(GregorianCalendar.MINUTE)) > view.getMinutes()) {
			if(dow == GregorianCalendar.SUNDAY)
				dow = GregorianCalendar.SATURDAY;
			else
				dow++;
		}
		if (!(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S)) && Timetable.isSwieto() && dow != GregorianCalendar.SATURDAY && dow != GregorianCalendar.SUNDAY)
		{
			daysSpinner.setEnabled(false);
			checkbox.setEnabled(false);
			checkbox.setText(R.string.noRepeatSpecialTimetable);
			//return;
		}
		ArrayAdapter<CharSequence> days = ArrayAdapter.createFromResource(context, timetable.getType() == Timetable.TYPE_S?R.array.addAlarmSDays:R.array.addAlarmPDays, android.R.layout.simple_spinner_item);
				
		days.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		daysSpinner.setAdapter(days);
		
		//if(calendar.getFirstDayOfWeek() == GregorianCalendar.MONDAY)
		//	dow = (dow-1)%5;
		//else
			dow = dow==1?1:dow<7?dow-2:0;
		
		if(Timetable.isSwieto() != (timetable.getType() == Timetable.TYPE_P))
			daysSpinner.setSelection(dow);
		new AlertDialog.Builder(context)
		.setView(v)
		.setIcon(android.R.drawable.ic_popup_reminder)
		.setTitle(view.getMinutes()/60+":"+(view.getMinutes()%60<10?"0"+view.getMinutes()%60:view.getMinutes()%60))
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				int day = daysSpinner.getSelectedItemPosition();
				Calendar c = Calendar.getInstance();
				day++;
				switch(timetable.getType())
				{
					case Timetable.TYPE_P:
						day++;
						break;
					case Timetable.TYPE_S:
						day = day == 2?1:7;
				}
				
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				c.add(Calendar.DAY_OF_MONTH, -(c.get(Calendar.DAY_OF_WEEK)-day));
				int minutes = view.getMinutes() - Integer.parseInt((String)timeSpinner.getSelectedItem());
				c.set(Calendar.HOUR_OF_DAY, minutes/60);
				c.set(Calendar.MINUTE, minutes%60);
				
				if(c.getTimeInMillis() < System.currentTimeMillis())
					c.add(Calendar.WEEK_OF_YEAR, 1);
				
				ContentValues values = new ContentValues();
				values.put(AutobuserAlert.SQL_KEY_MINUTES, view.getMinutes());
				values.put(AutobuserAlert.SQL_KEY_REPEAT, checkbox.isChecked());
				values.put(AutobuserAlert.SQL_KEY_TIME, c.getTimeInMillis());
				values.put(AutobuserAlert.SQL_KEY_DAY, day);
				
				Cursor x = context.getContentResolver().query(DatabaseProvider.CONTENT_URI_STOP, new String[] {Stop.SQL_KEY_NAME}, Stop.SQL_KEY_ID+"="+timetable.getStopid(), null, null);
				x.moveToFirst();
				values.put(AutobuserAlert.SQL_KEY_STOP, x.getString(0));
				x.close();
				
				x = context.getContentResolver().query(DatabaseProvider.CONTENT_URI_LINE, new String[] {Line.SQL_KEY_NAME}, Line.SQL_KEY_ID+"="+timetable.getLineid(), null, null);
				x.moveToFirst();
				values.put(AutobuserAlert.SQL_KEY_LINE, x.getString(0));
				x.close();
				
				context.getContentResolver().insert(DatabaseProvider.CONTENT_URI_ALERT, values);
				Toast.makeText(context, String.format(context.getResources().getString(R.string.AlarmWillRingAt), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime())), Toast.LENGTH_LONG).show();
				AutobuserAlert.setNextAlarm(context);
			}
			
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
			
		})
		.create().show();
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		//setTimetable(timetable);
		if(timetable == null)
			return;
		if(timetable.getDepartures() == null)
			return;
		
		super.dispatchDraw(canvas);
		int height = this.getHeight();
		int width = this.getWidth();
		for(int i = 40; i < height; i+=40)
		{
			canvas.drawLine(40, i, width, i, borderPaint);
		}
		for(int i = 39; i < width; i+=40)
		{
			canvas.drawLine(i, 1, i, height, borderPaint);
		}
	}

}
