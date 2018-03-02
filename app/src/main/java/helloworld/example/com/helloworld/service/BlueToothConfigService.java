package helloworld.example.com.helloworld.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import helloworld.example.com.helloworld.R;

import static android.content.ContentValues.TAG;

public class BlueToothConfigService extends WifiConfigService {

    private final UUID DEFALUT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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

        String name = mAdapter.getName();
        String address = mAdapter.getAddress();
        System.out.println("bluetooth name = " + name + ", address = " + address);

        super.onCreate();
    }

    @Override
    protected void didConfimedDeviceId() {
        super.didConfimedDeviceId();
        startBluetoothListening();
    }

    AcceptBluetoothConnection mAcceptThread;
    private void startBluetoothListening() {
//        mAcceptThread = new AcceptBluetoothConnection();
//        mAcceptThread.start();

        serverThread = new ServerThread();
        serverThread.start();
    }

    @Override
    public void onDestroy() {
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }
        super.onDestroy();
    }

    /**
     * 另外一个蓝牙实现方式
     */
    private class ServerThread extends Thread {
        private BluetoothServerSocket serverSocket;

        private final String TAG = "BlueToothTag";

        @Override
        public void run() {
            try {
                serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(TAG, DEFALUT_UUID);
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

    /**
     * 用于接受蓝牙连接的线程
      */
    private class AcceptBluetoothConnection extends Thread {
        private final BluetoothServerSocket mServerSocket;


        public AcceptBluetoothConnection() {
            BluetoothServerSocket tmp = null;
            String uuidStr = BlueToothConfigService.this.getDeviceId();
            if (uuidStr != null) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(mAdapter.getName(), uuid);
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(mAdapter.getName(), uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mServerSocket = tmp;
        }

        private int mState = BluetoothAdapter.STATE_DISCONNECTED;

        @Override
        public void run() {
            BluetoothSocket socket = null;
            // 循环，直到连接成功
            int mState = BluetoothAdapter.STATE_CONNECTING;

            while (mState != BluetoothAdapter.STATE_CONNECTED) {
                try {
                    // 这是一个阻塞调用 返回成功的连接
                    // mServerSocket.close()在另一个线程中调用，可以中止该阻塞
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                // 如果连接被接受
                if (socket != null) {
                    synchronized (BlueToothConfigService.this) {
                        switch (mState) {
                            case BluetoothAdapter.STATE_CONNECTING:
                                mState = BluetoothAdapter.STATE_CONNECTED;
                                // 正常情况。启动ConnectedThread。
                                System.out.println("获得链接：" + socket.getRemoteDevice().getName());
                                connected(socket, socket.getRemoteDevice());
                                break;

                            case BluetoothAdapter.STATE_CONNECTED:
                                // 没有准备或已连接。新连接终止。
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                if (mServerSocket != null) {
                    mServerSocket.close();
                }
            } catch (IOException e) {
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
            int readBytes = 0;
            while (mmInStream != null && readBytes <= 0) {
                try {
                    byte[] buffer = new byte[4096];

                    // 读取输入流
                    readBytes = mmInStream.read(buffer);

                    byte[] data = new byte[readBytes];
                    for (int i=0; i<readBytes; i++) {
                        data[i] = buffer[i];
                    }

                    String str = new String(data);
                    System.out.println(" 》》》》 》》》》 获得字符串[" + readBytes + "]：" + str);

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }
    }
}
