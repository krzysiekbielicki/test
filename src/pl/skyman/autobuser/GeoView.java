package pl.skyman.autobuser;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

public class GeoView extends View {

	private float[] mValues;
	private Paint   mPaint = new Paint();
	private Paint   tPaint = new Paint();
    private Path    mPath = new Path();

	private Location location;
	private Location mLocation;

    @Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
	}

	public GeoView(Context context, AttributeSet attrs)
    {
    	super(context, attrs); 
        mPath.moveTo(0, -26);
        mPath.lineTo(-2, -15);
        mPath.lineTo(2, -15);
        mPath.close();

        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);
        
        tPaint.setTextAlign(Paint.Align.CENTER);
        tPaint.setAntiAlias(true);
        tPaint.setColor(Color.WHITE);
    }
	
	public void updateGeoData(float[] data, Location location) {
		this.mValues = data;
		if(location.hasAccuracy())
			this.mLocation = location;
		invalidate();
	}
    
    @Override protected void onDraw(Canvas canvas) {
    	canvas.translate(30, 30);
        if (mValues != null)
        	if(mLocation != null && location != null) {
        		float angle =-mValues[0]+mLocation.bearingTo(location);  
    			canvas.rotate(angle);
                canvas.drawPath(mPath, mPaint);
                canvas.rotate(-angle);
        	}
        canvas.drawText(getDistance(), 0, 0, tPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public String getDistance()
	{
		double distance;
		if(mLocation == null || location == null)
			return "?";
		else
			distance = mLocation.distanceTo(location);
		String unit;
		String format;
		if(distance > 1500000) {
			distance /= 1000000;
			format= "0.00";
			unit = "tys. km";
		} else if(distance > 1500) {
			distance /= 1000;
			format= "0.00";
			unit = "km";
		} else {
			unit = "m";
			format = "0";
		}
		
		return (new DecimalFormat(format)).format(distance)+" "+unit;
	}
}
