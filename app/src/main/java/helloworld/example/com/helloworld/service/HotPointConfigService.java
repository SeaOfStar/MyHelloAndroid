package helloworld.example.com.helloworld.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;

import helloworld.example.com.helloworld.R;

public class HotPointConfigService extends WifiConfigService {

    static final String HOST_WIFI_PASSWORD = "2008gysghgg2018";
    static final String HOST_WIFI_IP = "192.168.128.168";

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
                    + "&pw=" + HOST_WIFI_PASSWORD
                    + "&ip=" + HOST_WIFI_IP;
            return WifiConfigService.generateBitmap(qrcodeInfo, 400, 400);
        }
        else {
            return null;
        }
    }

    protected String getHostSSID() {
        String deviceId = getDeviceId();
        if(deviceId != null) {

        }

        return null;
    }
}
