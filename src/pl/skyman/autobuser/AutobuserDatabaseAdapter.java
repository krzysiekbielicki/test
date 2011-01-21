package pl.skyman.autobuser;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class AutobuserDatabaseAdapter extends SQLiteOpenHelper{
	private static AutobuserDatabaseAdapter instance;
	
	private static final String DATABASE_NAME = "autobuser";
	private static final int DATABASE_VERSION = 7;

	private AutobuserDatabaseAdapter(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL(Stop.SQL_CREATE_TABLE);
		 db.execSQL(MyStop.SQL_CREATE_TABLE);
		 db.execSQL(Line.SQL_CREATE_TABLE);
		 db.execSQL(Timetable.SQL_CREATE_TABLE);
		 db.execSQL(ZiL.SQL_CREATE_TABLE);
		 db.execSQL(AutobuserAlert.SQL_CREATE_TABLE);
		 db.execSQL(Route.SQL_CREATE_TABLE);
		 db.execSQL(Search.SQL_CREATE_TABLE);
		 db.execSQL(MyPlace.SQL_CREATE_TABLE);
		 db.execSQL(MultiLineWidget.SQL_CREATE_TABLE);
		 db.execSQL("CREATE INDEX zilIndex1 ON "+ZiL.SQL_TABLE_NAME+" ("+ZiL.SQL_KEY_NAME+");");
		 db.execSQL("CREATE INDEX zilIndex2 ON "+ZiL.SQL_TABLE_NAME+" ("+ZiL.SQL_KEY_NAME2+");");
		 upgrade(7, db);
		 //insertDemoData(db);
		 /*
CREATE VIEW timetableview AS
SELECT ms.name AS mystop_name, 
ms.fav AS mystop_favourite,
s.id AS stop_id, 
s.name AS stop_name, 
l.id AS line_id, 
l.name AS line_name, 
fs.id AS first_id,  
fs.name AS first_name, 
ls.id AS last_id, 
ls.name AS last_name, 
n.departuretimes AS normal_times, 
n.departurenotes AS normal_notes, 
h.departuretimes AS holiday_times,
h.departurenotes AS holiday_notes 
FROM mystops ms 
JOIN stops s ON (s.id = ms.stopid) 
JOIN lines l ON (l.id = ms.lineid) 
JOIN stops fs ON (fs.id = l.end1) 
JOIN stops ls ON (ls.id = l.end2) 
JOIN timetables n ON (n.stopid = s.id AND n.lineid = l.id AND n.type=0) 
JOIN timetables h ON (h.stopid = s.id AND h.lineid = l.id AND h.type=1);
					*/
	}

	private void upgrade(int i, SQLiteDatabase db) {
		switch(i) {
		case 7:
			db.execSQL("CREATE VIEW timetableview AS " +
					 "SELECT (n.stopid || n.lineid || n.direction) AS _id, " +
					 "ms.name AS mystop_name, " +
					 "ms.fav AS mystop_favourite, " +
					 "s.id AS stop_id, " +
					 "s.name AS stop_name, " + 
					 "s.no AS stop_no, " + 
					 "s.lat AS stop_lat, " + 
					 "s.lon AS stop_lon, " + 
					 "l.id AS line_id, " +
					 "l.name AS line_name, " + 
					 "fs.id AS first_id, " +
					 "fs.name AS first_name, " +
					 "fs.no AS first_no, " +
					 "fs.lat AS first_lat, " +
					 "fs.lon AS first_lon, " +
					 "ls.id AS last_id, " +
					 "ls.name AS last_name, " + 
					 "ls.no AS last_no, " + 
					 "ls.lat AS last_lat, " + 
					 "ls.lon AS last_lon, " + 
					 "n.direction AS direction, " +
					 "n.departuretimes AS normal_times, " + 
					 "n.departurenotes AS normal_notes, " +
					 "n.comment AS normal_comment, " +
					 "h.departuretimes AS holiday_times, " +
					 "h.departurenotes AS holiday_notes, " +
					 "h.comment AS holiday_comment, " +
					 "MIN(n.updatetime, h.updatetime) AS updatetime " +
					 "FROM mystops ms " +
					 "JOIN stops s ON (s.id = ms.stopid) " + 
					 "JOIN lines l ON (l.id = ms.lineid) " +
					 "JOIN stops fs ON (fs.id = l.end1) " +
					 "JOIN stops ls ON (ls.id = l.end2) " +
					 "JOIN timetables n ON (n.stopid = s.id AND n.lineid = l.id AND n.type=0) " + 
					 "JOIN timetables h ON (h.stopid = s.id AND h.lineid = l.id AND h.type=1);");
			break;
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 2) {
			db.execSQL("ALTER TABLE "+ZiL.SQL_TABLE_NAME+" ADD COLUMN "+ZiL.SQL_KEY_NAME2+" text not null DEFAULT \"\";");
			db.execSQL("CREATE INDEX zilIndex1 ON "+ZiL.SQL_TABLE_NAME+" ("+ZiL.SQL_KEY_NAME+");");
			db.execSQL("CREATE INDEX zilIndex2 ON "+ZiL.SQL_TABLE_NAME+" ("+ZiL.SQL_KEY_NAME2+");");
		}
		if(oldVersion < 3) {
			db.execSQL("CREATE TABLE alarms2 AS SELECT * FROM "+AutobuserAlert.SQL_TABLE_NAME+";");
			db.execSQL("ALTER TABLE alarms2 ADD COLUMN "+AutobuserAlert.SQL_KEY_LINE+" text not null DEFAULT \"\";");
			db.execSQL("ALTER TABLE alarms2 ADD COLUMN "+AutobuserAlert.SQL_KEY_STOP+" text not null DEFAULT \"\";");
			db.execSQL("UPDATE alarms2 SET "+AutobuserAlert.SQL_KEY_STOP+"=(SELECT s.name FROM stops s WHERE s.id=stopid), "+AutobuserAlert.SQL_KEY_LINE+"=(SELECT l.name FROM lines l WHERE l.id=lineid);");
			db.execSQL("DROP TABLE "+AutobuserAlert.SQL_TABLE_NAME+";");
			db.execSQL(AutobuserAlert.SQL_CREATE_TABLE);
			db.execSQL("INSERT INTO "+AutobuserAlert.SQL_TABLE_NAME+"("+AutobuserAlert.SQL_KEY_TIME+", "+AutobuserAlert.SQL_KEY_DAY+", "+AutobuserAlert.SQL_KEY_STOP+", "+AutobuserAlert.SQL_KEY_LINE+", "+AutobuserAlert.SQL_KEY_MINUTES+", "+AutobuserAlert.SQL_KEY_REPEAT+") SELECT "+AutobuserAlert.SQL_KEY_TIME+", "+AutobuserAlert.SQL_KEY_DAY+", "+AutobuserAlert.SQL_KEY_STOP+", "+AutobuserAlert.SQL_KEY_LINE+", "+AutobuserAlert.SQL_KEY_MINUTES+", "+AutobuserAlert.SQL_KEY_REPEAT+" FROM alarms2;");
			
			db.execSQL("DROP TABLE alarms2;");
		}
		if(oldVersion < 4) {
			db.execSQL("ALTER TABLE "+Timetable.SQL_TABLE_NAME+" ADD COLUMN "+Timetable.SQL_KEY_UPDATETIME+" integer not null DEFAULT \"1\";");
		}
		if(oldVersion < 5) {
			db.execSQL("ALTER TABLE "+Timetable.SQL_TABLE_NAME+" ADD COLUMN "+Timetable.SQL_KEY_DEPARTURENOTES+" text not null DEFAULT \"\";");
			db.execSQL("ALTER TABLE "+Timetable.SQL_TABLE_NAME+" ADD COLUMN "+Timetable.SQL_KEY_COMMENT+" text not null DEFAULT \"\";");
		}
		if(oldVersion < 6) {
			db.execSQL(MultiLineWidget.SQL_CREATE_TABLE);
		}
		if(oldVersion < 7) {
			upgrade(7, db);
		}
		
        /*db.execSQL("DROP TABLE IF EXISTS " + Stop.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + MyStop.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + Line.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + Timetable.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + ZiL.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + Route.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + Search.SQL_TABLE_NAME );
        db.execSQL("DROP TABLE IF EXISTS " + MyPlace.SQL_TABLE_NAME );*/
        //onCreate(db);
	}
	
	private static final void initialize(Context context) {
		if(instance == null)
			instance = new AutobuserDatabaseAdapter(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public static final AutobuserDatabaseAdapter getInstance(Context context) {
		initialize(context);
		return instance;
	}
	
	public void close() {
		if(instance != null ) {
			instance.close();
			instance = null;
		}
	}
}
