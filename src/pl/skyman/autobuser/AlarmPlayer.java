package pl.skyman.autobuser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

public class AlarmPlayer {
	private Vibrator mVibrator;
	private boolean mPlaying;
	private MediaPlayer mMediaPlayer;
	private SharedPreferences settings;
	private static final float IN_CALL_VOLUME = 0.125f;
    private static final long[] sVibratePattern = new long[] { 500, 500 };


	AlarmPlayer(Context context) {
		mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }


	public void play(Context context) {
        if (mPlaying) stop(context);

        
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });
        boolean looping = true;
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                setDataSourceFromResource(context.getResources(),
                        mMediaPlayer, R.raw.res_raw_in_call_alarm);
                looping = false;
            } else {
            	//setDataSourceFromResource(context.getResources(),
                //        mMediaPlayer, R.raw.res_raw_in_call_alarm);
            	settings = context.getSharedPreferences(Autobuser.PREFS_NAME, 0);
            	String ringtone = settings.getString("reminderRingtone", null);
            	RingtoneManager ringtoneManager = new RingtoneManager(context);
        		ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
        		Uri ringtoneUri = ringtoneManager.getRingtoneUri(0);
        		if(ringtone != null)
        			ringtoneUri = Uri.parse(ringtone);
            	mMediaPlayer.setDataSource(context, ringtoneUri );
            }
            startAlarm(mMediaPlayer, looping);
        } catch (Exception ex) {
        	ex.printStackTrace();
            // The alert may be on the sd card which could be busy right now.
            // Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                //setDataSourceFromResource(context.getResources(), mMediaPlayer, android.R.raw.fallbackring);
                startAlarm(mMediaPlayer, looping);
            } catch (Exception ex2) {
            	ex2.printStackTrace();
                // At this point we just don't play anything.
            }
        }

        /* Start the vibrator after everything is ok with the media player */
        mVibrator.vibrate(sVibratePattern, 0);
        
        mPlaying = true;
    }
    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player, boolean looping)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(looping);
        player.prepare();
        player.start();
    }
    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop(Context context) {
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop vibrator
            mVibrator.cancel();

            /* disable alarm only if it is not set to repeat */
        }
    }

}
