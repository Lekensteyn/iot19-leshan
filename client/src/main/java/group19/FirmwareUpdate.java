package group19;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class FirmwareUpdate extends BaseInstanceEnabler {

	private UpdateState updateState = UpdateState.IDLE;
	private UpdateResult updateResult = UpdateResult.INITIAL;
	final private LightDevice device;
	final private UpdateType updateType;

	public FirmwareUpdate(UpdateType updateType, LightDevice device) {
		this.updateType = updateType;
		this.device = device;
	}

	@Override
	public ReadResponse read(int resourceid) {
		switch (resourceid) {
		case 3: /* Current update state. */
			return ReadResponse.success(resourceid, updateState.getValue());
		case 5: /* Update result. */
			return ReadResponse.success(resourceid, updateResult.getValue());
		default:
			return super.read(resourceid);
		}
	}

	@Override
	public WriteResponse write(int resourceid, LwM2mResource value) {
		switch (resourceid) {
		case 0: /* Firmware package */
			return WriteResponse.badRequest("Not implemented for " + updateType);
		case 1: /* Firmware package location */
			setPackageUri((String) value.getValue());
			return WriteResponse.success();
		default:
			return super.write(resourceid, value);
		}
	}

	@Override
	public ExecuteResponse execute(int resourceid, String params) {
		switch (resourceid) {
		case 2: /* Trigger Firmware update after download completes. */
			return ExecuteResponse.badRequest("Not implemented");
		default:
			return super.execute(resourceid, params);
		}
	}

	private void setPackageUri(String url) {
		setUpdateResult(UpdateResult.INITIAL);
		setUpdateState(UpdateState.DOWNLOADING);
		// TODO start downloading, on complete set state and result accordingly.
	}

	private void setUpdateState(UpdateState updateState) {
		if (this.updateState != updateState) {
			this.updateState = updateState;
			fireResourcesChange(3);
		}
	}

	private void setUpdateResult(UpdateResult updateResult) {
		if (this.updateResult != updateResult) {
			this.updateResult = updateResult;
			fireResourcesChange(5);
		}
	}

	public enum UpdateState {
		IDLE(0), DOWNLOADING(1), UPDATING(3);

		private final int value;

		UpdateState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum UpdateResult {
		INITIAL(0), SUCCESS(1), STORAGE_FULL(2), OUT_OF_MEMORY(3), CONNECTION_LOST(4), CHECKSUM_INVALID(
				5), UNSUPPORTED_PACKAGE(6), INVALID_URI(7), UPDATE_FAILED(8);

		private final int value;

		UpdateResult(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum UpdateType {
		LIGHT_BEHAVIOR, OWNERSHIP_PRIORITY
	}
}
