package helloworld.example.com.helloworld.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MediaManagerService extends Service {
    public MediaManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        System.out.println("onBind");
        return null;
    }

    @Override
    public void onCreate() {
        System.out.println("on create");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("接收到命令: " + intent.getAction() + "<" + intent.getFlags() + ">");
        return super.onStartCommand(intent, flags, startId);
    }
}
