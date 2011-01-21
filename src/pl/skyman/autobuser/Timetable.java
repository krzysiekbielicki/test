package pl.skyman.autobuser;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public class Timetable {
	
	public static final String[] PROJECTION = {DatabaseProvider.STOP_ID, DatabaseProvider.LINE_ID, DatabaseProvider.DIRECTION, DatabaseProvider.NORMAL_DEPARTURES, DatabaseProvider.HOLIDAY_DEPARTURES, DatabaseProvider.NORMAL_NOTES, DatabaseProvider.HOLIDAY_NOTES, DatabaseProvider.NORMAL_COMMENT, DatabaseProvider.HOLIDAY_COMMENT};
	
	public static final String SQL_TABLE_NAME = "timetables";
	public static final String SQL_KEY_STOPID = "stopid";
	public static final String SQL_KEY_LINEID = "lineid";
	public static final String SQL_KEY_DEPARTURETIMES = "departuretimes";
	public static final String SQL_KEY_DEPARTURENOTES = "departurenotes";
	public static final String SQL_KEY_TYPE = "type";
	public static final String SQL_KEY_DIRECTION = "direction";
	public static final String SQL_KEY_UPDATETIME = "updatetime";
	public static final String SQL_KEY_COMMENT = "comment";
	public static final int TYPE_P = 0;
	public static final int TYPE_S = 1;
	public static final String[] holidays={"2009-12-25", "2009-12-26", "2010-01-01"};
	
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_STOPID + " integer not null, " +
														SQL_KEY_LINEID + " text not null, " +
														SQL_KEY_DEPARTURETIMES + " text not null, " +
														SQL_KEY_DEPARTURENOTES + " text not null DEFAULT \"\", " +
														SQL_KEY_TYPE + " numeric not null, " +
														SQL_KEY_DIRECTION + " integer not null, " +
														SQL_KEY_UPDATETIME + " integer not null, " +
														SQL_KEY_COMMENT + " text not null DEFAULT \"\", " +
														"CONSTRAINT U_MyStop UNIQUE ("+SQL_KEY_STOPID+", "+SQL_KEY_LINEID+", "+SQL_KEY_TYPE+"));";
	
	private int type;
	private int index = 0;
	public int getType() {
		return type;
	}

	private long stopid, lineid;
	public long getStopid() {
		return stopid;
	}

	public long getLineid() {
		return lineid;
	}
	
	public int getDirection() {
		return direction;
	}

	private int direction;
	private LinkedList<Departure>[] departures = (LinkedList<Departure>[]) new LinkedList[2];

	private String comments;
	
	public Timetable(long sid, long lid, LinkedList<Departure> departuresP, LinkedList<Departure> departuresS, String comments, int dir) {
		this.direction = dir;
		this.stopid = sid;
		this.lineid = lid;
		
		this.departures[TYPE_P] = departuresP;
		this.departures[TYPE_S] = departuresS;
		this.comments = comments;
	}
	
	public Timetable(long sid, long lid, Cursor c)
	{
		type = isSwieto()?TYPE_S:TYPE_P;
		this.stopid = sid;
        this.lineid = lid;
        readTimetableFromCursor(c);
	}
	
	/*public Timetable(int sid, int lid, int[] departuretimesP, int[] departuretimesS, String departurenotesP, String departurenotesS, int dir)
	{
		this.direction = dir;
		this.stopid = sid;
		this.lineid = lid;
		this.departuretimesP = departuretimesP;
		this.departurenotesP = departurenotesP;
		this.departuretimesS = departuretimesS;
		this.departurenotesS = departurenotesS;
	}*/
	
	/*public Timetable(long sid, long lid, String[] departuretimesP, String[] departuretimesS, String departurenotesP, String departurenotesS, int dir)
	{
		this.direction = dir;
		this.stopid = sid;
		this.lineid = lid;
		
		this.departurenotesP = departurenotesP;
		this.departurenotesS = departurenotesS;
		this.departuretimesP = new int[departuretimesP[0].length()==0?0:departuretimesP.length];
    	for(int i = 0; i < departuretimesP.length; i++)
    		if(!departuretimesP[i].equals(""))
    			this.departuretimesP[i] = Integer.parseInt(departuretimesP[i]);
    
    	Arrays.sort(this.departuretimesP);
		this.departuretimesS = new int[departuretimesS[0].length()==0?0:departuretimesS.length];
    	for(int i = 0; i < departuretimesS.length; i++)
    		if(!departuretimesS[i].equals(""))
    			this.departuretimesS[i] = Integer.parseInt(departuretimesS[i]);
    
    	Arrays.sort(this.departuretimesS);
	}*/
	
	/*public Timetable(long sid, long lid, LinkedList<Departure> departuresP, LinkedList<Departure> departuresS, String notes, int dir) {
		this.direction = dir;
		this.stopid = sid;
		this.lineid = lid;
		
		this.departuresP = departuresP;
		this.departuresS = departuresS;
		this.notes = notes;
	}*/

	public static final boolean isSwieto() {
		GregorianCalendar date = new GregorianCalendar();
		if(date.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SATURDAY || date.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) return true;
		for(int i=0;i<holidays.length;i++) if(holidays[i].equals(date.get(GregorianCalendar.YEAR)+"-"+(date.get(GregorianCalendar.MONTH)+1)+"-"+date.get(GregorianCalendar.DAY_OF_MONTH))) return true;
		return false;
	}
	
	public static final boolean isSwietoTomorrow() {
		GregorianCalendar date = new GregorianCalendar();
		date.add(GregorianCalendar.DAY_OF_MONTH, 1);
		if(date.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SATURDAY || date.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) return true;
		for(int i=0;i<holidays.length;i++) if(holidays[i].equals(date.get(GregorianCalendar.YEAR)+"-"+(date.get(GregorianCalendar.MONTH)+1)+"-"+date.get(GregorianCalendar.DAY_OF_MONTH))) return true;
		return false;
	}

	public LinkedList<Departure> getDepartures() {
		return departures[type];
	}
	
	public LinkedList<Departure> getDeparturesTomorrow() {
		return departures[isSwietoTomorrow()?TYPE_S:TYPE_P];
	}
	
	public Departure getNearestDeparture() {
		return getNearestDepartures(1).get(0);
	}
	
	public String getComments() {
		return comments;
	}
	
	public LinkedList<Departure> getNearestDepartures(int n) {
		LinkedList<Departure> lDepartures = new LinkedList<Departure>();
		//for(int i = 0; i< n; i++)
		//	departures[i] = -1;
		LinkedList<Departure> departures = getDepartures();
		Calendar t = Calendar.getInstance();
		int currentMinutes = t.get(Calendar.HOUR_OF_DAY)*60+t.get(Calendar.MINUTE);
		int size;
		if(departures != null) {
			size = departures.size();
			while(this.index < size && departures.get(this.index).minute < currentMinutes)
				this.index++;
			
			if(this.index < size)
				for(int i = this.index; lDepartures.size() < n && i < size; i++)
					lDepartures.add(new Departure(departures.get(i).minute - currentMinutes, departures.get(i).isLowFloor, departures.get(i).modifier));
		}
		if(lDepartures.size() < n) {
			departures = getDeparturesTomorrow();
			if(departures != null) {
				size = departures.size();
				Departure dep;
				for(int i = 0; i < size && lDepartures.size() < n; i++) {
					dep = departures.get(i);
					lDepartures.add(new Departure(dep.minute + 1440 - currentMinutes, dep.isLowFloor, dep.modifier));
				}
			}
		}
		return lDepartures;
	}

	public void toggleSecondTimetable() {
		type = (type==TYPE_S?TYPE_P:TYPE_S);
	}
	
	public void setType(int type) {
		this.type = type;
	}
		
	public void readTimetableFromCursor(Cursor c)
	{
		if(!c.moveToFirst()) return;
		/*{
		 *0 TimetableProvider.STOP_ID, 
		 *1 TimetableProvider.LINE_ID, 
		 *2 TimetableProvider.DIRECTION, 
		 *3 TimetableProvider.NORMAL_DEPARTURES, 
		 *4 TimetableProvider.HOLIDAY_DEPARTURES, 
		 *5 TimetableProvider.NORMAL_NOTES, 
		 *6 TimetableProvider.HOLIDAY_NOTES, 
		 *7 TimetableProvider.NORMAL_COMMENT, 
		 *8 TimetableProvider.HOLIDAY_COMMENT};
		*/
		this.departures[TYPE_P] = createDepartureList(c.getString(3), c.getString(5));
		this.departures[TYPE_S] = createDepartureList(c.getString(4), c.getString(6));
        this.direction = c.getInt(2);
        this.comments = c.getString(7);
        c.close();
	}
	
	public static LinkedList<Departure> createDepartureList(String times, String notes) {
		LinkedList<Departure> list = new LinkedList<Departure>();
		String[] departuretimes = times.split(" ");
		char[] departurenotes = notes.toCharArray();
		if(departurenotes.length < departuretimes.length)
			departurenotes = new char[departuretimes.length];
		for(int i = 0; i < departuretimes.length; i++)
			if(departuretimes[i].equals(""))
				break;
			else
				list.add(new Departure(Integer.parseInt(departuretimes[i]), departurenotes[i]<97 && departurenotes[i]>'0', departurenotes[i]));
		return list;
	}

	public String getNearestDepartureTimeToString(int n) {
		//if(getDepartureTimes() == null)
		//	return "";
		String lDepartures = "";
		LinkedList<Departure> departures = getNearestDepartures(n);
		if(departures == null) return lDepartures;
		int size = departures.size();
		Departure dep;
		int departuretime;
		char note;
		for(int i = 0; i < size; i++) {
				dep = departures.get(i);
				note = dep.modifier;
				departuretime = dep.minute;
				if(departuretime < 60)
					lDepartures += (dep.isLowFloor?" <u>":" ")+(departuretime)+"min"+(dep.isLowFloor?"</u>":"");
				else
					lDepartures += (dep.isLowFloor?" <u>":" ")+(departuretime/60)+"h"+(departuretime%60<10?"0":"")+(departuretime%60)+"m"+(dep.isLowFloor?"</u>":"");
				if(note >= 'a')
					lDepartures += "<i>"+note+"</i>";
		}
		return lDepartures;
	}
	
	public void insertDB(ContentResolver cr) {
		ContentValues values = new ContentValues();
		values.put(SQL_KEY_LINEID, lineid);
		values.put(SQL_KEY_STOPID, stopid);
		values.put(SQL_KEY_DIRECTION, direction);
		values.put(SQL_KEY_UPDATETIME, System.currentTimeMillis()/60/1000);
		String departuretimes ="";
		String departurenotes ="";
		int size;
		Departure dep;
		if(departures[TYPE_P] != null) {
			size = departures[TYPE_P].size();
			for(int i = 0; i< size; i++) {
				dep = departures[TYPE_P].get(i);
				departuretimes  += dep.minute+" ";
				departurenotes  += dep.modifier=='\0'?(dep.isLowFloor?"1":"0"):new String(new char[] {(dep.isLowFloor?(char)(dep.modifier-32):(char)dep.modifier)});
			}
			values.put(SQL_KEY_DEPARTURETIMES, departuretimes);
			values.put(SQL_KEY_DEPARTURENOTES, departurenotes);
			values.put(SQL_KEY_COMMENT, comments);
			values.put(SQL_KEY_TYPE, TYPE_P);
			cr.insert(DatabaseProvider.CONTENT_URI_TIMETABLE, values);
		}
		if(departures[TYPE_S] != null) {
			departurenotes = "";
			departuretimes = "";
			size = departures[TYPE_S].size();
			for(int i = 0; i< size; i++) {
				dep = departures[TYPE_S].get(i);
				departuretimes  += dep.minute+" ";
				departurenotes  += dep.modifier=='\0'?(dep.isLowFloor?"1":"0"):new String(new char[] {(dep.isLowFloor?(char)(dep.modifier-32):(char)dep.modifier)});
			}
			values.put(SQL_KEY_DEPARTURETIMES, departuretimes);
			values.put(SQL_KEY_DEPARTURENOTES, departurenotes);
			values.put(SQL_KEY_COMMENT, comments);
			values.put(SQL_KEY_TYPE, TYPE_S);
			cr.insert(DatabaseProvider.CONTENT_URI_TIMETABLE, values);
		}
	}
	
	public void updateDB(ContentResolver cr) {
		ContentValues values = new ContentValues();
		values.put(SQL_KEY_UPDATETIME, System.currentTimeMillis()/60/1000);
		String departuretimes ="";
		String departurenotes ="";
		int size;
		Departure dep;
		if(departures[TYPE_P] != null) {
			size = departures[TYPE_P].size();
			for(int i = 0; i< size; i++) {
				dep = departures[TYPE_P].get(i);
				departuretimes  += dep.minute+" ";
				departurenotes  += dep.modifier=='\0'?(dep.isLowFloor?"1":"0"):new String(new char[] {(dep.isLowFloor?(char)(dep.modifier-32):(char)dep.modifier)});
			}
			values.put(SQL_KEY_DEPARTURETIMES, departuretimes);
			values.put(SQL_KEY_DEPARTURENOTES, departurenotes);
			values.put(SQL_KEY_COMMENT, comments);
			cr.update(DatabaseProvider.CONTENT_URI_TIMETABLE, values, SQL_KEY_LINEID+"="+lineid+" AND "+SQL_KEY_STOPID+"="+stopid+" AND "+SQL_KEY_TYPE+"="+TYPE_P+" AND "+SQL_KEY_DIRECTION+"="+direction, null);
		}
		if(departures[TYPE_S] != null) {
			departuretimes ="";
			departurenotes ="";
			size = departures[TYPE_S].size();
			for(int i = 0; i< size; i++) {
				dep = departures[TYPE_S].get(i);
				departuretimes  += dep.minute+" ";
				departurenotes  += dep.modifier=='\0'?(dep.isLowFloor?"1":"0"):new String(new char[] {(dep.isLowFloor?(char)(dep.modifier-32):(char)dep.modifier)});
			}
			values.put(SQL_KEY_DEPARTURETIMES, departuretimes);
			values.put(SQL_KEY_DEPARTURENOTES, departurenotes);
			values.put(SQL_KEY_COMMENT, comments);
			cr.update(DatabaseProvider.CONTENT_URI_TIMETABLE, values, SQL_KEY_LINEID+"="+lineid+" AND "+SQL_KEY_STOPID+"="+stopid+" AND "+SQL_KEY_TYPE+"="+TYPE_S+" AND "+SQL_KEY_DIRECTION+"="+direction, null);
		}
	}
	
	public static LinkedList<Departure> getDepartures( JSONObject tt ) throws JSONException {
		LinkedList<Departure> departures = new LinkedList<Departure>();
		Iterator hour = tt.keys();
		Iterator minute;
		String mMinute;
		while(hour.hasNext())
		{
			int hourKey = Integer.parseInt((String)hour.next());
			minute = tt.getJSONObject(hourKey+"").keys();
			while(minute.hasNext())
			{
				mMinute = (String) minute.next();
				//departuretimesS+=+" ";
				boolean isLowFloor = false;
				char modifier = 0;
				try {
					isLowFloor = tt.getJSONObject(hourKey+"").getInt(mMinute) == 1;
				} catch(JSONException e) {
					try {
						JSONObject o = tt.getJSONObject(hourKey+"").getJSONObject(mMinute);
						modifier = o.getString("1").charAt(0);
					} catch(JSONException f) {
						try{
							JSONArray o = tt.getJSONObject(hourKey+"").getJSONArray(mMinute);
							modifier = o.getString(1).charAt(0);
							isLowFloor = true;
						} catch (Exception g) {}
					}
				}
				departures.add(new Departure(Integer.parseInt((String)mMinute)+hourKey*60, isLowFloor, modifier));
			}
		}
		Collections.sort(departures, new Comparator<Departure>() {
			public int compare(Departure d1, Departure d2) {
				return d1.minute>d2.minute?1:-1;
			}
		});
		//departuretimesS = departuretimesS.trim();
		return departures;
	}
}
