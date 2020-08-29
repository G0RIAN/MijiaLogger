package de.gorian.sensorreader;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BluetoothLeService extends Service implements BluetoothProfile {
	public final static String ACTION_GATT_CONNECTED =
			"com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED =
			"com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED =
			"com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE =
			"com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA =
			"com.example.bluetooth.le.EXTRA_DATA";
	static final long BLE_SEARCH_INTERVAL_MS = 120 * 1000;
	private final static String TAG = BluetoothLeService.class.getSimpleName();
	private BluetoothLeScanner bluetoothLeScanner;
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	private List<BluetoothGatt> bluetoothGatt = new ArrayList<>();
	private List<Integer> connectionState = new ArrayList<>();
	// Various callback methods defined by the BLE API.
	private final BluetoothGattCallback gattCallback =
			new BluetoothGattCallback() {
				@Override
				public void onConnectionStateChange(BluetoothGatt gatt, int status,
				                                    int newState) {
					String intentAction;
					int ind = bluetoothGatt.indexOf(gatt);
					try {
						if (newState == BluetoothProfile.STATE_CONNECTED) {
							intentAction = ACTION_GATT_CONNECTED;
							connectionState.set(ind, STATE_CONNECTED);
							broadcastUpdate(intentAction);
							Log.i(TAG, "Connected to GATT server.");
							Log.i(TAG, "Attempting to start service discovery:" +
									gatt.discoverServices());

						} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
							intentAction = ACTION_GATT_DISCONNECTED;
							connectionState.set(ind, STATE_DISCONNECTED);
							Log.i(TAG, "Disconnected from GATT server.");
							broadcastUpdate(intentAction);
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						if (connectionState.size() == 1) {
							connectionState.set(0, newState);
						}
					}
				}

				@Override
				// New services discovered
				public void onServicesDiscovered(BluetoothGatt gatt, int status) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
					} else {
						Log.w(TAG, "onServicesDiscovered received: " + status);
					}
				}

				@Override
				// Result of a characteristic read operation
				public void onCharacteristicRead(BluetoothGatt gatt,
				                                 BluetoothGattCharacteristic characteristic,
				                                 int status) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
					}
				}
			};
	private List<BluetoothDevice> devices = new ArrayList<>();
	private ScanCallback leScanCallback =
			new ScanCallback() {
				@Override
				public void onScanResult(int callbackType, ScanResult result) {
					final BluetoothDevice device = result.getDevice();
					if (devices.contains(device)) {
						return;
					}
					Log.i(TAG, "New BLE device found: " + device.getName() + " " + device.getAddress());
					if (device.getName() != null && device.getName().contains("MJ_HT_V1") && !devices.contains(device)) {

						devices.add(device);
						BluetoothGatt gatt = device.connectGatt(BluetoothLeService.this, true, gattCallback);
						Log.i(TAG, "Connected to GATT server.");
						Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
						bluetoothGatt.add(gatt);
						connectionState.add(BluetoothProfile.STATE_CONNECTED);

						// TODO: remove below in release
						stopScan();
					}
				}
			};

	public BluetoothLeService() {
		super();
		Log.d(TAG, "Constructor executed.");
	}

	public static BluetoothLeService getINSTANCE() {
		return LazyHolder.INSTANCE;
	}

	@Override
	public List<BluetoothDevice> getConnectedDevices() {
		return devices;
	}

	@Override
	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
		List<BluetoothDevice> someDevices = new ArrayList<>();
		for (int ii = 0; ii < devices.size(); ii++) {
			if (Arrays.stream(states).boxed().collect(Collectors.toList()).contains(connectionState.get(ii))) {
				someDevices.add(devices.get(ii));
			}
		}
		return someDevices;
	}

	@Override
	public int getConnectionState(BluetoothDevice device) {
		int state = -1;
		try {
			state = connectionState.get(devices.indexOf(device));
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, "device (" + device.getName() + " " + device.getAddress() + ") unknown. Known devices :");
			devices.forEach(dev -> Log.d(TAG, "\t\t" + dev.getName() + " " + dev.getAddress()));
		}
		return state;
	}

//	public final static UUID UUID_HEART_RATE_MEASUREMENT =
//			UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

	@Override
	public void onCreate() {
		super.onCreate();
		LazyHolder.INSTANCE = this;
		initBTLE();
	}

	public void clearDevices() {
		devices.clear();
	}

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

		final Intent intent = new Intent(action);

		// write the data formatted in HEX.
		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));
			intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
					stringBuilder.toString());

			sendBroadcast(intent);
		}
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	boolean isInitialized() {
		return bluetoothLeScanner != null;
	}

	private boolean initBTLE() {

		Log.d(TAG, "Check permissions: " + hasRequiredPermissions());
		if (!hasRequiredPermissions()) return false;

		if (bluetoothManager == null) {
			bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
			bluetoothAdapter = bluetoothManager.getAdapter();
			bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
		}

		if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
			Log.d(TAG, "Sending request to enable bluetooth adapter. This could fail.");
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			int REQUEST_ENABLE_BT = 42;
			startActivity(enableIntent);
			return false;
		}

		return true;

	}

	boolean hasRequiredPermissions() {
		boolean hasBluetoothPermission = hasPermission(Manifest.permission.BLUETOOTH);
		boolean hasBluetoothAdminPermission = hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
		boolean hasLocationPermission = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
		return hasBluetoothPermission && hasBluetoothAdminPermission && hasLocationPermission;
	}

	private boolean hasPermission(String permission) {
		return ActivityCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
	}

	public void stopScan() {
		bluetoothLeScanner.stopScan(leScanCallback);
	}

	public int getNumberOfDevices() {
		return devices.size();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Service died.");
	}

	public void startScan() {
		bluetoothGatt.clear();
		bluetoothLeScanner.startScan(leScanCallback);
	}

	public static class LazyHolder {
		public static BluetoothLeService INSTANCE;
	}
}