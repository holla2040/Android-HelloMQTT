package com.rethinksun;

/* references
 * 	http://www.hardill.me.uk/wordpress/?p=204
 *  http://dalelane.co.uk/blog/?p=1599 - for more complete example using a service for MQTT
 *  http://saeedsiam.blogspot.com/2009/02/first-look-into-android-thread.html - handler example
 */

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.TextView;

import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttSimpleCallback;

public class HelloMQTTActivity extends Activity {
	/** Called when the activity is first created. */
	private String android_id;
	private MqttClient client;
	private TextView topicView;
	private TextView messageView;

	final static String broker = "tcp://horton.rethinksun.com:1883";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		topicView = (TextView) findViewById(R.id.topic);
		messageView = (TextView) findViewById(R.id.message);
		android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);
		connect();
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			topicView.setText(msg.getData().getString("topic"));
			messageView.setText(msg.getData().getString("message"));
		}
	};

	private boolean connect() {
		try {
			topicView.setText("waiting for message");
			messageView.setText(android_id);

			client = (MqttClient) MqttClient.createMqttClient(broker, null);
			client.registerSimpleHandler(new MessageHandler());
			client.connect("HM" + android_id, true, (short) 240);
			String topics[] = { "testing/#" };
			int qos[] = { 1 };
			client.subscribe(topics, qos);
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unused")
	private class MessageHandler implements MqttSimpleCallback {
		public void publishArrived(String _topic, byte[] payload, int qos,
				boolean retained) throws Exception {
			String _message = new String(payload);
			Bundle b = new Bundle();
			b.putString("topic", _topic);
			b.putString("message", _message);
			Message msg = handler.obtainMessage();
			msg.setData(b);
			handler.sendMessage(msg);
			Log.d("MQTT", _message);

		}

		public void connectionLost() throws Exception {
			client = null;
			Log.v("HelloMQTT", "connection dropped");
			Thread t = new Thread(new Runnable() {

				public void run() {
					do {// pause for 5 seconds and try again;
						Log.v("HelloMQTT",
								"sleeping for 10 seconds before trying to reconnect");
						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} while (!connect());
					System.err.println("reconnected");
				}
			});
		}
	}
}
