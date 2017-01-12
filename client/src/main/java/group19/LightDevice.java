package group19;

import java.io.IOException;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightDevice extends BaseInstanceEnabler {

	private final static Logger LOG = LoggerFactory.getLogger(LightDevice.class);

	private String lightId = "";
	private String deviceType = "Light Device";
	private LightState lightState = LightState.FREE;
	private UserType userType = UserType.USER3;
	private String userId = "";
	private RGBColor lightColor = new RGBColor(255, 255, 255);
	private boolean lowLight;
	private long groupNo;
	private double locationX;
	private double locationY;
	private String roomId = "";
	private BehaviorDeployment behaviorDeployment = BehaviorDeployment.Distributed;

	private LightProvider realDevice;

	public LightDevice(String lightId) {
		this.lightId = lightId;

		final RPiLightDevice realDevice = new RPiLightDevice();
		try {
			realDevice.start();
			this.realDevice = realDevice;
			// yep, nasty, there is probably a better place to add this.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					realDevice.destroy();
				}
			});
		} catch (IOException e) {
			LOG.error("Failed to start light device controller, state will not be updated!", e);
		}
	}

	@Override
	public ReadResponse read(int resourceid) {
		switch (resourceid) {
		case 0:
			return ReadResponse.success(resourceid, lightId);
		case 1:
			return ReadResponse.success(resourceid, deviceType);
		case 2:
			return ReadResponse.success(resourceid, lightState.name());
		case 3:
			return ReadResponse.success(resourceid, userType.name());
		case 4:
			return ReadResponse.success(resourceid, userId);
		case 5:
			return ReadResponse.success(resourceid, lightColor.toString());
		case 6:
			return ReadResponse.success(resourceid, lowLight);
		case 7:
			return ReadResponse.success(resourceid, groupNo);
		case 8:
			return ReadResponse.success(resourceid, locationX);
		case 9:
			return ReadResponse.success(resourceid, locationY);
		case 10:
			return ReadResponse.success(resourceid, roomId);
		case 11:
			return ReadResponse.success(resourceid, behaviorDeployment.name());

		default:
			return super.read(resourceid);
		}
	}

	@Override
	public WriteResponse write(int resourceid, LwM2mResource value) {
		switch (resourceid) {
		case 0:
			lightId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 1:
			deviceType = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 2:
			try {
				lightState = LightState.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 3:
			try {
				userType = UserType.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 4:
			userId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 5: /* Light Color */
			try {
				lightColor = new RGBColor((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			if (realDevice != null) {
				realDevice.setColor(lightColor.r, lightColor.g, lightColor.b);
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 6: /* Low Light mode */
			lowLight = (boolean) value.getValue();
			if (realDevice != null) {
				realDevice.setLowLightMode(lowLight);
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 7:
			groupNo = (long) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 8:
			locationX = (double) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 9:
			locationY = (double) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 10:
			roomId = (String) value.getValue();
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 11:
			try {
				behaviorDeployment = BehaviorDeployment.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			fireResourcesChange(resourceid);
			return WriteResponse.success();
		case 12:
			setOwnershipPriorityURL((String) value.getValue());
			return WriteResponse.success();
		case 13:
			setLightBehaviorURL((String) value.getValue());
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

	private void setOwnershipPriorityURL(String url) {
		LOG.info("Setting Ownership Priority URL to: " + url);
	}

	private void setLightBehaviorURL(String url) {
		LOG.info("Setting Light Behavior URL to: " + url);
	}

	// represents the state of the light device
	enum LightState {
		USED, FREE
	}

	// stating which lighting behavior deployment is used
	enum BehaviorDeployment {
		Broker, Distributed
	}

	enum UserType {
		USER1, USER2, USER3
	}

}
