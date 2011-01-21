package pl.skyman.autobuser;

import android.location.Location;
import android.view.KeyEvent;

public interface AutobuserActivity {
	public void locationChanges(Location newLocation);
	public void orientationChanges(float[] newOrientation);
	public void minuteChanges();
	public boolean onKeyDown(int keyCode, KeyEvent event);
}
