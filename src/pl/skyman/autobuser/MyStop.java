package pl.skyman.autobuser;

import android.location.Location;

public class MyStop {
	public static final String SQL_TABLE_NAME = "mystops";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_STOPID = "stopid";
	public static final String SQL_KEY_LINEID = "lineid";
	public static final String SQL_KEY_FAVOURITE = "fav";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
													SQL_TABLE_NAME + " (" + 
													SQL_KEY_NAME + " text, " +
													SQL_KEY_STOPID + " integer not null, " +
													SQL_KEY_LINEID + " integer not null, " +
													SQL_KEY_FAVOURITE + " boolean default false, " +
													"CONSTRAINT U_MyStop UNIQUE ("+SQL_KEY_STOPID+", "+SQL_KEY_LINEID+"));";
	
	private Stop stop;
	private Line line;
	private String name;
	private Timetable timetable;

	public MyStop(String n, Stop s, Line l, Timetable t)
	{
		name = n;
		stop = s;
		line = l;
		timetable = t;
	}
	
	public MyStop toggleSecondTimetable()
	{
		timetable.toggleSecondTimetable();
		return this;
	}
	
	public String toString()
	{
		return line.getName()+" "+stop.getName()+" > "+(timetable.getDirection()==0?line.getEnd1().getName():line.getEnd2().getName());
	}

	public Timetable getTimetable() {
		return timetable;
	}
	
	public double getDistance() {
		return stop.getDistance();
	}
	
	public void setDistanceTo(Location cur) {
		stop.setDistanceTo(cur);
	}

	public Location getLocation() {
		return stop.getLocation();
	}

	public String getName() {
		return name;
	}

	public long getStopId() {
		return stop.getId();
	}
	
	public long getLineId() {
		return line.getId();
	}
	
	public String getStopName() {
		return stop.getName();
	}
	
	public String getLineName() {
		return line.getName();
	}
}
