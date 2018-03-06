package helloworld.example.com.helloworld.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import helloworld.example.com.helloworld.R;

public class HotPointConfigService extends WifiConfigService {

    static final String TAG = "HotPointConfigService";

    static final String HOST_WIFI_PASSWORD = "2008gysghgg2018";
    static final String HOST_WIFI_IP = "192.168.128.168";

    private WifiManager wifiManager;

    public HotPointConfigService() {


    }

    @Override
    public Bitmap getQrCodeBitMap() {
        String deviceId = this.getDeviceId();
        if(deviceId != null) {
            String qrcodeInfo = getString(R.string.qrcode_pre)
                    + "?deviceid=" + deviceId
                    + "&type=HotPoint"
                    + "&ssid=" + getHostSSID()
//                    + "&pw=" + HOST_WIFI_PASSWORD
                    + "&ip=" + HOST_WIFI_IP;
            return WifiConfigService.generateBitmap(qrcodeInfo, 400, 400);
        }
        else {
            return null;
        }
    }

    @Override
    protected void didConfimedDeviceId() {
        super.didConfimedDeviceId();
        startHotPoint();
    }

    private void startHotPoint() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }

        WifiConfiguration cfg = new WifiConfiguration();
        cfg.SSID = getHostSSID();
//        cfg.preSharedKey = HOST_WIFI_PASSWORD;
        cfg.wepKeys[0] = "";
        cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        cfg.wepTxKeyIndex = 0;

        //通过反射调用设置热点
        try {
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, cfg, true);
            if (enable) {
                Log.d(TAG, "热点已开启 SSID:" + getHostSSID());
            } else {
                Log.d(TAG, "创建热点失败");
            }
        } catch (Exception e) {
            Log.d(TAG, "创建热点失败");
            e.printStackTrace();
        }
    }

    protected String getHostSSID() {
        String deviceId = getDeviceId();
        if(deviceId != null) {
            return "HP_SSID";
        }

        return null;
    }
}
