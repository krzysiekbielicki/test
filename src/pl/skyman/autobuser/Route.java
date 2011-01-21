package pl.skyman.autobuser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Route extends Activity implements AutobuserActivity {
	public static final String SQL_TABLE_NAME = "routehistory";
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_FROM = "start";
	public static final String SQL_KEY_TO = "dest";
	public static final String SQL_KEY_TIME = "time";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
													SQL_TABLE_NAME + " (" + 
													SQL_KEY_ID + " integer primary key autoincrement, " +
													SQL_KEY_FROM + " text not null, " +
													SQL_KEY_TO + " numeric not null, " +
													SQL_KEY_TIME + " numeric not null DEFAULT CURRENT_TIMESTAMP, " +
													"CONSTRAINT U_RouteHistory UNIQUE ("+SQL_KEY_FROM+", "+SQL_KEY_TO+"));";
	
	
	private LinkedList<Waypoint> points;
	private int time;
	private boolean odjazd = true;
	protected Location mLocation;
	private String start;
	private String destination;
    
	@Override
	protected void onResume() {
		getParent().setTitle(R.string.directions);
		if(((ListView)findViewById(android.R.id.list)).getVisibility() == View.GONE)
			getDirections();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.route);
		
		mLocation = ((Autobuser)getParent()).getLocation();
		Intent intent = getIntent();
		start = intent.getStringExtra("start");
		destination = intent.getStringExtra("destination");
		odjazd = intent.getBooleanExtra("odjazd", true);
		Calendar c = Calendar.getInstance();
		int timeSpan = 5;
		String timeSpanString = ((Autobuser)getParent()).getSettings().getString("fastRoutingTimeSpan", ""+timeSpan);
		try {
			timeSpan = Integer.parseInt(timeSpanString);
		} catch( NumberFormatException e ) {
			
		}
		
		time = ((c.get(Calendar.HOUR_OF_DAY)*60)+c.get(Calendar.MINUTE)+ timeSpan)%1440;
		time = getIntent().getIntExtra("time", time);
		registerForContextMenu(findViewById(android.R.id.list));
		getDirections();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		int pos = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		Waypoint w = points.get(pos);
		Intent i;
		if(w.stopno.indexOf("@")<0) {
			i = new Intent(Route.this, Search.class);
			i.putExtra("q", w.stopname);
			menu.add(String.format(getResources().getString(R.string.group), w.stopname)).setIntent(i);
		}
		if(!w.linename.equals("")) {
			i = new Intent(Route.this, Search.class);
			i.putExtra("q", w.linename);
			menu.add(String.format(getResources().getString(R.string.line), w.linename)).setIntent(i);
		}
		i = new Intent();
		i.putExtra("pos", pos);
		i.setAction("map");
		menu.add(getResources().getString(R.string.seeOnMap)).setIntent(i);
		Intent i2 = new Intent(i); 
		i2.setAction("reminder");
		if(!w.linename.equals(""))
			menu.add(getResources().getString(R.string.setReminder)).setIntent(i2);
		//showMap(pos);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent i = item.getIntent();
		if(i.hasExtra("q"))
			getParent().startActivity(i);
		else
			if(i.getAction().equals("map"))
				showMap(i.getIntExtra("pos", 0));
			else if(i.getAction().equals("reminder"))
				addAlarm(points.get(i.getIntExtra("pos", 0)));
		return true;
	}

	protected void getDirections() {
		final ProgressDialog pb = ProgressDialog.show(this, null, getResources().getText(R.string.gettingDirections), true, false);
		final Handler h = new Handler(){
			@Override
		    public void handleMessage(Message msg) {
				pb.dismiss();
				if(msg.what == 0) {
					try {
						ContentValues values = new ContentValues();
						values.put(SQL_KEY_FROM, start);
						values.put(SQL_KEY_TO, destination);
						long id = Long.parseLong(getContentResolver().insert(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, values).getLastPathSegment());
						getContentResolver().delete(DatabaseProvider.CONTENT_URI_ROUTEHISTORY, SQL_KEY_ID+"<"+(id-20), null);
					} catch(Exception e) {}
				} else if(msg.what == 1)
					((Autobuser)getParent()).toast(R.string.noNetwork, Toast.LENGTH_LONG);
				showDirections();
	        }
		};
		
		Thread t = new Thread(){
			@SuppressWarnings("unchecked")
			public void run() {
				points = new LinkedList<Waypoint>();
				String s = start, d = destination;
				if(s.indexOf("@GPS") >= 0)
					s = mLocation.getLatitude()+","+mLocation.getLongitude();
				if(d.indexOf("@GPS") >= 0)
					d = mLocation.getLatitude()+","+mLocation.getLongitude();
				String url = String.format(getString(R.string.queryPolaczenia), java.net.URLEncoder.encode(s), java.net.URLEncoder.encode(d), time, (Timetable.isSwieto()?1:0), (odjazd?"odjazd":"przyjazd"))+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter("http.socket.timeout", new Integer(1000));
				HttpGet request = new HttpGet(url);
				request.getParams().setParameter("http.socket.timeout", new Integer(5000));
	
				HttpResponse response;
				try {
					response = client.execute(request);
					 
		            HttpEntity entity = response.getEntity();
		            if (entity != null) {
		            	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
		            	JSONObject json = new JSONObject(in.readLine());
						if(!json.has("trasy")) {
							h.sendEmptyMessage(2);
							return;
						}
						JSONArray trasy = json.getJSONArray("trasy");
						int size =trasy.length(); 
						boolean startB;
						Waypoint point;
						Location l1, l2;
						int head = 0;
						String linesS;
						for(int i = 0; i< size; i++) {
							JSONObject trasa = trasy.getJSONObject(i);
							String punkt = json.getJSONObject("zapytanie").getString("start");
							
							startB = true;
							linesS = "";
							while(punkt != null)
							{
								point = new Waypoint();
								point.start = startB;
								startB = false;
								JSONObject zespol =  json.getJSONObject("zespoly").getJSONObject(punkt);
								point.stopname = zespol.getString("nazwa");
								
								if(trasa.getString(punkt).equals("END")) {
									point.end = true;
									punkt = null;
									point.lat = zespol.getDouble("lat");
									point.lon = zespol.getDouble("lon");
								} else {
									JSONObject p = trasa.getJSONObject(punkt);
									Iterator lines = p.keys();
									JSONObject linia = null;
									int lowtime = Integer.MAX_VALUE;
									String li;
									while(lines.hasNext()) {
										li = (String) lines.next();
										if(lowtime > p.getJSONObject(li).getInt("pierwszy")) {
											point.linename = li;
											linia = p.getJSONObject(li);
											lowtime = p.getJSONObject(li).getInt("pierwszy");
										}
									}
									
									linesS += point.linename + " - ";
									
									point.stopno = linia.getString("0");
									if(!zespol.getJSONObject("mapa").has(point.stopno))
									{
										point.lat = zespol.getDouble("lat");
										point.lon = zespol.getDouble("lon");
										if(linia.has("stacje"))
											point.stopsNumber = linia.getInt("stacje");
										//zespol =  json.getJSONObject("zespoly").getJSONObject(linia.getString("kierunek"));
										//point.direction = zespol.getString("nazwa");;
										if(linia.has("kierunek"))
											point.direction = linia.getInt("kierunek") == 3282 ? "Kabaty" : "Młociny";
									} else {
										point.lat = zespol.getJSONObject("mapa").getJSONArray(point.stopno).getDouble(0);
										point.lon = zespol.getJSONObject("mapa").getJSONArray(point.stopno).getDouble(1);
									}
									point.travelTime = linia.getInt("1");
									point.departure = linia.getInt("pierwszy")%1440;
									
									punkt = linia.getJSONArray("stops").getString(1);
								}
								if(point.start) {
									Waypoint point2 = new Waypoint();
									point2.stopno = "@HEAD";
									point2.stopname = (i+1)+"";
									point2.travelTime = -point.departure;
									head = points.size();
									points.add(point2);
									if(json.getJSONObject("info").getJSONObject("A").getBoolean("geo")) {
										point2 = new Waypoint();
										point2.start = true;
										point2.stopname = start;
										if(start.indexOf("@GPS")>=0) {
											point2.stopno = "@GPS";
										} else {
											point2.stopno = "@ADR";
										}
										JSONArray p = json.getJSONObject("info").getJSONObject("A").getJSONArray("punkt");
										l1 = new Location("");
										point2.lat = p.getDouble(0);
										point2.lon = p.getDouble(1);
										l1.setLatitude(point2.lat);
										l1.setLongitude(point2.lon);
										l2 = new Location("");
										l2.setLatitude(point.lat);
										l2.setLongitude(point.lon);
										point2.travelTime = (int) l1.distanceTo(l2);
										points.add(point2);
									}
									points.add(point);
								} else if(point.end) {
									if(json.getJSONObject("info").getJSONObject("B").getBoolean("geo")) {
										Waypoint point2 = new Waypoint();
										point2.stopname = destination;
										if(destination.indexOf("@GPS")>=0) {
											point2.stopno = "@GPS";
											point.stopno = "@GPS";
										} else {
											point2.stopno = "@ADR";
											point.stopno = "@ADR";
										}
										JSONArray p = json.getJSONObject("info").getJSONObject("B").getJSONArray("punkt");
										point2.lat = p.getDouble(0);
										point2.lon = p.getDouble(1);
										l1 = new Location("");
										l1.setLatitude(point2.lat);
										l1.setLongitude(point2.lon);
										l2 = new Location("");
										l2.setLatitude(point.lat);
										l2.setLongitude(point.lon);
										point2.end = true;
										point.end = false;
										int x = (int) l1.distanceTo(l2);
										point.travelTime = x;
										point.departure = points.getLast().departure+points.getLast().travelTime;
										points.get(head).travelTime += point.departure;
										if(points.get(head).travelTime < 0)
											points.get(head).travelTime += 1440;
										points.get(head).linename = linesS.substring(0, linesS.length() - 3);
										points.add(point);
										points.add(point2);
									} else {
										point.departure = points.getLast().departure+points.getLast().travelTime;
										points.get(head).travelTime += point.departure;
										points.get(head).linename = linesS.substring(0, linesS.length() - 3);
										points.add(point);
									}
										
								} else
									points.add(point);
							}
						}	
						h.sendEmptyMessage(0);
					}
					else
						h.sendEmptyMessage(1);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					h.sendEmptyMessage(1);
				} catch (IOException e) {
					e.printStackTrace();
					h.sendEmptyMessage(1);
				} catch (JSONException e) {
					e.printStackTrace();
					h.sendEmptyMessage(0);
				}
			}
		};
		t.start();
	}

	protected void showDirections() {
		ListView list = (ListView)findViewById(android.R.id.list);
		if(!points.isEmpty()) {
			((TextView)findViewById(R.id.fromTo)).setText(String.format(getResources().getString(R.string.fromTo), start, destination));
			((TextView)findViewById(R.id.departure)).setText(String.format(getResources().getString(odjazd?R.string.departuresFrom:R.string.arrivalsTo), (time/60)+":"+(time%60<10?"0"+time%60:time%60)));
			list.setAdapter(new DirectionsAdapter(this, points, this));
			list.setVisibility(View.VISIBLE);
			((View)findViewById(android.R.id.empty)).setVisibility(View.GONE);
			((View)findViewById(R.id.header)).setVisibility(View.VISIBLE);
		} else {
			list.setVisibility(View.GONE);
			((View)findViewById(android.R.id.empty)).setVisibility(View.VISIBLE);
			((View)findViewById(R.id.header)).setVisibility(View.GONE);
		}
	}
	
	public void locationChanges(Location newLocation) {
		mLocation = newLocation;
	}

	public void minuteChanges() {
		return;
	}

	public void orientationChanges(float[] newOrientation) {
		return;
	}

	public void showMap(int pos) {
		Waypoint w = points.get(pos);
		Intent i = new Intent(Route.this, AutobuserMap.class);
		int point = 0;
		int steps = 0;
		if(!w.stopno.equals("@HEAD")) {
			point--;
		//	steps--;
		}
		while(!w.stopno.equals("@HEAD")) {
			point++;
			pos--;
			w = points.get(pos);
		}
		//if(w.stopno.equals("@HEAD")) {
			 
			do {
				steps++;
				w = points.get(pos+steps);
			}while(!w.stopno.equals("@HEAD") && pos+steps+1 < points.size());
			if(pos+steps+1 < points.size())
				steps--;
			int[] lats = new int[steps];
			int[] lons = new int[steps];
			String[] titles = new String[steps];
			String[] snippets = new String[steps];
			for(int x = 0; x<steps; x++) {
				w = points.get(pos+1+x);
				lats[x] = (int) (w.lat*1E6);
				lons[x] = (int) (w.lon*1E6);
				titles[x] = w.stopname+(w.stopno.indexOf("@")>=0||w.stopno.equals("0")?"":"|"+w.stopno+"|"+w.linename);
				if(x+1 == steps) {
					snippets[x] = getString(R.string.directionrecord_end);
				} else if(w.stopno.indexOf("@")>=0) {
					if(!w.start)
						snippets[x] =  String.format(getString(R.string.directionrecord_getoff), (w.departure/60)+":"+(w.departure%60<10?"0"+w.departure%60:w.departure%60))+", "+String.format(getString(R.string.directionrecord_walk), w.travelTime);
					else
					snippets[x] = String.format(getString(R.string.directionrecord_walk), w.travelTime);
				} else {
					int minutes = (w.departure%60);
					snippets[x] = String.format(getString(w.start?R.string.directionrecord_geton:R.string.directionrecord_change), (w.departure/60)+":"+(minutes<10?"0"+minutes:minutes));
					if(w.stopno.equals("0"))
						if(w.stopsNumber == 1)
							snippets[x]+=" "+String.format(getString(R.string.directionrecord_underground1), w.direction, w.stopsNumber);
						else if(w.stopsNumber > 1 && w.stopsNumber < 5)
							snippets[x]+=" "+String.format(getString(R.string.directionrecord_underground234), w.direction, w.stopsNumber);
						else
							snippets[x]+=" "+String.format(getString(R.string.directionrecord_underground5up), w.direction, w.stopsNumber);
					else {
						snippets[x]+=" "+w.linename;
						snippets[x]+=" ("+w.travelTime+"min)";
					}
				}
			}
			i.putExtra("latsE6", lats);
			i.putExtra("lonsE6", lons);
			i.putExtra("titles", titles);
			i.putExtra("snippets", snippets);
			i.putExtra("point", point);
			i.putExtra("time", System.currentTimeMillis());
		/*} else {
			i.putExtra("latE6", (int) (w.lat * 1E6));
			i.putExtra("lonE6", (int) (w.lon * 1E6));
			i.putExtra("title", w.stopname+" "+(w.stopno.indexOf("@")>=0?"":w.stopno));
		}*/
		getParent().startActivity(i);
	}
	
	private void addAlarm(final Waypoint w) {
		View v = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.addalarm, null);
		ArrayAdapter<CharSequence> timeBefore = ArrayAdapter.createFromResource(this, R.array.addAlarmMinutes, android.R.layout.simple_spinner_item);
		timeBefore.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner timeSpinner = ((Spinner)v.findViewById(R.id.AddAlarmTimeSpinner));
		timeSpinner.setAdapter(timeBefore);
		final Spinner daysSpinner = ((Spinner)v.findViewById(R.id.AddAlarmDaySpinner));
		final CheckBox checkbox = ((CheckBox)v.findViewById(R.id.AddAlarmCheckboxRepeat));
		GregorianCalendar calendar = new GregorianCalendar();
		int dow = calendar.get(GregorianCalendar.DAY_OF_WEEK);
		ArrayAdapter<CharSequence> days = ArrayAdapter.createFromResource(this, Timetable.isSwieto()?R.array.addAlarmSDays:R.array.addAlarmPDays, android.R.layout.simple_spinner_item);
				
		days.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		daysSpinner.setAdapter(days);
		
		if(calendar.getFirstDayOfWeek() == GregorianCalendar.MONDAY)
			dow = (dow-1)%5;
		else
			dow = dow==1?1:dow<7?dow-2:0;
		
		//if(Timetable.isSwieto() != (timetable.getType() == Timetable.TYPE_P))
		//	daysSpinner.setSelection(dow);
		new AlertDialog.Builder(this)
		.setView(v)
		.setIcon(android.R.drawable.ic_popup_reminder)
		.setTitle(w.departure/60+":"+(w.departure%60<10?"0"+w.departure%60:w.departure%60))
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				int day = daysSpinner.getSelectedItemPosition();
				Calendar c = Calendar.getInstance();
				day++;

				if(Timetable.isSwieto())
					day = day == 2?1:7;
				else
					day++;
				
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				c.add(Calendar.DAY_OF_MONTH, -(c.get(Calendar.DAY_OF_WEEK)-day));
				int minutes = w.departure - Integer.parseInt((String)timeSpinner.getSelectedItem());
				c.set(Calendar.HOUR_OF_DAY, minutes/60);
				c.set(Calendar.MINUTE, minutes%60);
				
				if(c.getTimeInMillis() < System.currentTimeMillis())
					c.add(Calendar.WEEK_OF_YEAR, 1);
				
				ContentValues values = new ContentValues();
				values.put(AutobuserAlert.SQL_KEY_MINUTES, w.departure);
				values.put(AutobuserAlert.SQL_KEY_REPEAT, checkbox.isChecked());
				values.put(AutobuserAlert.SQL_KEY_TIME, c.getTimeInMillis());
				values.put(AutobuserAlert.SQL_KEY_DAY, day);
				
				values.put(AutobuserAlert.SQL_KEY_STOP, w.stopname);
				values.put(AutobuserAlert.SQL_KEY_LINE, w.linename);
				//values.put(AutobuserAlert.SQL_KEY_ID, null);
				getContentResolver().insert(DatabaseProvider.CONTENT_URI_ALERT, values);
				Toast.makeText(Route.this, String.format(getResources().getString(R.string.AlarmWillRingAt), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime())), Toast.LENGTH_LONG).show();
				AutobuserAlert.setNextAlarm(Route.this);
			}
			
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
			
		})
		.create().show();
	}

}
