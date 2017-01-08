package group19;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class LightDevice extends BaseInstanceEnabler {
	private String lightId = "";
	private String deviceType="";
	private LightState lightState = LightState.FREE;
	private UserType userType = UserType.USER3; 
	private String userId;
	private String lightColor;
	private boolean lowLight;
	private int groupNo;
	private float locationX;
	private float locationY;
	private String roomId;
	private BehaviorDeployment behaviorDeployment = BehaviorDeployment.DISTRIBUTED;

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
			return ReadResponse.success(resourceid, lightColor);
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
			return WriteResponse.success();
		case 1:
			deviceType = (String) value.getValue();
			return WriteResponse.success();
		case 2:
			try {
				lightState = LightState.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 3:
			try {
				userType = UserType.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
			return WriteResponse.success();
		case 4:
			userId = (String) value.getValue();
			return WriteResponse.success();
		case 7:
			groupNo = (int) value.getValue();
			return WriteResponse.success();
		case 8:
			locationX = (float) value.getValue();
			return WriteResponse.success();
		case 9:
			locationY = (float) value.getValue();
			return WriteResponse.success();
		case 10:
			roomId = (String) value.getValue();
			return WriteResponse.success();
		case 11:
			try {
				behaviorDeployment = BehaviorDeployment.valueOf((String) value.getValue());
			} catch (IllegalArgumentException ex) {
				return WriteResponse.badRequest("Invalid argument");
			}
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
	//represents the state of the light device
	enum LightState {
		USED, FREE
	}
	//stating which lighting behavior deployment is used
	enum BehaviorDeployment {
		BROKER, DISTRIBUTED
	}
	enum UserType {
		USER1, USER2, USER3
	}
	
}
