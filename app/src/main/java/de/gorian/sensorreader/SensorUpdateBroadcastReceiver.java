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
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SensorUpdateBroadcastReceiver extends BroadcastReceiver {
	final String serverUri = "tcp://127.0.0.1:1883";
	String clientId = "SensorReaderClient";
	MqttAndroidClient mqttAndroidClient;

	//	final String publishTopic = "MijiaSensor/";
//	final String publishMessage = "Hello World!";
	@Override
	public void onReceive(Context context, Intent intent) {


		clientId = clientId + System.currentTimeMillis();

		MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
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
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				addToHistory(context, "Incoming message: " + new String(message.getPayload()));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {

			}
		});

		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		mqttConnectOptions.setCleanSession(false);

		try {
			addToHistory(context, "Connecting to " + serverUri);
			mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
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

		String extraData = (String) intent.getExtras().get("com.example.bluetooth.le.EXTRA_DATA");
		Log.d("BroadcastReceiver", "Broadcast received.");
		String[] split = extraData.split("\n");
		String[] hexBytes = split[1].split(" ");

		if (hexBytes.length < 14) return;

		byte[] data = new byte[hexBytes.length];
		for (int ii = 0; ii < hexBytes.length; ii++) {
			data[ii] = Byte.parseByte(hexBytes[ii]);
		}


		Integer battery = null;
		Double temp = null;
		Double humidity = null;

		double value = (data[16] + data[17] * 16 * 16) / 10.0;
		switch (data[13]) {
			case 0x06: //humidity
				humidity = value;
				break;
			case 0x0D: //temp + humidity
				humidity = (data[18] + data[19] * 16 * 16) / 10.0;
			case 0x04: //temp
				temp = value;
				break;
			case 0x0A: //battery
				battery = (int) data[16];
		}

		Log.d("BroadcastReceiver", "Sensor data parsed (" + temp + " hum: " + humidity + "battery:" + battery);

		if (temp != null) publishMessage("SensorReader/temperature", temp.toString(), context);
		if (humidity != null) publishMessage("SensorReader/humidity", humidity.toString(), context);
		if (battery != null) publishMessage("SensorReader/humidity", battery.toString(), context);
	}

	private void addToHistory(Context context, String mainText) {
		System.out.println("LOG: " + mainText);
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
			System.err.println("Error Publishing: " + e.getMessage());
			e.printStackTrace();
		}
	}


}
