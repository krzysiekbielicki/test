package pl.skyman.autobuser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class TimetableActivity extends Activity implements AutobuserActivity {
	Stop stop;
	Line line;
	Timetable timetable;
	protected float[] mValues;
	private static final int INTERNET = 0;
	private static final int DB = 1;
	private int source;
	
	protected Location mLocation;
	private boolean favourite;
    
	protected void redrawGeoViews() {
		((GeoView)findViewById(R.id.geo)).updateGeoData(mValues, mLocation);
	}
	
	protected void refreshTimes() {
		try {
			if(!(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S))) {
				String deps = timetable.getNearestDepartureTimeToString(3);
				if(deps.equals(""))
					deps = getResources().getString(R.string.noDeparturesToday);
				else
					deps = getResources().getString(R.string.departureIn)+deps;
				((TextView)findViewById(R.id.departures)).setText(Html.fromHtml(deps));
			}
		} catch (Exception e) {}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		//refreshMyStopsList();
	}
	
	@Override
    protected void onStop()
    {
	    //refreshTimesTask.cancel();
        super.onStop();
    }
	
	@Override
    protected void onDestroy()
    {
	    super.onDestroy();
    }
		

	@Override
	protected void onResume() {
		super.onResume();
		getParent().setTitle(R.string.schedule);
		if(mLocation == null)
			mLocation = new Location(((Autobuser)getParent()).getLocation());
		else
			mLocation.set(((Autobuser)getParent()).getLocation());
		if(timetable!=null)
			((TimetableView)findViewById(R.id.timetable)).reinitView();
		else
			onNewIntent(getIntent());
		//redrawGeoViews();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		if(intent.hasExtra("linia")) {
			source = INTERNET;
			String linia = intent.getStringExtra("linia");
			int id_przystanku = intent.getIntExtra("id_przystanku", 0);
			String numer = intent.getStringExtra("numer");
			getStopData(numer, id_przystanku, linia);
		} else {
			source = DB;
			long lineid = intent.getLongExtra("lineid", 0);
			long stopid = intent.getLongExtra("stopid", 0);
			stop = new Stop(stopid, getContentResolver());
			line = new Line(lineid, getContentResolver());
			timetable = new Timetable(stopid, lineid, getContentResolver().query(DatabaseProvider.CONTENT_URI, Timetable.PROJECTION, DatabaseProvider.STOP_ID+"="+stopid+" AND "+DatabaseProvider.LINE_ID+"="+lineid, null, null));
			timetable.setType(Timetable.isSwieto()?Timetable.TYPE_S:Timetable.TYPE_P);
			displayTimetable();
		}
		//super.onNewIntent(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timetable);
		((Button) findViewById(R.id.typeP)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				showPTimetable();
			}
		});
		((Button) findViewById(R.id.typeS)).setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				showSTimetable();
			}
		});
		//onNewIntent(getIntent());
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
	
	private void showPTimetable() {
		((Button) findViewById(R.id.typeP)).setBackgroundColor(getResources().getColor(R.color.white));
		((Button) findViewById(R.id.typeP)).setTextColor(getResources().getColor(R.color.black));
		((Button) findViewById(R.id.typeS)).setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));
		((Button) findViewById(R.id.typeS)).setTextColor(getResources().getColor(R.color.white));
		
		((TimetableView)findViewById(R.id.timetable)).setType(Timetable.TYPE_P);
		((TimetableView)findViewById(R.id.timetable)).reinitView();
		if(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S)) {
			((TextView)findViewById(R.id.departures)).setText(R.string.checkSecondTimetable);
			((TextView)findViewById(R.id.departures)).setTextColor(getResources().getColor(R.color.red));
		} else {
			((TextView)findViewById(R.id.departures)).setTextColor(getResources().getColor(R.color.white));
			refreshTimes();
		}
	}
	
	private void showSTimetable() {
		((Button) findViewById(R.id.typeS)).setBackgroundColor(getResources().getColor(R.color.white));
		((Button) findViewById(R.id.typeS)).setTextColor(getResources().getColor(R.color.black));
		((Button) findViewById(R.id.typeP)).setBackgroundDrawable(getResources().getDrawable(android.R.drawable.list_selector_background));
		((Button) findViewById(R.id.typeP)).setTextColor(getResources().getColor(R.color.white));
		
		((TimetableView)findViewById(R.id.timetable)).setType(Timetable.TYPE_S);
		((TimetableView)findViewById(R.id.timetable)).reinitView();
		if(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S)) {
			((TextView)findViewById(R.id.departures)).setText(R.string.checkSecondTimetable);
			((TextView)findViewById(R.id.departures)).setTextColor(getResources().getColor(R.color.red));
		} else {
			((TextView)findViewById(R.id.departures)).setTextColor(getResources().getColor(R.color.white));
			refreshTimes();
		}
	}

	protected void displayTimetable() {
		((TextView)findViewById(R.id.title)).setText(line.getName()+" "+stop.getName()+">"+(timetable.getDirection()==0?line.getEnd1().getName():line.getEnd2().getName()));
		((Button) findViewById(timetable.getType()==Timetable.TYPE_S?R.id.typeS:R.id.typeP)).setBackgroundColor(getResources().getColor(R.color.white));
		((Button) findViewById(timetable.getType()==Timetable.TYPE_S?R.id.typeS:R.id.typeP)).setTextColor(getResources().getColor(R.color.black));
		if(Timetable.isSwieto() ^ (timetable.getType()==Timetable.TYPE_S))
			((TextView)findViewById(R.id.departures)).setText(R.string.checkSecondTimetable);
		else {
			String deps = timetable.getNearestDepartureTimeToString(3);
			if(deps.equals(""))
				deps = getResources().getString(R.string.noDeparturesToday);
			else
				deps = getResources().getString(R.string.departureIn)+deps;
			((TextView)findViewById(R.id.departures)).setText(Html.fromHtml(deps));
		}
		((GeoView)findViewById(R.id.geo)).setLocation(stop.getLocation());
		((TimetableView)findViewById(R.id.timetable)).setTimetable(timetable);
		((TimetableView)findViewById(R.id.timetable)).reinitView();
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI, new String[] {DatabaseProvider.MYSTOP_NAME}, DatabaseProvider.STOP_ID+"="+stop.getId()+" AND "+DatabaseProvider.LINE_ID+"="+line.getId(), null, null);
		
		if(c.moveToFirst()) {
			favourite = true;
			if(source == INTERNET)
				timetable.updateDB(getContentResolver());
		} else {
			favourite = false;
		}
		setButton();
		c.close();
	}
	
	private void setButton() {
		ImageButton button;
		button = (ImageButton)findViewById(R.id.button);
		button.setVisibility(View.VISIBLE);
		button.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				showButtonMenu();
			}
		});
	}
	
	private void showButtonMenu() {
		//ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.addAlarmMinutes, android.R.layout.select_dialog_item);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);
		adapter.add(getResources().getString(favourite?R.string.deleteFromFavourites:R.string.addToFavourites));
		adapter.add(String.format(getResources().getString(R.string.line), line.getName()));
		adapter.add(String.format(getResources().getString(R.string.group), stop.getName()));
		adapter.add(getResources().getString(R.string.showNotes));
		adapter.add(getResources().getString(R.string.seeOnMap));
		adapter.add(getResources().getString(R.string.updateTimetable));
		new AlertDialog.Builder(this)
		.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent i;
				switch(which) {
				case 0:
					if(favourite) deletefromFav(); else addToFav();
					break;
				case 1:
					i = new Intent(TimetableActivity.this, Search.class);
					i.putExtra("q", line.getName());
					getParent().startActivity(i);
					break;
				case 2:
					i = new Intent(TimetableActivity.this, Search.class);
					i.putExtra("q", stop.getName());
					getParent().startActivity(i);
					break;
				case 3:
					showNotes();
					break;
				case 4:
					i = new Intent(TimetableActivity.this, AutobuserMap.class);
					i.putExtra("latsE6", new int[] {(int) (stop.getLocation().getLatitude() * 1E6)});
					i.putExtra("lonsE6", new int[] {(int) (stop.getLocation().getLongitude() * 1E6)});
					String no = stop.getNo()+"";
					if(no.length() == 6)
						no = no.substring(4);
					i.putExtra("titles", new String[] { stop.getName()+"|"+no } );
					i.putExtra("snippets", new String[] { "" } );
					i.putExtra("time", System.currentTimeMillis());
					getParent().startActivity(i);
					break;
				case 5:
					getTimetable();
					break;
				}
			}
		})
		.create().show();
	}

	protected void showNotes() {
		new AlertDialog.Builder(this).setMessage(timetable.getComments()).create().show();
	}

	private void addToFav() {
		ContentValues values = new ContentValues();
		values.put(MyStop.SQL_KEY_NAME, ((TextView)findViewById(R.id.title)).getText().toString());
		values.put(MyStop.SQL_KEY_LINEID, line.getId());
		values.put(MyStop.SQL_KEY_STOPID, stop.getId());
		//db.beginTransaction();
		getContentResolver().insert(DatabaseProvider.CONTENT_URI_MYSTOP, values);
		timetable.insertDB(getContentResolver());
		//db.setTransactionSuccessful();
		//db.endTransaction();
		toast(R.string.stopAddedToList, Toast.LENGTH_LONG);
		favourite = true;
	}

	protected void deletefromFav() {
		getContentResolver().delete(DatabaseProvider.CONTENT_URI_MYSTOP, MyStop.SQL_KEY_LINEID+"="+line.getId()+" AND "+MyStop.SQL_KEY_STOPID+"="+stop.getId(), null);
		getContentResolver().delete(DatabaseProvider.CONTENT_URI_TIMETABLE, Timetable.SQL_KEY_LINEID+"="+line.getId()+" AND "+Timetable.SQL_KEY_STOPID+"="+stop.getId(), null);
		toast(R.string.stopDeletedFromList, Toast.LENGTH_LONG);
		favourite = false;
	}

	protected void getStopData(final String numer, final int id_przystanku, final String linia) {
		final ProgressDialog pb = ProgressDialog.show(this, null, getResources().getText(R.string.gettingStopData), true, false);
		final Handler handler = new Handler() {
			@Override
		    public void handleMessage(Message msg) {
				pb.dismiss();
				if(msg.what == 0)
					getTimetable();
				else if(msg.what == 1)
					toast(R.string.noStopData, Toast.LENGTH_LONG);
				else
					toast(R.string.noNetwork, Toast.LENGTH_LONG);
	        }
		};
		
		Thread t = new Thread(){
			@SuppressWarnings("unchecked")
			public void run() {
				String url = getString(R.string.queryAutobuserApiZ)+java.net.URLEncoder.encode(numer)+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
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
						long myStop = 0, myLine = 0;
						Cursor c;
						if(json.has("przystanki"))
						{
							JSONObject p = null;
							p = json.getJSONObject("przystanki").getJSONObject(id_przystanku+"");
							ContentValues values = new ContentValues();
							
							c = getContentResolver().query(DatabaseProvider.CONTENT_URI_STOP, new String[] {Stop.SQL_KEY_ID}, Stop.SQL_KEY_NO+"="+id_przystanku, null, null);
							if(!c.moveToFirst())
							{
								c.close();
								values.put(Stop.SQL_KEY_NAME, p.getString("nazwa"));
								values.put(Stop.SQL_KEY_NO, id_przystanku);
								values.put(Stop.SQL_KEY_LAT, p.getDouble("lat"));
								values.put(Stop.SQL_KEY_LON, p.getDouble("lon"));
								myStop = Long.parseLong(getContentResolver().insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
								values.clear();
							} else {
								c.moveToFirst();
								myStop = c.getLong(0);
								c.close();
							}
							stop = new Stop(myStop, getContentResolver());
							
							c = getContentResolver().query(DatabaseProvider.CONTENT_URI_LINE, new String[] {Line.SQL_KEY_ID}, Line.SQL_KEY_NAME+"='"+linia+"'", null, null);
							if(!c.moveToFirst())
							{
								c.close();
								long endStop1;
								long endStop2;
								
								url = getString(R.string.queryAutobuserApiQ)+java.net.URLEncoder.encode(linia)+"&app=android&app_ver=0.8.9b&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

								client = new DefaultHttpClient();
								client.getParams().setParameter("http.socket.timeout", new Integer(1000));
								request = new HttpGet(url);
								request.getParams().setParameter("http.socket.timeout", new Integer(5000));
								response = client.execute(request);
								entity = response.getEntity();
					            if (entity != null) {
					            	in = new BufferedReader(new InputStreamReader(entity.getContent()));
					            	json = new JSONObject(in.readLine());
									Iterator pk = json.getJSONObject("linie").getJSONObject(linia).keys();
									JSONObject rekord1 = (JSONObject) json.getJSONObject("linie").getJSONObject(linia).getJSONArray((String) pk.next()).get(0);
									JSONArray id_przystankow = rekord1.getJSONObject("trasa").getJSONArray("id_przystanku");
									JSONArray nazwy_przystankow = rekord1.getJSONObject("trasa").getJSONArray("nazwa");
									
									endStop1 = selectOrInsertStop(id_przystankow.getString(0), nazwy_przystankow.getString(0));
									
									endStop2 = selectOrInsertStop(id_przystankow.getString(nazwy_przystankow.length()-1), nazwy_przystankow.getString(nazwy_przystankow.length()-1));
									
									values.put(Line.SQL_KEY_NAME, linia);
									values.put(Line.SQL_KEY_END1, endStop1);
									values.put(Line.SQL_KEY_END2, endStop2);
									myLine = Long.parseLong(getContentResolver().insert(DatabaseProvider.CONTENT_URI_LINE, values).getLastPathSegment());
									values.clear();
								}
							}
							else
							{
								myLine = c.getLong(0);
								c.close();
							}
							line = new Line(myLine, getContentResolver());
						}
						handler.sendEmptyMessage(0);
					}
					return;
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					handler.sendEmptyMessage(-1);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					handler.sendEmptyMessage(-1);
					return;
				} catch (JSONException e) {
					e.printStackTrace();
					handler.sendEmptyMessage(1);
					return;
				}
			}
		};
		t.start();
	}
	
	private void getTimetable() {
		final String linia = line.getName();
		final int id_przystanku = stop.getNo();
		final ProgressDialog pb = ProgressDialog.show(this, null, getResources().getText(R.string.gettingTimetableData), true, false);
		final Handler handler = new Handler() {
			@Override
		    public void handleMessage(Message msg) {
				pb.dismiss();
				if(msg.what == 0) {
					timetable.setType(Timetable.isSwieto()?Timetable.TYPE_S:Timetable.TYPE_P);
					source = INTERNET;
					displayTimetable();
				} else if(msg.what == 1)
					toast(R.string.noStopData, Toast.LENGTH_LONG);
				else
					toast(R.string.noNetwork, Toast.LENGTH_LONG);
	        }
		};
		
		Thread t = new Thread(){
			public void run() {
				String url = getString(R.string.queryAutobuserApiQ)+java.net.URLEncoder.encode(linia+":"+id_przystanku)+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
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

						if(json.has("rozklady")) {
							
							JSONObject p = json.getJSONObject("rozklady").getJSONObject(linia+":"+id_przystanku);
							if(p==null) return;
							// TODO Wrzucić do funkcji
							
							//Cursor lineRecord = getContentResolver().query(TimetableProvider.CONTENT_URI, new String[] {TimetableProvider.FIRST_STOP_NO, TimetableProvider.LAST_STOP_NO}, TimetableProvider.LINE_ID+"="+line.getId(), null, null); 
							//lineRecord.moveToFirst();
							int d = ((line.getEnd1().getNo()/100) == Integer.parseInt(p.getString("kierunek_numer")))? 0 : 1;
							//lineRecord.close();
							LinkedList<Departure> departuresP = new LinkedList<Departure>();
							if(!p.isNull("p")) {
								departuresP = Timetable.getDepartures(p.getJSONObject("p"));
							}
							LinkedList<Departure> departuresS = new LinkedList<Departure>();
							if(!p.isNull("s")) {
								departuresS = Timetable.getDepartures(p.getJSONObject("s"));
							}
							timetable = new Timetable(stop.getId(), line.getId(), departuresP, departuresS, p.getString("zmiany"), d);
						}
						handler.sendEmptyMessage(0);
		            } else
		            	handler.sendEmptyMessage(-1);
				} catch (ClientProtocolException e) {
					handler.sendEmptyMessage(-1);
					return;
				} catch (IOException e) {
					handler.sendEmptyMessage(-1);
					return;
				} catch (JSONException e) {
					e.printStackTrace();
					handler.sendEmptyMessage(1);
					return;
				}
			}
		};
		t.start();
	}

	protected long selectOrInsertStop(String sid, String sname) throws ClientProtocolException, IOException, JSONException {
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_STOP, new String[] {Stop.SQL_KEY_ID}, Stop.SQL_KEY_NO+"="+sid, null, null);
		long stop = 0;
		if(!c.moveToFirst())
		{
			c.close();
			if(sid.length()!=6){
				ContentValues values = new ContentValues();
				values.put(Stop.SQL_KEY_NAME, sname);
				values.put(Stop.SQL_KEY_NO, sid);
				values.put(Stop.SQL_KEY_LAT, 0);
				values.put(Stop.SQL_KEY_LON, 0);
				stop = Long.parseLong(getContentResolver().insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
				values.clear();
				return stop;
			}
			String url = getString(R.string.queryAutobuserApiZ)+java.net.URLEncoder.encode(sid.substring(0, 4))+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter("http.socket.timeout", new Integer(1000));
			HttpGet request = new HttpGet(url);
			request.getParams().setParameter("http.socket.timeout", new Integer(5000));

			HttpResponse response;
			{
				response = client.execute(request);
				 
	            HttpEntity entity = response.getEntity();
	            if (entity != null) {
	            	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
	            	JSONObject json = new JSONObject(in.readLine());
					JSONObject p = null;
					if(json.getJSONObject("przystanki").has(sid))
					{
						p = json.getJSONObject("przystanki").getJSONObject(sid);
					} else {
						p = json.getJSONObject("przystanki").getJSONObject((String) json.getJSONObject("przystanki").keys().next());
					}
					ContentValues values = new ContentValues();
					values.put(Stop.SQL_KEY_NAME, sname);
					values.put(Stop.SQL_KEY_NO, sid);
					values.put(Stop.SQL_KEY_LAT, p.getDouble("lat"));
					values.put(Stop.SQL_KEY_LON, p.getDouble("lon"));
					stop = Long.parseLong(getContentResolver().insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
					values.clear();
				}
			}
		}
		else {
			stop = c.getLong(0);
			c.close();
		}
		
		return stop;
	}
	protected void toast(int i, int lengthLong) {
		Toast.makeText(this, i, lengthLong).show();
	}

	public void locationChanges(Location newLocation) {
		mLocation.set(newLocation);
		redrawGeoViews();
	}

	public void minuteChanges() {
		refreshTimes();
	}

	public void orientationChanges(float[] newOrientation) {
		mValues = newOrientation;
		redrawGeoViews();
	}
}
