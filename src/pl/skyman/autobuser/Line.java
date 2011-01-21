package pl.skyman.autobuser;

import android.content.ContentResolver;
import android.database.Cursor;

public class Line {
	
	public static final String SQL_TABLE_NAME = "lines";
	public static final String SQL_KEY_ID = "id";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_END1 = "end1";
	public static final String SQL_KEY_END2 = "end2";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_ID + " integer primary key autoincrement, " +
														SQL_KEY_NAME + " text not null, " +
														SQL_KEY_END1 + " integer not null, " +
														SQL_KEY_END2 + " integer not null, " +
														"CONSTRAINT U_Line UNIQUE ("+SQL_KEY_NAME+"));";
	
	private long id;
	private String name;
	private Stop end1, end2;
	
	public Line(long id, ContentResolver cr)
	{
		Cursor c = cr.query(DatabaseProvider.CONTENT_URI_LINE, new String[] {Line.SQL_KEY_ID, Line.SQL_KEY_NAME, Line.SQL_KEY_END1, Line.SQL_KEY_END2}, Line.SQL_KEY_ID+"="+id, null, null);
		c.moveToFirst();
        this.id   = c.getLong(0);
        this.name = c.getString(1);
        this.end1 = new Stop(c.getInt(2), cr);
        this.end2 = new Stop(c.getInt(3), cr);
        c.close();
        
	}
	
	public Line(String name, Stop end1, Stop end2)
	{
		this.name = name;
		this.end1 = end1;
		this.end2 = end2;
	}
	
	public Line(long id, String name, Stop end1, Stop end2)
	{
		this(name, end1, end2);
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Stop getEnd1() {
		return end1;
	}

	public Stop getEnd2() {
		return end2;
	}
}
