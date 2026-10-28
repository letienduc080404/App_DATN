package com.example.myapp.data.remote;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Quản lý kết nối MQTT đến Broker Mosquitto.
 */
public class MqttManager {
    private static final String TAG = "MqttManager";
    private static final String BROKER_URL = "tcp://letienduc247.io.vn:1883";
    private static final String CLIENT_ID = "AgriGuard_Android_Client";

    private MqttClient mqttClient;
    private MqttMessageListener messageListener;

    public interface MqttMessageListener {
        void onMessageReceived(String topic, String payload);
        void onConnectionLost(Throwable cause);
        void onConnected();
    }

    public MqttManager(MqttMessageListener listener) {
        this.messageListener = listener;
        initClient();
    }

    private void initClient() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Mất kết nối MQTT: " + cause.getMessage());
                    if (messageListener != null) messageListener.onConnectionLost(cause);
                    // Tự động kết nối lại sau 5s
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Nhận tin nhắn topic [" + topic + "]: " + payload);
                    if (messageListener != null) messageListener.onMessageReceived(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Lỗi khởi tạo MQTT Client: " + e.getMessage());
        }
    }

    public void connect() {
        new Thread(() -> {
            try {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(10);
                
                if (!mqttClient.isConnected()) {
                    mqttClient.connect(options);
                    Log.d(TAG, "Kết nối MQTT thành công");
                    if (messageListener != null) messageListener.onConnected();
                }
            } catch (MqttException e) {
                Log.e(TAG, "Lỗi kết nối MQTT: " + e.getMessage());
            }
        }).start();
    }

    public void subscribe(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, 1);
                Log.d(TAG, "Đã Subscribe topic: " + topic);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Lỗi Subscribe: " + e.getMessage());
        }
    }

    private void reconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Lỗi Disconnect: " + e.getMessage());
        }
    }
}