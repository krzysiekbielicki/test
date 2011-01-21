package pl.skyman.autobuser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
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

import pl.skyman.autobuser.ZiLStop.ZilStopDir;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Search extends Activity implements AutobuserActivity{
	
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_VALUE = "value";
	public static final String SQL_TABLE_NAME = "searchhistory";
	public static final String SQL_KEY_TIME = "time";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
													SQL_TABLE_NAME + " (" + 
													SQL_KEY_ID + " integer primary key autoincrement, " +
													SQL_KEY_VALUE + " text not null, " +
													SQL_KEY_TIME + " numeric not null DEFAULT CURRENT_TIMESTAMP, " +
													"CONSTRAINT U_SearchHistory UNIQUE ("+SQL_KEY_VALUE+"));";
	private LinkedList<ZiLStop> stops;
	protected boolean zespoly;
	protected String q, k1, k2, zespol, linia;
	@Override
	protected void onResume() {
		getParent().setTitle(R.string.search);
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		stops = new LinkedList<ZiLStop>();
		q = getIntent().getStringExtra("q");
		findZiL();
	}
	
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		stops = new LinkedList<ZiLStop>();
		q = intent.getStringExtra("q");
		findZiL();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
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
				stop = Integer.parseInt(getContentResolver().insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
				values.clear();
				return stop;
			}
			String url = getString(R.string.queryAutobuserApiZ)+java.net.URLEncoder.encode(sid.substring(0, 4))+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter("http.socket.timeout", new Integer(1000));
			HttpGet request = new HttpGet(url);
			request.getParams().setParameter("http.socket.timeout", new Integer(5000));

			HttpResponse response;
			//try {
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
					stop = Integer.parseInt(getContentResolver().insert(DatabaseProvider.CONTENT_URI_STOP, values).getLastPathSegment());
					values.clear();
				}
		}
		else {
			stop = c.getLong(0);
			c.close();
		}
		
		return stop;
	}
	protected void findZiL() {
		final ProgressDialog pb = ProgressDialog.show(this, null, getResources().getText(R.string.findingZiL), true, false);
		stops.clear();
		final Handler handler = new Handler() {
			@Override
		    public void handleMessage(Message msg) {
				if(msg.what == 0)
					updateFoundList();
				else if(msg.what == 1)
					((Autobuser)getParent()).toast(R.string.noLineData, Toast.LENGTH_LONG);
				else
					((Autobuser)getParent()).toast(R.string.noNetwork, Toast.LENGTH_LONG);
				pb.dismiss();
	        }
		};
		Thread t = new Thread(){
			@SuppressWarnings("unchecked")
			public void run() {
				String url = getString(R.string.queryAutobuserApiQ)+java.net.URLEncoder.encode(q)+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
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
						if(json.has("zespoly"))
						{
							zespoly = true;
							Iterator zespoly = json.getJSONObject("zespoly").keys();
							Iterator it;
							while(zespoly.hasNext())
							{
								zespol = (String)zespoly.next();
								if(json.getJSONObject("zespoly").getJSONObject(zespol).has("autobusy"))
								{
									it = json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("autobusy").keys();
									while(it.hasNext())
										stops.add(addByStop( json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("autobusy").getJSONObject((String) it.next()) ));
								}
								if(json.getJSONObject("zespoly").getJSONObject(zespol).has("tramwaje"))
								{
									it = json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("tramwaje").keys();
									while(it.hasNext())
										stops.add(addByStop( json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("tramwaje").getJSONObject((String) it.next()) ));
								}
								if(json.getJSONObject("zespoly").getJSONObject(zespol).has("nocne"))
								{
									it = json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("nocne").keys();
									while(it.hasNext())
										stops.add(addByStop( json.getJSONObject("zespoly").getJSONObject(zespol).getJSONObject("nocne").getJSONObject((String) it.next()) ));
								}
							}
						}
						else if(json.has("linie"))
						{
							zespoly = false;
							Iterator linie = json.getJSONObject("linie").keys();
							ZiLStop stop = null;
							while(linie.hasNext())
							{
								linia = (String) linie.next();
								Iterator pk = json.getJSONObject("linie").getJSONObject(linia).keys();
								
								int index = 0;
								String k = (String) pk.next();
								if(Timetable.isSwieto() && json.getJSONObject("linie").getJSONObject(linia).getJSONArray(k).length() > 1)
									index = 1;
								else 
									index = 0;
								
								JSONObject rekord1 = (JSONObject) json.getJSONObject("linie").getJSONObject(linia).getJSONArray(k).get(index);
								String l = rekord1.getString("linia");
								JSONArray nazwy_przystankow1 = rekord1.getJSONObject("trasa").getJSONArray("nazwa");
								JSONArray id_przystankow1 = rekord1.getJSONObject("trasa").getJSONArray("id_przystanku");
								k1 = rekord1.getString("koniec_nazwa");
								JSONArray nazwy_przystankow2;
								JSONArray id_przystankow2;
								if(pk.hasNext()) {
									k = (String) pk.next();
									if(Timetable.isSwieto() && json.getJSONObject("linie").getJSONObject(linia).getJSONArray(k).length() > 1)
										index = 1;
									else 
										index = 0;
									JSONObject rekord2 = (JSONObject) json.getJSONObject("linie").getJSONObject(linia).getJSONArray(k).get(index);
									nazwy_przystankow2 = rekord2.getJSONObject("trasa").getJSONArray("nazwa");
									k2 = rekord2.getString("koniec_nazwa");
									id_przystankow2 = rekord2.getJSONObject("trasa").getJSONArray("id_przystanku");
								} else {
									nazwy_przystankow2 = new JSONArray();
									id_przystankow2 = new JSONArray();
									k2 = "";
								}
								int i = 0;
								while( i < nazwy_przystankow1.length() - 1 || i < nazwy_przystankow2.length() - 1 )
								{
									stop = new ZiLStop();
									stop.nazwa = "";//nazwy_przystankow1.getString(i);
									stop.linia = l;
									if(i < nazwy_przystankow1.length()-1)
									{
										stop.dir1.kierunek = nazwy_przystankow1.getString(i);
	                                    stop.dir1.id_przystanku = id_przystankow1.getString(i);
	                                    if(stop.dir1.id_przystanku.length()==6)
											stop.dir1.numer = stop.dir1.id_przystanku.substring(0,4);
										else
											stop.dir1.numer = stop.dir1.id_przystanku;
	                                    //stop.dir1.numer_przystanku = id_przystankow1.getString(i).substring(4);
									}
                                
                                    if(i < nazwy_przystankow2.length()-1)
                                    {
	                                    stop.dir2.kierunek = nazwy_przystankow2.getString(i);
										stop.dir2.id_przystanku = id_przystankow2.getString(i);
										if(stop.dir2.id_przystanku.length()==6)
											stop.dir2.numer = stop.dir2.id_przystanku.substring(0,4);
										else
											stop.dir2.numer = stop.dir2.id_przystanku;
	                                    //stop.dir2.numer_przystanku = id_przystankow2.getString(nazwy_przystankow2.length()-1-j).substring(4);
                                    }
                                    i++;
                                    stops.add(stop);
								}
							}
						}
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					handler.sendEmptyMessage(-1);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(1);
				}
				handler.sendEmptyMessage(0);
			}

		};
		t.start();
	}
	
	@SuppressWarnings("unchecked")
	private ZiLStop addByStop(JSONObject object) throws JSONException {
		Iterator kierunki = object.keys();
		JSONObject przystanek = object.getJSONObject((String) kierunki.next());
		ZiLStop stop = new ZiLStop();
		stop.linia = (String) przystanek.get("linia");
		stop.dir2.kierunek = (String) przystanek.get("kierunek");
        stop.dir2.id_przystanku = (String) przystanek.get("id_przystanku");
        stop.dir2.numer = (String) przystanek.get("numer");
        //stop.dir2.numer_przystanku = (String) przystanek.get("numer_przystanku");
        
        if(kierunki.hasNext())
        {
        	przystanek = object.getJSONObject((String) kierunki.next());
			stop.dir1.kierunek = (String) przystanek.get("kierunek");
            stop.dir1.id_przystanku = (String) przystanek.get("id_przystanku");
            stop.dir1.numer = (String) przystanek.get("numer");
            //stop.dir1.numer_przystanku = (String) przystanek.get("numer_przystanku");
        }
        
        stop.nazwa = (String) przystanek.get("nazwa");
        return stop;
	}
	protected void updateFoundList() {
		if(zespoly) {
			Collections.sort(stops, new Comparator<ZiLStop>() {
				public int compare(ZiLStop arg0, ZiLStop arg1) {
					if( arg0.linia.length() == arg1.linia.length()  )
						return arg0.linia.compareToIgnoreCase(arg1.linia);
					else
						if( (arg0.linia.length() == 1 && arg1.linia.length() == 2) || (arg0.linia.length() == 2 && arg1.linia.length() == 1) )
							return arg0.linia.compareToIgnoreCase(arg1.linia);
						else
							return 0;
				}
			});
			((TextView)findViewById(R.id.headerText)).setText(String.format(getResources().getString(R.string.group), zespol));
			((TextView)findViewById(R.id.headerLeft)).setVisibility(View.GONE);
			((TextView)findViewById(R.id.headerLeft)).setVisibility(View.GONE);
		} else {
			((TextView)findViewById(R.id.headerText)).setText(String.format(getResources().getString(R.string.line), linia));
			((TextView)findViewById(R.id.headerLeft)).setText(String.format(getResources().getString(R.string.to), k1) );
			((TextView)findViewById(R.id.headerRight)).setText(String.format(getResources().getString(R.string.to), k2) );
		}
		if(!stops.isEmpty()) {
			saveSearch();
			ListView list = (ListView) findViewById(android.R.id.list);
			list.setFocusable(false);
			list.setItemsCanFocus(true);
			list.setAdapter(new ZiLStopsListAdapter(this, R.layout.zilstopview, stops, zespoly, new ZiLListener() {
				public void onClick(int i, int dir) {
					//getStopData(stops.get(i), dir);
					ZiLStop stop = stops.get(i); 
					Intent intent = new Intent(Search.this, TimetableActivity.class);
					intent.putExtra("linia", stop.linia);
					intent.putExtra("nazwa", stop.nazwa);
					ZilStopDir stopDir = (dir == 0)?stop.dir1:stop.dir2;
					intent.putExtra("kierunek", stopDir.kierunek);
					intent.putExtra("id_przystanku", Integer.parseInt(stopDir.id_przystanku));
					intent.putExtra("numer", stopDir.numer);
					((Autobuser)getParent()).startActivity(intent);
				}
			}));
			((View)findViewById(R.id.header)).setVisibility(View.VISIBLE);
			((View)findViewById(android.R.id.empty)).setVisibility(View.GONE);
		} else {
			((View)findViewById(R.id.header)).setVisibility(View.GONE);
			((View)findViewById(android.R.id.empty)).setVisibility(View.VISIBLE);
		}
	}
	
	public void saveSearch() {
		ContentValues values = new ContentValues();
		values.put(SQL_KEY_VALUE, q);
		getContentResolver().insert(DatabaseProvider.CONTENT_URI_SEARCHHISTORY, values);
	}

	public void locationChanges(Location newLocation) {
		
	}

	public void minuteChanges() {
		
	}

	public void orientationChanges(float[] newOrientation) {
		
	}

	public void setTitle(String title) {
		
	}
}
