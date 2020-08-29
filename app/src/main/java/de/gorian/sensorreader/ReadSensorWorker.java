package de.gorian.sensorreader;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReadSensorWorker extends Worker {

	private final static String TAG = BluetoothLeService.class.getSimpleName();
	BluetoothLeService leService;
	private BluetoothGatt bluetoothGatt;

	ReadSensorWorker(
			@NonNull Context context,
			@NonNull WorkerParameters params) {
		super(context, params);
	}

	@NonNull
	@Override
	public Result doWork() {
		// TODO: read sensor
		return Result.success();
	}

}
