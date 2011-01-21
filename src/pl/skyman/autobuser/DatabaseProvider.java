package pl.skyman.autobuser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class DatabaseProvider extends ContentProvider {
	/*
	 * SELECT ms.name AS mystop_name, " +
					"ms.favourite AS mystop_favourite, " +
					"s.id AS stop_id, " +
					"s.name AS stop_name, " +
					"l.id AS line_id, " +
					"l.name AS line_name, " +
					"f.id AS first_id, " +
					"f.name AS first_name, " +
					"l.id AS last_id, " +
					"l.name AS last_name, " +
					"n.departures AS normal_departures, " +
					"h.departures AS holiday_departures " +
					"FROM mystops ms " +
					"JOIN stops s ON (s.id = ms.stopid) " +
					"JOIN line l ON (l.id = ms.lineid) " +
					"JOIN stops fs ON (fs.id = l.end1) " +
					"JOIN stops ls ON (ls.id = l.end2) " +
					"JOIN timetables n ON (n.stopid = s.id AND n.lineid = l.id AND n.type=0) " +
					"JOIN timetables h ON (h.stopid = s.id AND h.lineid = l.id AND h.type=1);
	*/
	public static final Uri    CONTENT_URI        = Uri.parse("content://pl.skyman.autobuser.timetableprovider");
	public static final Uri    CONTENT_URI_TIMETABLE = Uri.parse("content://pl.skyman.autobuser.timetableprovider/timetable");
	public static final Uri    CONTENT_URI_MYSTOP = Uri.parse("content://pl.skyman.autobuser.timetableprovider/mystop");
	public static final Uri    CONTENT_URI_STOP = Uri.parse("content://pl.skyman.autobuser.timetableprovider/stop");
	public static final Uri    CONTENT_URI_LINE = Uri.parse("content://pl.skyman.autobuser.timetableprovider/line");
	public static final Uri    CONTENT_URI_MYPLACE = Uri.parse("content://pl.skyman.autobuser.timetableprovider/myplace");
	public static final Uri    CONTENT_URI_ZIL = Uri.parse("content://pl.skyman.autobuser.timetableprovider/zil");
	public static final Uri    CONTENT_URI_ROUTEHISTORY = Uri.parse("content://pl.skyman.autobuser.timetableprovider/routehistory");
	public static final Uri    CONTENT_URI_SEARCHHISTORY = Uri.parse("content://pl.skyman.autobuser.timetableprovider/searchhistory");
	public static final Uri    CONTENT_URI_ALERT = Uri.parse("content://pl.skyman.autobuser.timetableprovider/alert");
	public static final Uri    CONTENT_URI_MULTILINEWIDGET = Uri.parse("content://pl.skyman.autobuser.timetableprovider/multilinewidget");
	public static final String ID                 = "_id";
	public static final String MYSTOP_NAME        = "mystop_name";
	public static final String MYSTOP_FAVOURITE   = "mystop_favourite";
	public static final String STOP_ID            = "stop_id";
	public static final String STOP_NAME          = "stop_name";
	public static final String STOP_NO          = "stop_no";
	public static final String STOP_LAT           = "stop_lat";
	public static final String STOP_LON           = "stop_lon";
	public static final String LINE_ID            = "line_id";
	public static final String LINE_NAME          = "line_name";
	public static final String FIRST_STOP_ID      = "first_id";
	public static final String FIRST_STOP_NAME    = "first_name";
	public static final String FIRST_STOP_NO    = "first_no";
	public static final String FIRST_STOP_LAT    = "first_lat";
	public static final String FIRST_STOP_LON    = "first_lon";
	public static final String LAST_STOP_ID       = "last_id";
	public static final String LAST_STOP_NAME     = "last_name";
	public static final String LAST_STOP_NO     = "last_no";
	public static final String LAST_STOP_LAT     = "last_lat";
	public static final String LAST_STOP_LON     = "last_lon";
	public static final String DIRECTION  		  = "direction";
	public static final String NORMAL_DEPARTURES  = "normal_times";
	public static final String NORMAL_NOTES       = "normal_notes";
	public static final String NORMAL_COMMENT     = "normal_comment";
	public static final String HOLIDAY_DEPARTURES = "holiday_times";
	public static final String HOLIDAY_NOTES      = "holiday_notes";
	public static final String HOLIDAY_COMMENT    = "holiday_comment";
	public static final String UPDATETIME         = "updatetime";
	private static final String DATABASE_TABLE_NAME = "timetableview";
	private SQLiteDatabase db;
	
	
	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		int ret = 0;
		if(uri.equals(CONTENT_URI_MYPLACE))
        	ret = db.delete(MyPlace.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_SEARCHHISTORY))
        	ret = db.delete(Search.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_ROUTEHISTORY))
        	ret = db.delete(Route.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_MYSTOP))
        	ret = db.delete(MyStop.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_TIMETABLE))
        	ret = db.delete(Timetable.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_ALERT))
        	ret = db.delete(AutobuserAlert.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_ZIL))
        	ret = db.delete(ZiL.SQL_TABLE_NAME, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_MULTILINEWIDGET))
        	ret = db.delete(MultiLineWidget.SQL_TABLE_NAME, whereClause, whereArgs);
		return ret;
	}

	@Override
	public String getType(Uri arg0) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri ret = null;
		if(uri.getLastPathSegment().equals("timetable")) {
			db.insert(Timetable.SQL_TABLE_NAME, null, values);
		} else if(uri.getLastPathSegment().equals("mystop")) {
			db.insert(MyStop.SQL_TABLE_NAME, null, values);
		} else if(uri.equals(CONTENT_URI_MYPLACE))
			db.insert(MyPlace.SQL_TABLE_NAME, null, values);
		else if(uri.equals(CONTENT_URI_ZIL))
			db.insert(ZiL.SQL_TABLE_NAME, null, values);
		else if(uri.equals(CONTENT_URI_STOP))
			ret = Uri.withAppendedPath(uri, db.insert(Stop.SQL_TABLE_NAME, "", values)+"");
		else if(uri.equals(CONTENT_URI_LINE))
			ret = Uri.withAppendedPath(uri, db.insert(Line.SQL_TABLE_NAME, "0", values)+"");
		else if(uri.equals(CONTENT_URI_ROUTEHISTORY))
			ret = Uri.withAppendedPath(uri, db.replace(Route.SQL_TABLE_NAME, null, values)+"");
		else if(uri.equals(CONTENT_URI_SEARCHHISTORY))
			ret = Uri.withAppendedPath(uri, db.replace(Search.SQL_TABLE_NAME, null, values)+"");
		else if(uri.equals(CONTENT_URI_ALERT))
			ret = Uri.withAppendedPath(uri, db.insert(AutobuserAlert.SQL_TABLE_NAME, "0", values)+"");
		else if(uri.equals(CONTENT_URI_MULTILINEWIDGET))
			ret = Uri.withAppendedPath(uri, db.insert(MultiLineWidget.SQL_TABLE_NAME, null, values)+"");
		return ret;
	}

	@Override
	public boolean onCreate() {
		db = AutobuserDatabaseAdapter.getInstance(getContext()).getWritableDatabase();
        return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// SQLiteQueryBuilder is a helper class that creates the
        // proper SQL syntax for us.
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		String limit = null;
        // Set the table we're querying.
        if(uri.equals(CONTENT_URI_MYPLACE))
        	qBuilder.setTables(MyPlace.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_STOP))
        	qBuilder.setTables(Stop.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_LINE))
        	qBuilder.setTables(Line.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_ZIL)) {
        	qBuilder.setTables(ZiL.SQL_TABLE_NAME);
        	limit = "25";
        }
        else if(uri.equals(CONTENT_URI_ROUTEHISTORY))
        	qBuilder.setTables(Route.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_SEARCHHISTORY))
        	qBuilder.setTables(Search.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_ALERT))
        	qBuilder.setTables(AutobuserAlert.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_TIMETABLE))
        	qBuilder.setTables(Timetable.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI_MULTILINEWIDGET))
        	qBuilder.setTables(MultiLineWidget.SQL_TABLE_NAME);
        else if(uri.equals(CONTENT_URI))
       		qBuilder.setTables(DATABASE_TABLE_NAME);
            

        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        /*if(uri.getLastPathSegment() != null){
            qBuilder.appendWhere("_id=" + uri.getLastPathSegment());
        }*/

        // Make the query.
        Cursor c = qBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                limit);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
		int ret = 0;
		if(uri.getLastPathSegment().equals("timetable")) {
			ret = db.update(Timetable.SQL_TABLE_NAME, values, whereClause, whereArgs);
		} else if(uri.getLastPathSegment().equals("mystop")) {
			ret = db.update(MyPlace.SQL_TABLE_NAME, values, whereClause, whereArgs);
		} else if(uri.equals(CONTENT_URI_STOP))
			ret = db.update(Stop.SQL_TABLE_NAME, values, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_ALERT))
			ret = db.update(AutobuserAlert.SQL_TABLE_NAME, values, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_MYPLACE))
			ret = db.update(MyPlace.SQL_TABLE_NAME, values, whereClause, whereArgs);
		else if(uri.equals(CONTENT_URI_MULTILINEWIDGET))
			ret = db.update(MultiLineWidget.SQL_TABLE_NAME, values, whereClause, whereArgs);
		return ret;
	}

}
