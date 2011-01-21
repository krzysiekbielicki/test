package pl.skyman.autobuser;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.PowerManager;

public class AutobuserAlertWakeLock {
	private static PowerManager.WakeLock sWakeLock;
	private static KeyguardLock mKeyguardLock;
	final static void acquire(Context context) {
		if (sWakeLock != null)
			sWakeLock.release();
		if (mKeyguardLock != null)
			mKeyguardLock.reenableKeyguard();
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		sWakeLock = pm.newWakeLock(	PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "AutobuserWL");
		sWakeLock.acquire();
		KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		mKeyguardLock = mKeyguardManager.newKeyguardLock("AutobuserWL");
        mKeyguardLock.disableKeyguard();
	}
	final static void release() {
		if (sWakeLock != null) {
			sWakeLock.release();
			sWakeLock = null;
		}

		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
			mKeyguardLock = null;
		}
	}
}