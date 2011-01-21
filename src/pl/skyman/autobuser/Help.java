package pl.skyman.autobuser;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Help extends Activity implements AutobuserActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((Autobuser)getParent()).setTitle(R.string.help);
		setContentView(R.layout.help);
		((Button)findViewById(R.id.email)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"autobuser@skyman.pl"});
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Help.this.getResources().getString(R.string.sendEmailToDeveloperSubject));
				//emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, myBodyText);
				Help.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
			}
		});
		((Button)findViewById(R.id.reportBug)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"bug.autobuser@skyman.pl"});
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Help.this.getResources().getString(R.string.sendBugReportSubject));
				String bugText = Help.this.getResources().getString(R.string.sendBugReportText);
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, bugText);
				Help.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
			}
		});
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

}
