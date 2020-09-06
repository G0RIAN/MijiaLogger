package de.gorian.sensorreader;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
	private final static String READ_SENSOR_WORKER_TAG = "ReadSensor";
	static final long BLE_SEARCH_INTERVAL_MS = 120 * 1000;
	private final static String TAG = BluetoothLeService.class.getSimpleName();
	private static final long SCAN_INTERVAL = 1;
	private static final String SENSOR_UPDATE_ACTION = "de.gorian.sensorReader.MIJIA_DATA_RECEIVED";
	private BluetoothLeScanner bluetoothLeScanner;
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	private List<BluetoothGatt> bluetoothGatt = new ArrayList<>();
	private List<Integer> connectionState = new ArrayList<>();
	private SensorUpdateBroadcastReceiver sensorUpdateBroadcastReceiver;
	// Various callback methods defined by the BLE API.
	private ScanCallback leScanCallback =
			new ScanCallback() {
				@Override
				public void onScanResult(int callbackType, ScanResult result) {

					final BluetoothDevice device = result.getDevice();

//					Log.i(TAG, "New BLE device found: " + device.getName() + " " + device.getAddress());
					if (device.getName() != null && device.getName().contains("MJ_HT_V1")) {

						try {
							Collection<byte[]> scanData = Objects.requireNonNull(result.getScanRecord()).getServiceData().values();
							for (byte[] aByteSet : scanData) {
								BluetoothLeService.this.broadcastUpdate(SENSOR_UPDATE_ACTION, aByteSet);
							}

							connectionState.add(BluetoothProfile.STATE_CONNECTED);
						} catch (NullPointerException e) {
							// do nothing
						}


						// TODO: update UI
					}
				}
			};

	public String byteToHex(byte num) {
		char[] hexDigits = new char[2];
		hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
		hexDigits[1] = Character.forDigit((num & 0xF), 16);
		return new String(hexDigits);
	}

	public BluetoothLeService() {
		super();
		Log.d(TAG, "Constructor executed.");
	}

	public static BluetoothLeService getINSTANCE() {
		return LazyHolder.INSTANCE;
	}

	@Override
	public List<BluetoothDevice> getConnectedDevices() {
		return null;
	}

	@Override
	public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
//		List<BluetoothDevice> someDevices = new ArrayList<>();
//		for (int ii = 0; ii < devices.size(); ii++) {
//			if (Arrays.stream(states).boxed().collect(Collectors.toList()).contains(connectionState.get(ii))) {
//				someDevices.add(devices.get(ii));
//			}
//		}
		return null;
	}

	@Override
	public int getConnectionState(BluetoothDevice device) {
//		int state = -1;
//		try {
//			state = connectionState.get(devices.indexOf(device));
//		} catch (IndexOutOfBoundsException e) {
//			Log.e(TAG, "device (" + device.getName() + " " + device.getAddress() + ") unknown. Known devices :");
//			devices.forEach(dev -> Log.d(TAG, "\t\t" + dev.getName() + " " + dev.getAddress()));
//		}
		return BluetoothProfile.STATE_CONNECTED;
	}


	@Override
	public void onCreate() {
		super.onCreate();
		LazyHolder.INSTANCE = this;
		initBTLE();
		this.sensorUpdateBroadcastReceiver = new SensorUpdateBroadcastReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(this.sensorUpdateBroadcastReceiver, new IntentFilter(SENSOR_UPDATE_ACTION));
	}

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

		final Intent intent = new Intent(action);

		// write the data formatted in HEX.
		final byte[] data = characteristic.getValue();
		broadcastUpdate(action, data);
	}

	private void broadcastUpdate(final String action, final byte[] data) {

		final Intent intent = new Intent(action);

		// write the data formatted in HEX.
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));
			intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
					stringBuilder.toString());
			Log.d(TAG, "Sensor update broadcasted: " + stringBuilder.toString());
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
		if (bluetoothGatt.size() > 0) {
			Log.i(TAG, "SERVICES: ");
			bluetoothGatt.get(0).getServices().forEach(bluetoothGattService -> Log.i(TAG, bluetoothGattService.getUuid().toString()));
		}

//		WorkManager workMan = WorkManager.getInstance(this);
//		for (BluetoothDevice device : devices) {
//			Data data = new Data.Builder().putString("MAC", device.getAddress()).build();
//			PeriodicWorkRequest worker = new PeriodicWorkRequest.Builder(ReadSensorWorker.class, SCAN_INTERVAL, TimeUnit.SECONDS).setInputData(data).addTag(READ_SENSOR_WORKER_TAG).build();
//			workMan.enqueue(worker);
//		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorUpdateBroadcastReceiver);
		Log.d(TAG, "Service died.");
	}

	public void startScan() {
		bluetoothGatt.clear();
		bluetoothLeScanner.startScan(leScanCallback);
		WorkManager workMan = WorkManager.getInstance(this);
		workMan.cancelAllWorkByTag(READ_SENSOR_WORKER_TAG);
	}

	public static class LazyHolder {
		public static BluetoothLeService INSTANCE;
	}
}