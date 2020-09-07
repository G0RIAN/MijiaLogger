package de.gorian.sensorreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Locale;
import java.util.Objects;

public class SensorUpdateBroadcastReceiver extends BroadcastReceiver implements MqttCallback {
	public static final String TAG = SensorUpdateBroadcastReceiver.class.getSimpleName();
	String clientId = "SensorReaderClient";
	MqttAndroidClient mqttAndroidClient;
	final String serverUri = "tcp://192.168.0.213:1883";

	//	final String publishTopic = "MijiaSensor/";
//	final String publishMessage = "Hello World!";
	@Override
	public void onReceive(Context context, Intent intent) {


		createClient(context);
		connect(context);
		try {
			parseAndSendData(context, intent);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


	}

	void connect(Context context) {
		if (!mqttAndroidClient.isConnected()) {
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setAutomaticReconnect(true);
			mqttConnectOptions.setCleanSession(false);
			try {
				addToHistory(context, "Connecting to " + serverUri);
				mqttAndroidClient.connect(mqttConnectOptions, context, new IMqttActionListener() {
					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
						disconnectedBufferOptions.setBufferEnabled(true);
						disconnectedBufferOptions.setBufferSize(100);
						disconnectedBufferOptions.setPersistBuffer(false);
						disconnectedBufferOptions.setDeleteOldestMessages(false);
						mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
						addToHistory(context, "Failed to connect to: " + serverUri);
					}
				});


			} catch (MqttException ex) {
				ex.printStackTrace();
			}
		}
	}

	void createClient(Context context) {
		if (mqttAndroidClient == null) {
			clientId = clientId + System.currentTimeMillis();
			mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
			mqttAndroidClient.setCallback(new MqttCallbackExtended() {
				@Override
				public void connectComplete(boolean reconnect, String serverURI) {

					if (reconnect) {
						addToHistory(context, "Reconnected to : " + serverURI);
					} else {
						addToHistory(context, "Connected to: " + serverURI);
					}
				}

				@Override
				public void connectionLost(Throwable cause) {
					addToHistory(context, "The Connection was lost.");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) {
					addToHistory(context, "Incoming message: " + new String(message.getPayload()));
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {

				}
			});
		}
	}

	private void parseAndSendData(Context context, Intent intent) throws InterruptedException {
		Thread.sleep(200);
		String extraData = (String) Objects.requireNonNull(intent.getExtras()).get("com.example.bluetooth.le.EXTRA_DATA");
		Log.d(TAG, "Broadcast received.");
		assert extraData != null;
		String[] split = extraData.split("\n");
		String[] hexBytes = split[1].split(" ");
		try {
			if (hexBytes.length < 12) return;

			int[] data = new int[hexBytes.length];
			for (int ii = 0; ii < hexBytes.length; ii++) {
				data[ii] = Integer.parseInt(hexBytes[ii], 16);
			}


			Integer battery = null;
			Double temp = null;
			Double humidity = null;

			switch (data[11]) {
				case 0x06: //humidity
					humidity = data[14] / 10.d + (data[15] * 16 * 16) / 10.d;
					break;
				case 0x0D: //temp + humidity
					humidity = data[16] / 10.d + data[17] * 16 * 16 / 10.d;
				case 0x04: //temp
					temp = data[14] / 10.d + data[15] * 16 * 16 / 10.d;
					break;
				case 0x0A: //battery
					battery = data[14];
					break;
			}
			Log.d("BroadcastReceiver", "Sensor data parsed (temp: " + temp + "°C, hum: " + humidity + "%, battery: " + battery + "%)");

			if (this.mqttAndroidClient == null) {
				Log.e(TAG, "mqttAndroidClient is null. Could not connect to mqtt server. ");
				return;
			}

			if (temp != null)
				publishMessage("SensorReader/temperature", String.format(Locale.GERMANY, "%2.1f", temp), context);
			if (humidity != null)
				publishMessage("SensorReader/humidity", String.format(Locale.GERMANY, "%2.1f", humidity), context);
			if (battery != null)
				publishMessage("SensorReader/battery", String.format(Locale.GERMANY, "%3d", battery), context);
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				mqttAndroidClient.disconnect();
			} catch (MqttException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void addToHistory(Context context, String mainText) {
		Log.i(TAG, "LOG: " + mainText);
		Toast toast = Toast.makeText(context, mainText, Toast.LENGTH_LONG);
		toast.show();
	}

	public void publishMessage(String publishTopic, String publishMessage, Context context) {

		try {
			MqttMessage message = new MqttMessage();
			message.setPayload(publishMessage.getBytes());
			mqttAndroidClient.publish(publishTopic, message);
			addToHistory(context, "Message Published");
			if (!mqttAndroidClient.isConnected()) {
				addToHistory(context, mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
			}
		} catch (MqttException e) {
			Log.e(TAG, "Error Publishing: " + e.getMessage());
			e.printStackTrace();
		}
	}


	@Override
	public void connectionLost(Throwable cause) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

	}

}
