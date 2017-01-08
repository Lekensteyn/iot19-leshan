package group19;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class SensorDevice extends BaseInstanceEnabler {
	private String sensorId;
	private String deviceType = "Sensor Device";
	private SensorState sensorState = SensorState.FREE;
	private String userId = "";
	private int groupNo;
	private float locationX;
	private float locationY;
	private String roomId = "";

	public SensorDevice(String sensorId) {
		this.sensorId = sensorId;
	}

	@Override
	public ReadResponse read(int resourceid) {
		switch (resourceid) {
		case 0:
			return ReadResponse.success(resourceid, sensorId);
		case 1:
			return ReadResponse.success(resourceid, deviceType);
		case 2:
			return ReadResponse.success(resourceid, sensorState.name());
		case 3:
			return ReadResponse.success(resourceid, userId);
		case 4:
			return ReadResponse.success(resourceid, groupNo);
		case 5:
			return ReadResponse.success(resourceid, locationX);
		case 6:
			return ReadResponse.success(resourceid, locationY);
		case 7:
			return ReadResponse.success(resourceid, roomId);

		default:
			return super.read(resourceid);
		}
	}

	@Override
	public WriteResponse write(int resourceid, LwM2mResource value) {
		switch (resourceid) {
		case 0:
			sensorId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 1:
			deviceType = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 2:
			try {
				sensorState = SensorState.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 3:
			userId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 4:
			groupNo = (int) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 5:
			locationX = (float) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 6:
			locationY = (float) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 7:
			roomId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		default:
			return super.write(resourceid, value);
		}
	}

	@Override
	public ExecuteResponse execute(int resourceid, String params) {
		switch (resourceid) {
		default:
			return super.execute(resourceid, params);
		}
	}

	// represents the state of the sensor device
	enum SensorState {
		USED, FREE
	}

}