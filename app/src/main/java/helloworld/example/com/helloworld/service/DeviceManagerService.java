package helloworld.example.com.helloworld.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import helloworld.example.com.helloworld.R;


public class DeviceManagerService extends Service {

    static private final String DEVICE_INFO_KEY = "CLOUD_SHOW_DEVICE_INFO_KEY";
    static private final String DEVICE_ID_LOAD_KEY = "DEVICE_ID_KEY";

//    static public final String DEVICE_ID = "DEVICE_ID";

    static private int mTempIndex = 0;

    private String mDeviceId = null;

    public String getDeviceId() {
        if(mDeviceId == null) {
            reloadDeviceId();
        }
        return mDeviceId;
    }

    public DeviceManagerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mTempIndex ++;
        System.out.println("DeviceManagerService索引：" + mTempIndex);

        // 生成唯一设备标示
        reloadDeviceId();

        // 监听请求设备ID的消息
        regDeviceIdRequest();

        // 配置蓝牙网络

        // 显示唯一设备标示
//        showDeviceID();
    }

    private DeviceIdReceiver mReceiver;
    private void regDeviceIdRequest() {
        mReceiver = new DeviceIdReceiver();
        IntentFilter filter = new IntentFilter(getString(R.string.action_request_device_id));
        registerReceiver(mReceiver, filter);
    }

    private void showDeviceID() {
        Intent intent = new Intent();
        intent.setAction(getString(R.string.intent_action_start_device_id_show));
        intent.putExtra(getString(R.string.device_id_key), mDeviceId);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
        sendBroadcast(intent);
    }

    private void reloadDeviceId() {
        mDeviceId = loadDeviceId();
    }

    private String loadDeviceId() {
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(DEVICE_INFO_KEY, 0);
        String deviceId = settings.getString(DEVICE_ID_LOAD_KEY, java.util.UUID.randomUUID().toString());
        settings.edit().putString(DEVICE_ID_LOAD_KEY, deviceId).commit();
        return deviceId;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DeviceManagerBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("DeviceManagerService接收到命令: " + intent.getAction() + "<" + intent.getFlags() + ">");

        String action = intent.getAction();
        if(action != null && action.equals(getString(R.string.action_request_device_id))) {
            broadcastDeviceId();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 广播设备ID信息
     */
    private void broadcastDeviceId() {
        Intent intent = new Intent();
        intent.setAction(getString(R.string.action_broadcast_device_id));
        intent.putExtra(getString(R.string.device_id_key), mDeviceId);
        sendBroadcast(intent);
    }

    /**
     * bind用的内部类
     */
    public class DeviceManagerBinder extends Binder {

        /**
         * 获取当前service的实例
         * @return
         */
        public DeviceManagerService getService() {
            return DeviceManagerService.this;
        }
    }

    private class DeviceIdReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(getString(R.string.action_request_device_id))) {
                broadcastDeviceId();
            }
        }
    }
}
