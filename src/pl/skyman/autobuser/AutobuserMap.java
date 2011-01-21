package pl.skyman.autobuser;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;

public class AutobuserMap extends MapActivity implements AutobuserActivity {

	private Projection projection;
	private MapController mc;
	private AutobuserOverlay overlay;
	private MyLocationOverlay mLocationOverlay;
	private MapView map;
	private int point = 0;
	private View prevStop;
	private View nextStop; 
	@Override
	protected void onResume() {
		super.onResume();
		getParent().setTitle(R.string.map);
		mLocationOverlay.enableMyLocation();
	}
	
	@Override
	protected void onPause() {
		mLocationOverlay.disableMyLocation();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.map);
		map = (MapView) findViewById(R.id.mapview);
		map.setSatellite(false);
		projection = map.getProjection();
		//map = new MapView(this, "0tyDgXhrMMEaDthb6_COLAC2t5hXfdQLjjhNWog"); //DEBUG-duzy
		//0tyDgXhrMMEZ3sYVYgIUjba6LIdAFicVAjaj5eQ //DEBUG-laptop
		//MapView map = new MapView(this, "0tyDgXhrMMEbsr2RBRF_G_ST3J9OC3MZVKHyWiw"); //RELEASE
		mc = map.getController();
		overlay = new AutobuserOverlay(getResources().getDrawable(R.drawable.icon));
		onNewIntent(getIntent());
		
		map.getOverlays().add(overlay);
		//map.setBuiltInZoomControls(true);
        mLocationOverlay = new MyLocationOverlay(this, map);
        map.getOverlays().add(mLocationOverlay);
	}

	@Override
	public void onNewIntent(Intent newIntent) {
		point = newIntent.getIntExtra("point", 0);
		overlay.clear();
		//super.onNewIntent(newIntent);
		overlay.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChanged(ItemizedOverlay overlay,	OverlayItem newFocus) {
				point = overlay.getLastFocusedIndex();
				updateMapControls();
			}
		});
		((LinearLayout) findViewById(R.id.toolsview)).removeAllViews();
		((LinearLayout) findViewById(R.id.toolsview)).addView(map.getZoomControls());
		/*if(newIntent.hasExtra("latE6")) {
			drawLine = false;
			GeoPoint point = new GeoPoint(newIntent.getIntExtra("latE6", 0), newIntent.getIntExtra("lonE6", 0));
	        mc.setZoom(15);
	        String text = newIntent.getStringExtra("title");
	        overlay.addOverlay(new OverlayItem(point, text, "b"));
		} else {*/
			int[] lats = newIntent.getIntArrayExtra("latsE6");
			int[] lons = newIntent.getIntArrayExtra("lonsE6");
			String[] titles = newIntent.getStringArrayExtra("titles");
			String[] snippets = newIntent.getStringArrayExtra("snippets");
			int size = lats.length;
		    for( int i = 0; i< size; i++) {
				GeoPoint point = new GeoPoint(lats[i], lons[i]);
			    mc.setZoom(15);
			    overlay.addOverlay(new OverlayItem(point, titles[i], snippets[i]));
			}
		    if(lats.length>1)
		    	((LinearLayout) findViewById(R.id.toolsview)).addView(getMapControls());
		    else
		    	mc.animateTo(overlay.getItem(0).getPoint());
	}
	
	private View getMapControls() {
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflater.inflate(R.layout.mapcontrols, null);
		prevStop = v.findViewById(R.id.prev);
		nextStop = v.findViewById(R.id.nxt);
		v.findViewById(R.id.prev).setOnClickListener(new OnClickListener() {
			public void onClick(View w) {
				point = overlay.getLastFocusedIndex() - 1;
				overlay.setFocus(point);
				updateMapControls();
			}
		});
		v.findViewById(R.id.nxt).setOnClickListener(new OnClickListener() {
			public void onClick(View w) {
				point = overlay.getLastFocusedIndex() + 1;
				overlay.setFocus(point);
				updateMapControls();
			}
		});
		overlay.setFocus(point);
		updateMapControls();
		return v;
	}
	
	private void updateMapControls() {
		if(prevStop != null) {
			prevStop.setEnabled(point > 0);
			nextStop.setEnabled(point < overlay.size()-1);
		}
		OverlayItem ovl = overlay.createItem(point);
		Rect bounds = ovl.getMarker(0).getBounds();
		Point p = new Point();
		projection.toPixels(ovl.getPoint(), p);
		if(bounds.width() > 160)
			p.offset(bounds.width() - 165, 0);
		mc.animateTo(projection.fromPixels(p.x, p.y));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
	

	public void locationChanges(Location newLocation) {
	}

	public void minuteChanges() {
	}

	public void orientationChanges(float[] newOrientation) {
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected boolean isLocationDisplayed() {
		return true;
	}

	public class AutobuserOverlay extends ItemizedOverlay<OverlayItem> {
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private Paint textPaint, noPaint, pointPaint, snippetPaint;
		int padding = 4;
		private final int TOUCH_SPAN = 35;
		private Paint linePaint;
		Rect r = new Rect();
		Drawable d;
		private int prevFocus;
		public AutobuserOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
			textPaint = new Paint();
			textPaint.setColor(Color.BLACK);
			textPaint.setTextSize(16);
			textPaint.setAntiAlias(true);
			snippetPaint = new Paint(textPaint);
			snippetPaint.setTextSize(14);
			noPaint = new Paint();
			noPaint.setTextSize(16);
			noPaint.setAntiAlias(true);
			noPaint.setColor(Color.WHITE);
			pointPaint = new Paint();
			pointPaint.setColor(Color.RED);
			pointPaint.setAntiAlias(true);
			linePaint = new Paint();
			linePaint.setColor(Color.BLUE);
			linePaint.setStrokeWidth(7);
			linePaint.setAlpha(40);
			linePaint.setAntiAlias(true);
		}
		
		public void clear() {
			mOverlays.clear();
		}

		public void setFocus(int i) {
			setLastFocusedIndex(i);
		}
		
		public void addOverlay(OverlayItem overlay) {
		    mOverlays.add(overlay);
		    populate();
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			OverlayItem ovl = mOverlays.get(i);
			Point mPoint = new Point();
            projection.toPixels(ovl.getPoint(), mPoint);
            if(prevFocus == getLastFocusedIndex()) {
				Drawable d = ovl.getMarker(0);
				Rect bounds = d.getBounds();
				d.setBounds(mPoint.x, mPoint.y-bounds.height(), mPoint.x+bounds.width(), mPoint.y);
				ovl.setMarker(d);
				return ovl;
			}/**/
			String text = ovl.getTitle();
			String[] a = text.split("\\|");
			if(point == i && size() > 1) {
				int width;
				if(a.length == 1) {
					d = (NinePatchDrawable) getResources().getDrawable(R.drawable.popup_addr_expanded);
	                textPaint.getTextBounds(text, 0, text.length(), r);
	                width = r.width();
	                snippetPaint.getTextBounds(ovl.getSnippet(), 0, ovl.getSnippet().length(), r);
	                width = Math.max(width, r.width());
	                d.setBounds(mPoint.x, mPoint.y-60, mPoint.x+width+(39-r.height()), mPoint.y);
	                //d.setBounds(100, 100, 100, 100);
				} else {
					d = (NinePatchDrawable) getResources().getDrawable(R.drawable.popup_stop_expanded);
	                textPaint.getTextBounds(a[0], 0, a[0].length(), r);
	                width = r.width();
	                snippetPaint.getTextBounds(ovl.getSnippet(), 0, ovl.getSnippet().length(), r);
	                width = Math.max(width, r.width());
	                d.setBounds(mPoint.x, mPoint.y-60, mPoint.x+width+(39-r.height())+30, mPoint.y);
				}
			} else {
				if(a.length == 1) {
					d = (NinePatchDrawable) getResources().getDrawable(R.drawable.popup_addr);
	                textPaint.getTextBounds(text, 0, text.length(), r);
	                d.setBounds(mPoint.x, mPoint.y-39, mPoint.x+r.width()+(39-r.height()), mPoint.y);
				} else {
					d = (NinePatchDrawable) getResources().getDrawable(R.drawable.popup_stop);
	                textPaint.getTextBounds(a[0], 0, a[0].length(), r);
	                d.setBounds(mPoint.x, mPoint.y-39, mPoint.x+r.width()+(39-r.height())+30, mPoint.y);
				}
			}
            ovl.setMarker(d);
			return ovl;
		}

		@Override
		public int size() {
			return mOverlays.size();
		}
		
		@Override  
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {  
			int size = mOverlays.size();
			Rect r = new Rect();
			String[] text = new String[size];
			String[] no = new String[size];
			Point[] point = new Point[size];
			OverlayItem ovl;
			String[] a;
			for (int i = 0; i < size; i++) {
                ovl = createItem(i);
                point[i] = new Point();
                projection.toPixels(ovl.getPoint(), point[i]);
                if(i > 0)
                	canvas.drawLine(point[i-1].x, point[i-1].y, point[i].x, point[i].y, linePaint);
                text[i] = ovl.getTitle();
                no[i] = ovl.getSnippet();
            }
			int width;
			int i;
	    
			for (i = 0; i < size; i++) {
				if(i == getLastFocusedIndex() && size() > 1)
					continue;
			    ovl = createItem(i);
			    ovl.getMarker(0).draw(canvas);
			    a = text[i].split("\\|");
		    	if(a.length == 1) {
					textPaint.getTextBounds(a[0], 0, a[0].length(), r);
	                canvas.drawText(a[0], point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-3, textPaint);
				} else {
					textPaint.getTextBounds(a[0], 0, a[0].length(), r);
	                canvas.drawText(a[0], point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-3, textPaint);
	                canvas.drawText(a[1], point[i].x+r.width()+(39-r.height())+3, point[i].y-( (39-r.height()) / 2)-3, noPaint);
				}
		    	canvas.drawCircle(point[i].x, point[i].y, padding, pointPaint);
            }
			i = getLastFocusedIndex();
			if(i>=0 && size() > 1) {
			    a = text[i].split("\\|");
			    ovl = createItem(i);
			    ovl.getMarker(0).draw(canvas);
				if(a.length == 1) {
					textPaint.getTextBounds(a[0], 0, a[0].length(), r);
	                canvas.drawText(a[0], point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-24, textPaint);
	                canvas.drawText(ovl.getSnippet(), point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-3, snippetPaint);
				} else {
					textPaint.getTextBounds(a[0], 0, a[0].length(), r);
					canvas.drawText(a[0], point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-24, textPaint);
	                
					width = r.width();
					snippetPaint.getTextBounds(ovl.getSnippet(), 0, ovl.getSnippet().length(), r);
	                width = Math.max(width, r.width());
	                canvas.drawText(a[1], point[i].x+width+(39-r.height())+3, point[i].y-( (39-r.height()) / 2)-24, noPaint);
	                
	                canvas.drawText(ovl.getSnippet(), point[i].x+( (39-r.height()) / 2), point[i].y-( (39-r.height()) / 2)-3, snippetPaint);
				}
				canvas.drawCircle(point[i].x, point[i].y, padding, pointPaint);
			}
			prevFocus = getLastFocusedIndex();
		}

		@Override
		protected boolean hitTest(OverlayItem item, Drawable marker, int hitX, int hitY) {
			Rect b = marker.getBounds();
			return (hitX-TOUCH_SPAN<=b.width() && hitX+TOUCH_SPAN>=0 && hitY+TOUCH_SPAN>=0 && hitY-TOUCH_SPAN<=b.height());
		}

		@Override
		protected boolean onTap(int index) {
			if(prevFocus == index)
				showMenu(index);
			return true;
		}
	}

	public void showMenu(int index) {
		OverlayItem ovl = overlay.getItem(index);
		final String[] a = ovl.getTitle().split("\\|");
		if(a.length!=3)
			return;
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);
		adapter.add(String.format(getResources().getString(R.string.line), a[2]));
		adapter.add(String.format(getResources().getString(R.string.group),a[0]));
		new AlertDialog.Builder(this)
		.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent i;
				switch(which) {
				case 0:
					i = new Intent(AutobuserMap.this, Search.class);
					i.putExtra("q", a[2]);
					getParent().startActivity(i);
					break;
				case 1:
					i = new Intent(AutobuserMap.this, Search.class);
					i.putExtra("q", a[0]);
					getParent().startActivity(i);
					break;
				}
			}
		})
		.create().show();
	}
}
	