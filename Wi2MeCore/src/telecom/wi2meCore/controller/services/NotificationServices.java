/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meCore.controller.services;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

/**
 * Service managing the notifications. Plays a sound when requested.
 * @author Milcea
 *
 */
public class NotificationServices implements INotificationServices{
	private MediaPlayer mMediaPlayer;
	private AudioManager audioManager;
	private Context context;
	private Uri alert;
	
	public NotificationServices(Context context){
		this.context = context;
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);		
		alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	}

	@Override
	public void playNotificationSound() {	
		
	     if(alert == null){
	         return;
	     }
		 mMediaPlayer = new MediaPlayer();
		 try{
			 mMediaPlayer.setDataSource(context, alert);
		 } catch (Exception e) {
			 Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		 }
		 if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
			 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
			 try {
				mMediaPlayer.prepare();
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				return;
			}
			 mMediaPlayer.setLooping(false);
			 mMediaPlayer.start();
		  }

	}

}
