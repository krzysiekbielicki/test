package pl.skyman.autobuser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;

public class Stop {
	
	public static final String SQL_TABLE_NAME = "stops";
	public static final String SQL_KEY_ID = "id";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_NO = "no";
	public static final String SQL_KEY_LAT = "lat";
	public static final String SQL_KEY_LON = "lon";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_ID + " integer primary key autoincrement, " +
														SQL_KEY_NAME + " text not null, " +
														SQL_KEY_NO + " numeric not null, " +
														SQL_KEY_LAT + " numeric, " +
														SQL_KEY_LON + " numeric, " +
														"CONSTRAINT U_Stop UNIQUE ("+SQL_KEY_NAME+", "+SQL_KEY_NO+"));";
	
	private long id;
	private String name;
	private int no;
	private Location location;
	private double distance = 0;
	
	public Stop(long id, ContentResolver cr)
	{
		Cursor c = cr.query(DatabaseProvider.CONTENT_URI_STOP, new String[] {SQL_KEY_ID, SQL_KEY_NAME, SQL_KEY_NO, SQL_KEY_LAT, SQL_KEY_LON}, SQL_KEY_ID+"="+id, null, null);
        if(!c.moveToFirst()) {
        	c.close();
        	return;
        }
        this.id = c.getInt(0);
        this.name = c.getString(1);
        this.no = c.getInt(2);
		setLocation(c.getDouble(3), c.getDouble(4));
		c.close();
	}
	
	public long insertDB(ContentResolver cr) {
		ContentValues values = new ContentValues(); 
		values.put(Stop.SQL_KEY_NAME, name);
		values.put(Stop.SQL_KEY_NO, no);
		values.put(Stop.SQL_KEY_LAT, location.getLatitude());
		values.put(Stop.SQL_KEY_LON, location.getLongitude());
		long id = Long.parseLong(cr.insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
		values.clear();
		
		return id;
	}
	
	public void updateDB(ContentResolver cr) {
		ContentValues values = new ContentValues(); 
		values.put(Stop.SQL_KEY_NAME, name);
		values.put(Stop.SQL_KEY_NO, no);
		values.put(Stop.SQL_KEY_LAT, location.getLatitude());
		values.put(Stop.SQL_KEY_LON, location.getLongitude());
		cr.update(DatabaseProvider.CONTENT_URI_STOP, values, SQL_KEY_NO+"="+no, null);
		values.clear();
	}
	
	private void setLocation(double lat, double lon) {
		location = new Location("reverseGeocoded");
		location.setLatitude(lat);
		location.setLongitude(lon);
	}

	public Stop(String name, int no, double lat, double lon)
	{
		this.name = name;
		this.no = no;
		setLocation(lat, lon);
	}
	
	public Stop(long id, String name, int no, double lat, double lon)
	{
		this(name, no, lat, lon);
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public int getNo() {
		return no;
	}


	public Location getLocation() {
		return location;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public void setDistanceTo(Location cur) {
		distance = location.distanceTo(cur);
	}
}
