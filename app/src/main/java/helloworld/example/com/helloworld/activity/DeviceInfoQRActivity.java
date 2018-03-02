package helloworld.example.com.helloworld.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import helloworld.example.com.helloworld.R;
import helloworld.example.com.helloworld.service.BlueToothConfigService;
import helloworld.example.com.helloworld.service.WifiConfigService;

public class DeviceInfoQRActivity extends AppCompatActivity {

    private WifiConfigService mQRCodeMakerService = null;
    private WifiConfigService.WifiConfigBinder binder = null;

    private View mContentView;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            System.out.println("绑定QR生成服务成功！");
            binder = (WifiConfigService.WifiConfigBinder)iBinder;
            mQRCodeMakerService = binder.getService();
         }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mQRCodeMakerService = null;
        }
    };
    private QRReadyBroadcastReceiver mQRReadyReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_info_qr);

        mContentView = findViewById(R.id.fullscreen_Image);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // bindto service
        bindToService();
    }

    @Override
    public void onDestroy() {
        unbindService(conn);
        unregisterReceiver(mQRReadyReceiver);
        super.onDestroy();
    }

    private void bindToService() {
        // 监听蓝牙图片就绪的消息
        IntentFilter filter = new IntentFilter(getString(R.string.action_bitmap_ready));
        mQRReadyReceiver = new QRReadyBroadcastReceiver();
        registerReceiver(mQRReadyReceiver, filter);


        // 启动生成蓝牙图片的服务
        Intent intent = new Intent();
        intent.setClass(DeviceInfoQRActivity.this, BlueToothConfigService.class);
        intent.setAction(getString(R.string.action_request_bluetooth_qrbitmap));
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private class QRReadyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                if (intent.getAction().equals(getString(R.string.action_bitmap_ready))) {
                    showQRCode();
                }
            }
        }
    }

    private void showQRCode() {
        if (mQRCodeMakerService != null) {
            Bitmap bm = mQRCodeMakerService.getQrCodeBitMap();
            ((ImageView)mContentView).setImageBitmap(bm);
        }
    }
}
