package group19;

public interface SensorChangeListener {

	/**
	 * Reports the current sensor state.
	 * 
	 * @param isUsed
	 *            true if the sensor detects a face, false otherwise.
	 */
	public void sensorChanged(boolean isUsed);

}
