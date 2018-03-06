package helloworld.example.com.helloworld.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import helloworld.example.com.helloworld.R;

import static android.content.ContentValues.TAG;

public class BlueToothConfigService extends WifiConfigService {

    BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private ServerThread serverThread;

    public BlueToothConfigService() {
    }

    @Override
    public Bitmap getQrCodeBitMap() {
        String deviceId = this.getDeviceId();
        if(deviceId != null) {
            String qrcodeInfo = getString(R.string.qrcode_pre)
                    + "?deviceid=" + deviceId
                    + "&type=bluetooth"
                    + "&bluetoothname=" + mAdapter.getName()
                    + "&bluetoothaddress=" + mAdapter.getAddress();
            return WifiConfigService.generateBitmap(qrcodeInfo, 400, 400);
        }
        else {
            return null;
        }
    }

    @Override
    public void onCreate() {
        System.out.println("*****************\nBlueToothConfigService.onCreate()\n*****************");

        // 蓝牙相关设置
        if (!mAdapter.isEnabled()) {
            System.out.println("蓝牙未启动，直接启动蓝牙！");
            mAdapter.enable();
        }

//        String name = mAdapter.getName();
//        String address = mAdapter.getAddress();
//        System.out.println("bluetooth name = " + name + ", address = " + address);

        super.onCreate();
    }

    @Override
    protected void didConfimedDeviceId() {
        super.didConfimedDeviceId();
        startBluetoothListening();
    }

    private void startBluetoothListening() {
        serverThread = new ServerThread(getDeviceId());
        serverThread.start();
    }

    @Override
    public void onDestroy() {
//        if (serverThread != null) {
//            serverThread.cancel();
//        }
        super.onDestroy();
    }

    /**
     * 另外一个蓝牙实现方式
     */
    private class ServerThread extends Thread {
        private BluetoothServerSocket serverSocket;

        private final String TAG = "BlueToothTag";
        private final UUID deviceId;

        private ServerThread(String deviceId) {
            this.deviceId = UUID.fromString(deviceId);
        }

        @Override
        public void run() {
            try {
                serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(TAG, deviceId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, "等待客户连接……");

            while(serverSocket != null) {
                try {
                    BluetoothSocket socket = serverSocket.accept();
                    BluetoothDevice device = socket.getRemoteDevice();
                    Log.d(TAG, "接受客户连接 , 远端设备名字:" + device.getName() + " , 远端设备地址:" + device.getAddress());

                    if (socket.isConnected()) {
                        Log.d(TAG, "已建立与客户连接.");

                        // 写数据
//                        sendDataToClient(socket);

                        // 读数据
                        new ReadDataThread(socket).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendDataToClient(BluetoothSocket socket) {
            String s = "hello,world ! Server is talking.";
            byte[] buffer = s.getBytes();

            try {
                OutputStream os = socket.getOutputStream();

                os.write(buffer);
                os.flush();

                // os.close();
                // socket.close();

                Log.d(TAG, "服务器端数据发送完毕!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        new ReadDataThread(socket).start();
    }

    private class ReadDataThread extends Thread {
        private final BluetoothSocket socket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        private ReadDataThread(BluetoothSocket socket) {
            this.socket = socket;
            try {
                mmInStream = socket.getInputStream();
                mmOutStream =socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // JSON 解析
            JsonReader reader = new JsonReader(new InputStreamReader(mmInStream));
            Map<String, String>result = new HashMap<String, String>();
            try {
                reader.beginObject();
                while(reader.hasNext()) {
                    result.put(reader.nextName(), reader.nextString());
                }
                reader.endObject();

                String networkId = result.get("network_id");
                String password = result.get("password");
                int crypt = Integer.parseInt(result.get("crpt_type"));
                System.out.println("网络ID：" + networkId);
                System.out.println("密码：" + password);
                System.out.println("加密方式：" + crypt);

                // 使用新配置连接网络
                configWifi(networkId, password, crypt);

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "JSON解析失败：" + reader.toString());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
