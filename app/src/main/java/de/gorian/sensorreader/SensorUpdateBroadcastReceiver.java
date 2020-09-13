package de.gorian.sensorreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class SensorUpdateBroadcastReceiver extends BroadcastReceiver implements MqttCallback {

	public static final String TAG = SensorUpdateBroadcastReceiver.class.getSimpleName();
	String publishTopicHeader = "SensorReader";
	MqttAndroidClient mqttAndroidClient;
	boolean publishEnabled = true;
	private String clientId = "SensorReader";
	private Context context;
	private String serverUri = "tcp://127.0.0.1:1883";

	public SensorUpdateBroadcastReceiver() {
		super();
	}


	@Override
	public void onReceive(Context context, Intent intent) {

		if (!publishEnabled) {
			Log.d(TAG, "Publishing disabled. Skipping received data.");
			return;
		}
		createClient();
		connect();
		try {
			parseAndSendData(intent);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	void connect() {
		if (!mqttAndroidClient.isConnected()) {
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setAutomaticReconnect(true);
			mqttConnectOptions.setCleanSession(false);
			try {
				addToHistory("Connecting to " + serverUri);
				mqttAndroidClient.connect(mqttConnectOptions, this.context, new IMqttActionListener() {
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
						addToHistory("Failed to connect to: " + serverUri);
					}
				});


			} catch (MqttException ex) {
				ex.printStackTrace();
			}
		}
	}

	void createClient() {
		if (mqttAndroidClient == null) {
			mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
			mqttAndroidClient.setCallback(new MqttCallbackExtended() {
				@Override
				public void connectComplete(boolean reconnect, String serverURI) {

					if (reconnect) {
						addToHistory("Reconnected to : " + serverURI);
					} else {
						addToHistory("Connected to: " + serverURI);
					}
				}

				@Override
				public void connectionLost(Throwable cause) {
					addToHistory("The Connection was lost.");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) {
					addToHistory("Incoming message: " + new String(message.getPayload()));
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {

				}
			});
		}
	}

	private void parseAndSendData(Intent intent) throws InterruptedException {
		Thread.sleep(200);
		Log.d(TAG, "Broadcast received.");

		String mac = (String) intent.getExtras().get(BluetoothLeService.ADDRESS);
		Double temperature = (Double) intent.getExtras().get(BluetoothLeService.TEMPERATURE);
		Double humidity = (Double) intent.getExtras().get(BluetoothLeService.HUMIDITY);
		Integer battery = (Integer) intent.getExtras().get(BluetoothLeService.BATTERY);

		if (mac == null) {
			mac = "000000000000";
			Log.w(TAG, "Received sensor values from device with unknown MAC. Sending data on default topic (" + publishTopicHeader + "/000000000000/) ...");
		}

		Log.d("BroadcastReceiver", "Sensor data parsed (temp: " + temperature + "°C, hum: " + humidity + "%, battery: " + battery + "%) from device: (MAC: " + mac + ")");

		if (this.mqttAndroidClient == null) {
			Log.e(TAG, "mqttAndroidClient is null. Could not connect to mqtt server. ");
			return;
		}
		if (temperature != null)
			publishMessage(publishTopicHeader + "/" + mac + "/temperature", String.format(Locale.GERMANY, "%2.1f", temperature));
		if (humidity != null)
			publishMessage(publishTopicHeader + "/" + mac + "/humidity", String.format(Locale.GERMANY, "%2.1f", humidity));
		if (battery != null)
			publishMessage(publishTopicHeader + "/" + mac + "/battery", String.format(Locale.GERMANY, "%3d", battery));
	}

	private void addToHistory(String mainText) {
		Log.i(TAG, "LOG: " + mainText);
		Toast toast = Toast.makeText(context, mainText, Toast.LENGTH_LONG);
		toast.show();
	}

	public void publishMessage(String publishTopic, String publishMessage) {

		try {
			MqttMessage message = new MqttMessage();
			message.setPayload(publishMessage.getBytes());
			mqttAndroidClient.publish(publishTopic, message);
			addToHistory("Message Published");
			if (!mqttAndroidClient.isConnected()) {
				addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
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

	public void setContext(Context context) {
		this.context = context;
	}

	public void updateClient(SharedPreferences sharedPreferences, String key) {

		Log.d(TAG, "preferences changed");

		String oldServerUri = this.serverUri;
		String oldClientId = this.clientId;
		this.publishEnabled = sharedPreferences.getBoolean("publish_data", true);
		this.serverUri = sharedPreferences.getString("server_uri", serverUri);
		this.clientId = sharedPreferences.getString("client_id", clientId);
		this.publishTopicHeader = sharedPreferences.getString("publish_topic_header", publishTopicHeader);
		if ((!oldServerUri.equals(this.serverUri)) || (!oldClientId.equals(this.clientId)) && mqttAndroidClient != null) {
			try {
				mqttAndroidClient.disconnect();
				mqttAndroidClient.close();
				mqttAndroidClient = null;
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}
}