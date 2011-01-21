package pl.skyman.autobuser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.LocalActivityManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class Autobuser extends ActivityGroup {
	public static final String APP_VERSION = "1.0.0";
	static final String SETTINGS_KEY_SYNCED = "synced";
	static final String SETTINGS_KEY_LASTUPDATEQUESTION = "lastupdatequestion";
	private static final int LOCATION_UPDATE_INTERVAL_MILLIS = 1000;
	
	
	private SensorManager mSensorManager;
	protected float angle = 0;
	private final SensorEventListener mListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}
		public void onSensorChanged(SensorEvent event) {
			float[] values = event.values;
			values[0] += angle;
			((AutobuserActivity)getCurrentActivity()).orientationChanges(values);
		}
    };
	protected Location mLocation;
	protected boolean mGpsAvailable;
	protected boolean mNetworkAvailable;
	protected long mLastGpsFixTime = Long.MIN_VALUE;
	protected boolean mHaveLocation;
    
    private final LocationListener mLocationListener = new LocationListener() {
    	private static final long RETAIN_GPS_MILLIS = 10000L;

		public void onLocationChanged(Location location) {
			if (!mHaveLocation) {
	            mHaveLocation = true;
	        }

	        final long now = SystemClock.uptimeMillis();
	        boolean useLocation = false;
	        final String provider = location.getProvider();
	        if (LocationManager.GPS_PROVIDER.equals(provider)) {
	            // Use GPS if available
	            mLastGpsFixTime = SystemClock.uptimeMillis();
	            useLocation = true;
	        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
	            // Use network provider if GPS is getting stale
	            useLocation = now - mLastGpsFixTime > RETAIN_GPS_MILLIS;
	            mLastGpsFixTime = 0L;
	        }
	        if (useLocation) {
	        	if (mLocation == null)
	                mLocation = new Location(location);
	            else
	                mLocation.set(location);
	        	((AutobuserActivity)getCurrentActivity()).locationChanges(mLocation);
	        }
		}
		public void onProviderDisabled(String arg0) {}
		public void onProviderEnabled(String arg0) {}
		public void onStatusChanged(String provider, int status, Bundle extras) { 
	        
	        if (LocationManager.GPS_PROVIDER.equals(provider)) {
	            switch (status) {
	            case LocationProvider.AVAILABLE:
	                mGpsAvailable = true;
	                break;
	            case LocationProvider.OUT_OF_SERVICE:
	            case LocationProvider.TEMPORARILY_UNAVAILABLE:
	                mGpsAvailable = false;
	                
	                if (mNetLocation != null && mNetworkAvailable) {
	                    // Fallback to network location
	                    mLastGpsFixTime = 0L;
	                    onLocationChanged(mNetLocation);
	                } else {
	                    //handleUnknownLocation();
	                }
	             
	                break;
	            }

	        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
	            switch (status) {
	            case LocationProvider.AVAILABLE:
	                mNetworkAvailable = true;
	                break;
	            case LocationProvider.OUT_OF_SERVICE:
	            case LocationProvider.TEMPORARILY_UNAVAILABLE:
	                mNetworkAvailable = false;
	                
	                if (!mGpsAvailable) {
	                    //handleUnknownLocation();
	                }
	                break;
	            }
	        }
	    }

    };
    
    private Runnable mTimerTask = new Runnable() {
		public void run() {
			mStartTime += 60*1000;
			//mLocation = ((LocationManager)getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			getLocation();
			handler.postAtTime(this, mStartTime);//(this, 1000);//60*1000);
			((AutobuserActivity)getCurrentActivity()).minuteChanges();
		}
    };
	private long mStartTime;
	private LocalActivityManager mLocalActivityManager;
	private SharedPreferences settings;
	private LinkedList<Intent> activities;
	private Location mNetLocation;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activities = new LinkedList<Intent>();
        handler = new Handler(); 
    	//*FOR_EMU
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        /*FOR_EMU*/
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        	new AlertDialog.Builder(this)
            .setMessage(R.string.showLocationSettingsquestion)
    		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	Intent i = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
                	startActivity(i);
                }
            })
            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked Cancel so do some stuff */
                }
            })
            .create().show();
        }
        mLocalActivityManager = getLocalActivityManager();
        settings = getSharedPreferences(Autobuser.PREFS_NAME, 0);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        	String[] data = getIntent().getDataString().split("/");
        	if(data[0].equals("search")) {
        		Intent i = new Intent(Autobuser.this, Search.class);
        		i.putExtra("q", data[1]);
        		startActivity(i);
        	} else if(data[0].equals("route")) {
        		Intent i = new Intent(Autobuser.this, Route.class);
				i.putExtra("start", data[1]);
				i.putExtra("destination", data[2]);
				startActivity(i);
        	}
        }
        else {
	        if(!settings.contains(SETTINGS_KEY_SYNCED))
	        	new AlertDialog.Builder(this)
	            .setMessage(R.string.syncZiLquestion)
	    		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	syncZespolyILinie(Autobuser.this);
	                }
	            })
	            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	
	                    /* User clicked Cancel so do some stuff */
	                }
	            })
	            .create().show();
	        startActivity(new Intent(Autobuser.this, Favourites.class));
        }
    }

	@Override
	protected void onPause() {
		//*FOR_EMU 
		mSensorManager.unregisterListener(mListener);
		/*FOR_EMU */
		mLocationManager.removeUpdates(mLocationListener);
		mLocationManager.removeUpdates(mLocationListener);
		handler.removeCallbacks(mTimerTask);
		super.onPause();
	}
	
	@Override
    protected void onStop()
    {
		//*FOR_EMU 
        mSensorManager.unregisterListener(mListener);
        /*FOR_EMU */
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.removeUpdates(mLocationListener);
        handler.removeCallbacks(mTimerTask);
        mLocalActivityManager.removeAllActivities();
        //refreshTimesTask.cancel();
        super.onStop();
    }

	@Override
	protected void onResume() {
		super.onResume();
		//*FOR_EMU
		List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		if(sensors.size() > 0)
			mSensorManager.registerListener(mListener, sensors.get(0), SensorManager.SENSOR_DELAY_UI);
		/*FOR_EMU */
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI, new String[] {"MIN("+DatabaseProvider.UPDATETIME+")"}, null, null, null);
		if(c.moveToFirst()) {
			long l = c.getLong(0);
			c.close();
			long time = System.currentTimeMillis();
	        if( l > 0 && ( l + 14*24*60 ) * 60 * 1000 < time && ( settings.getLong(SETTINGS_KEY_LASTUPDATEQUESTION, 0) + 24*60*60*1000 < time)) {
	        	new AlertDialog.Builder(this)
	            .setMessage(R.string.updateTimetablesQuestion)
	    		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	updateTimetables(Autobuser.this, null);
	                }
	            })
	            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	
	                    /* User clicked Cancel so do some stuff */
	                }
	            })
	            .create().show();
	        	settings.edit().putLong(SETTINGS_KEY_LASTUPDATEQUESTION, time).commit();
	        }
		}
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL_MILLIS, 1, mLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL_MILLIS, 1, mLocationListener);
		handler.removeCallbacks(mTimerTask);
		mStartTime = SystemClock.uptimeMillis();
		mStartTime += 50 + (60*1000) - (System.currentTimeMillis()%(60*1000));
		handler.postAtTime(mTimerTask, mStartTime);
		angle = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE?90:0;
		restartLastActivity();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		angle = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE?90:0;
	}



	public static final String PREFS_NAME = "pl.skyman.autobuser_preferences";
	
	public static final int SORT_BY_NAME = 0;
	public static final int SORT_BY_DISTANCE = 1;
//	private static final int DONT_SORT = 2;
	
	private LocationManager mLocationManager;
	private Handler handler;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//AutobuserMenu.createMenu(menu);
		(new MenuInflater(this)).inflate(R.menu.mainmenu, menu);
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && !activities.isEmpty()) {
			mLocalActivityManager.destroyActivity(activities.removeLast().toString(), true);
			if(!activities.isEmpty()) { 
				restartLastActivity();
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			onSearchRequested();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		//super.onOptionsItemSelected(item);
		// Find which menu item has been selected
		try {
			activities.clear();
			activities.add(new Intent(Autobuser.this, Favourites.class));
			mLocalActivityManager.removeAllActivities();
		switch (item.getItemId()) {
			// Check for each known menu item
			case (R.id.mainmenu_favorites):
				startActivity(new Intent(Autobuser.this, Favourites.class));
			return true;
			case (R.id.mainmenu_search):
				startActivity(new Intent(Autobuser.this, SearchForm.class));
			return true;
			case (R.id.mainmenu_settings):
				//startActivity(new Intent(Autobuser.this, pl.skyman.autobuser.Settings.class));
				super.startActivity(new Intent(Autobuser.this, pl.skyman.autobuser.Settings.class));
			return true;
			case (R.id.mainmenu_directions):
				startActivity(new Intent(Autobuser.this, RouteForm.class));
			return true;
			case (R.id.mainmenu_help):
				startActivity(new Intent(Autobuser.this, Help.class));
			return true;
		}
		}catch (IllegalStateException e) {}
		// Return false if you have not handled the menu item.
		return false;
	}
	
	 /*@Override
	 public boolean onSearchRequested() {
	    return false;
	 }*/
	
	protected void toast(int i, int lengthLong) {
		Toast.makeText(this, i, lengthLong).show();
	}
	
	public SharedPreferences getSettings() {
		return settings;
	}
	
	public static void syncZespolyILinie(final Context context) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		final WakeLock sWakeLock = pm.newWakeLock(	PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "AutobuserWL");
		sWakeLock.acquire();
        final ProgressDialog pb = ProgressDialog.show(context, null, context.getResources().getText(R.string.syncingZiL), true, false);
		final Handler handler = new Handler() {
			@Override
		    public void handleMessage(Message msg) {
				pb.dismiss();
				sWakeLock.release();
				if(msg.what == 1)
					Toast.makeText(context, R.string.noNetwork, Toast.LENGTH_LONG).show();
				else if(msg.what == 0)
					context.getSharedPreferences(Autobuser.PREFS_NAME, 0).edit().putBoolean(SETTINGS_KEY_SYNCED, true).commit();
				else
					Toast.makeText(context, R.string.ZILcheckUpdate, Toast.LENGTH_LONG).show();
	        }
		};
		
		
		Thread t = new Thread(){
			public void run() {
				String url = context.getString(R.string.syncZespolyILinieURL);//"+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
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
		            	String zespoly = in.readLine().replaceFirst("nazwy_zespolow=\\['", "").replaceFirst("'\\]", "");
						String linie = in.readLine().replaceFirst("nazwy_linii=\\['", "").replaceFirst("'\\]", "");
						
					    String[] zil = (zespoly+"','"+linie).split("','");
					    
					    char[] chars1 = "ŻÓŁĆĘŚĄŹŃ".toCharArray();
						char[] chars2 = "ZOLCESAZN".toCharArray();
						int size = chars1.length;
						for(int j = 0; j<size; j++)
							zespoly = zespoly.replace(chars1[j], chars2[j]);
						
						String[] zil2 = zespoly.split("','");
						size = zil2.length;
						ContentResolver cr = context.getContentResolver();
						cr.delete(DatabaseProvider.CONTENT_URI_ZIL, null, null);
					    int i;
					    ContentValues[] values = new ContentValues[zil.length];
					    ContentValues v;
					    for(i = 0; i< size; i++)
					    {
					    	v = new ContentValues();
					    	v.put("name", zil[i]);
					    	v.put(ZiL.SQL_KEY_NAME2, zil2[i]);
					    	//cr.insert(DatabaseProvider.CONTENT_URI_ZIL, values);
					    	values[i] = v;
					    }
					    size = zil.length;
						for(i = i; i< size; i++)
					    {
							v = new ContentValues();
					    	v.put("name", zil[i]);
					    	v.put(ZiL.SQL_KEY_NAME2, zil[i]);
					    	//cr.insert(DatabaseProvider.CONTENT_URI_ZIL, values);
					    	values[i] = v;
					    	
					    }
						cr.bulkInsert(DatabaseProvider.CONTENT_URI_ZIL, values);
					    handler.sendEmptyMessage(0);
					} else {
						handler.sendEmptyMessage(2);
					}
				} catch (ClientProtocolException e) {
					handler.sendEmptyMessage(0);
				} catch (IOException e) {
					handler.sendEmptyMessage(1);
				}
			}
		};
		t.start();
	}
	
	public static void updateTimetables(final Context context, final Handler callbackHandler) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		final WakeLock sWakeLock = pm.newWakeLock(	PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "AutobuserWL");
		sWakeLock.acquire();
		final ProgressDialog pb = ProgressDialog.show(context, null, context.getResources().getText(R.string.updatingTimetables), true, false);
		
		final Handler handler = new Handler() {
			@Override
		    public void handleMessage(Message msg) {
				pb.dismiss();
				if(callbackHandler != null)
					callbackHandler.sendEmptyMessage(0);
				sWakeLock.release();
				if(msg.what == 0) {
//					syncZespolyILinie(context);
				} else if(msg.what == 1)
					Toast.makeText(context, R.string.noStopData, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(context, R.string.noNetwork, Toast.LENGTH_LONG).show();
	        }
		};
		
		Thread t = new Thread(){
			public void run() {
				String linia, id_przystanku;
				long lineId, stopId;
				Cursor myStops = context.getContentResolver().query(DatabaseProvider.CONTENT_URI, new String[] {DatabaseProvider.LINE_NAME, DatabaseProvider.STOP_NO, DatabaseProvider.LINE_ID, DatabaseProvider.STOP_ID, DatabaseProvider.FIRST_STOP_NO, DatabaseProvider.LAST_STOP_NO}, null, null, null);
				try {
					while(myStops.moveToNext()) {
						linia = myStops.getString(0);
						id_przystanku = myStops.getString(1);
						lineId = myStops.getLong(2);
						stopId = myStops.getLong(3);
						String url = context.getString(R.string.queryAutobuserApiQ)+java.net.URLEncoder.encode(linia+":"+id_przystanku)+"&app=android&app_ver="+Autobuser.APP_VERSION+"&app_uniq="+(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
						HttpClient client = new DefaultHttpClient();
						client.getParams().setParameter("http.socket.timeout", new Integer(1000));
						HttpGet request = new HttpGet(url);
						request.getParams().setParameter("http.socket.timeout", new Integer(5000));
		
						HttpResponse response;
						response = client.execute(request);
						 
			            HttpEntity entity = response.getEntity();
			            if (entity != null) {
			            	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
			            	JSONObject json = new JSONObject(in.readLine());
							if(json.has("rozklady")) {
								
								JSONObject p = json.getJSONObject("rozklady").getJSONObject(linia+":"+id_przystanku);
								if(p==null) return;
								
								int d = ((myStops.getInt(4)/100) == Integer.parseInt(p.getString("kierunek_numer")))? 0 : 1;
								LinkedList<Departure> departuresP = new LinkedList<Departure>();
								if(!p.isNull("p")) {
									departuresP = Timetable.getDepartures(p.getJSONObject("p"));
								}
								LinkedList<Departure> departuresS = new LinkedList<Departure>();
								if(!p.isNull("s")) {
									departuresS = Timetable.getDepartures(p.getJSONObject("s"));
								}
								(new Timetable(stopId, lineId, departuresP, departuresS, p.getString("zmiany"), d)).updateDB(context.getContentResolver());
							}
						}
					}
					handler.sendEmptyMessage(0);
				} catch (ClientProtocolException e) {
					handler.sendEmptyMessage(-1);
					return;
				} catch (IOException e) {
					handler.sendEmptyMessage(-1);
					return;
				} catch (JSONException e) {
					handler.sendEmptyMessage(1);
					return;
				}
			}
		};
		t.start();
	}

	public void startActivity(Intent i) {
		if(i.getAction() != null && i.getAction().startsWith("android.settings")) {
			super.startActivity(i);
		}
		else {
			if(i.getComponent().getClassName().equals("pl.skyman.autobuser.Favourites"))
				activities.clear();
			activities.add(i);
			setContentView(mLocalActivityManager.startActivity(i.toURI(), i).getDecorView());
		}
	}
	
	public void restartLastActivity() {
		try {
			setContentView(mLocalActivityManager.startActivity(activities.getLast().toURI(), activities.getLast()).getDecorView());
		} catch( Exception e) {
		}
	}
	
	public Location getLocation() {
		if(mLocation == null) {
			mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if( mLocation != null && location != null && mLocation.getTime() < location.getTime() )
	        	mLocation = location;
			else if(mLocation == null && location != null )
	        	mLocation = location;
			else if(mLocation == null)
	        	mLocation = new Location(LocationManager.NETWORK_PROVIDER);
		}
		return mLocation;
	}
	
	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

}