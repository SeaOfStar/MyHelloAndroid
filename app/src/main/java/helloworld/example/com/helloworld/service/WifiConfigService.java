package helloworld.example.com.helloworld.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;

import helloworld.example.com.helloworld.R;

import static helloworld.example.com.helloworld.R.*;

public abstract class WifiConfigService extends Service {


    public WifiConfigService() {
    }

    private DeviceManagerService mDeviceService = null;
    public String getDeviceId() {
        if(mDeviceService != null) {
            return mDeviceService.getDeviceId();
        }
        else {
            return null;
        }
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            System.out.println("绑定设备ID服务成功！");
            mDeviceService = ((DeviceManagerService.DeviceManagerBinder)iBinder).getService();
            // 广播设备ID就绪的消息
            broadcastQRCodeReadyMsg();
            didConfimedDeviceId();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDeviceService = null;
        }
    };

    abstract public Bitmap getQrCodeBitMap();
    protected void didConfimedDeviceId() {
        // do nothing
    }

    public void broadcastQRCodeReadyMsg() {
        // 全局广播QRCode图片就绪的消息
        Intent intent = new Intent(getString(R.string.action_bitmap_ready));
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new WifiConfigBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 绑定设备服务，取得设备号。
        registerService();
    }

    @Override
    public void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("接收到命令：" + intent.getAction() + "<" + flags + ">: " + startId);
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerService() {
        Intent intent = new Intent(getString(string.action_request_device_id));
        intent.setClass(this, DeviceManagerService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    public class WifiConfigBinder extends Binder {
        public WifiConfigService getService() {
            return WifiConfigService.this;
        }
    }


    /**
     * 从文字生成QRCode
     */
    public static Bitmap generateBitmap(String content, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, String> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        try {
            BitMatrix encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    pixels[i * width + j] = encode.get(j, i) ? 0x00000000 : 0xffffffff;
                }
            }
            return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class WifiConnector {
        final String ssidToConnect;
        final String password;
        final int cipherType;       // 使用WifiManager中的定义
        private WifiManager wifiManager;

//        public enum WifiCipherType {
//            WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
//        }

        public WifiConnector(String ssidToConnect, String password, int cipherType) {
            this.ssidToConnect = ssidToConnect;
            this.password = password;
            this.cipherType = cipherType;

            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }

        private WifiConfiguration createConfig() {
            WifiConfiguration config = new WifiConfiguration();
            config.allowedAuthAlgorithms.clear();
            config.allowedGroupCiphers.clear();
            config.allowedKeyManagement.clear();
            config.allowedPairwiseCiphers.clear();
            config.allowedProtocols.clear();
            config.SSID = "\"" + ssidToConnect + "\"";

            switch (this.cipherType) {
                case WifiConfiguration.KeyMgmt.NONE:
                {
                    config.wepKeys[0] = "";
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.wepTxKeyIndex = 0;
                }
                break;

                case WifiConfiguration.KeyMgmt.WPA_PSK:
                {
                    config.preSharedKey = "\""+password+"\"";
                    config.hiddenSSID = true;
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    config.status = WifiConfiguration.Status.ENABLED;
                }
                break;

                case WifiConfiguration.KeyMgmt.WPA_EAP:
                {
                    config.hiddenSSID = true;
                    config.wepKeys[0]= "\""+password+"\"";
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.wepTxKeyIndex = 0;
                }
                break;
            }
            return config;
        }

        public void connect() {
            WifiConfiguration cfg = this.createConfig();
            int configId = wifiManager.addNetwork(cfg);
            boolean b = wifiManager.enableNetwork(configId, true);
        }
    }



    /**
     * 配置Wifi连接参数并启动
     */
    public boolean configWifi(final String ssid, final String password, final int type) {

        WifiConnector connector = new WifiConnector(ssid, password, type);
        connector.connect();

        return true;
    }

}
