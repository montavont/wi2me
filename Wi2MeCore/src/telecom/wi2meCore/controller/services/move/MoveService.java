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

package telecom.wi2meCore.controller.services.move;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import telecom.wi2meCore.controller.services.ITimeService;
import android.os.PowerManager;

/**
 * Service to detect movements and determine whether the phone is static or moving.
 * @author XXX
 *
 */
public class MoveService implements IMoveService {

	private static final float TIME_CONSTANT = 100;
	private static final double ACCELERATION_THRESHOLD = 0.000001; 
	private static final long TIME_BETWEEN_DETECTIONS = 500; //In milliseconds. Time between one detection of acceleration over the threshold, and the next, to consider movement.

	private SensorManager sensorManager;
	private AccelerometerListener accListener;
	private PowerManager.WakeLock wl;
	private ITimeService timeService;
	private MoveLock moveLock;

	//this is the time when we detected acceleration higher than the threshold, but not necessarily a movement
	private long lastDetectedMovementTimestamp;

	private long lastMeasureReceivedTimestamp;
	private double x;
	private double y;
	private double z;
	private float[] gravity = new float[3];

	private long lastMovementTimestamp;

	public MoveService(Context context, ITimeService timeService){
		this.timeService = timeService;
		lastMovementTimestamp = timeService.getCurrentTimeInMilliseconds();
		lastMeasureReceivedTimestamp=timeService.getCurrentTimeInMilliseconds();
		lastDetectedMovementTimestamp=timeService.getCurrentTimeInMilliseconds();

		accListener = new AccelerometerListener();
		sensorManager=(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(accListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    	//acquire wakeLock for the accelerometer to broadcast results continuously
    	PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    	wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName() + "-WakeLock");
    	wl.acquire();
	}

	private void initializeLock() {
		moveLock = new MoveLock();
		moveLock.lock.lock();
	}

	/* (non-Javadoc)
	 * @see android.servicetest.IMoveService#finalizeService()
	 */
	@Override
	public void finalizeService(){
		wl.release();
		sensorManager.unregisterListener(accListener);
	}

	/* (non-Javadoc)
	 * @see android.servicetest.IMoveService#isMoving()
	 */
	@Override
	public boolean isMoving(){
		synchronized (this) {
			return isMovingNotSynchronized();
		}
	}

	private boolean isMovingNotSynchronized() {
		boolean ret = false;
		double acc= modAdditionAcc();
		if (acc > ACCELERATION_THRESHOLD){
			if (lastMeasureReceivedTimestamp - lastDetectedMovementTimestamp > TIME_BETWEEN_DETECTIONS){
				ret = true;
			}
			lastDetectedMovementTimestamp = lastMeasureReceivedTimestamp;
		}
		return ret;
	}

	private double modAdditionAcc() {
		return Math.abs(x)+Math.abs(y)+Math.abs(z);

	}

	/* (non-Javadoc)
	 * @see android.servicetest.IMoveService#getMovingLock()
	 */
	@Override
	public MoveLock getMovingLock(){
		synchronized (this) {
			return moveLock;
		}
	}

	private void releaseLock() {
		//keep a reference
		MoveLock myMoveLock = moveLock;
		//initialize a new lock for next movement
		initializeLock();
		if (myMoveLock != null)
			//unlock current threads
			myMoveLock.lock.unlock();
	}

	public long getLastMovementTimestamp(){
		synchronized (this) {
			return lastMovementTimestamp;
		}
	}

	public class MoveLock implements IMoveLock{
		private Lock lock;
		private MoveLock(){
			lock = new ReentrantLock();
		}

		public void waitForMovement() throws InterruptedException{
			lock.lockInterruptibly();
			lock.unlock();
		}

	}

	private class AccelerometerListener implements SensorEventListener{

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			//DO NOT IMPLEMENT
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
				//Log.d("SENSOR CHANGED", "Received");
				if (moveLock == null)
					releaseLock();

				long now = event.timestamp;
				long delta= now-lastMeasureReceivedTimestamp;
				lastMeasureReceivedTimestamp = now;

				float alpha = (float)((float)TIME_CONSTANT/(float)(TIME_CONSTANT+delta));

			    gravity[0] = alpha * gravity[0] + (1.0f - alpha) * event.values[0];
			    gravity[1] = alpha * gravity[1] + (1.0f - alpha) * event.values[1];
			    gravity[2] = alpha * gravity[2] + (1.0f - alpha) * event.values[2];

			    synchronized (this) {
					 x = event.values[0] - gravity[0];
					 y = event.values[1] - gravity[1];
					 z = event.values[2] - gravity[2];

					 if (isMovingNotSynchronized()){
						 releaseLock();
						 lastMovementTimestamp = timeService.getCurrentTimeInMilliseconds();
					 }
				}

			}

		}

	}

	@Override
	public void resetLastMovementTimestamp() {
		lastMovementTimestamp = timeService.getCurrentTimeInMilliseconds();
	}

}
