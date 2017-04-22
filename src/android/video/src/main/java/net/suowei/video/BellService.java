package net.suowei.video;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class BellService extends Service
{
    private MediaPlayer player;

    public void onCreate() {
        player = MediaPlayer.create(getApplicationContext(), R.raw.bell);
        player.setLooping(true);
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        player.start();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy()
    {
        player.stop();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent)
    {
        player.start();
        return null;
    }

    public boolean onUnbind(Intent intent)
    {
        player.stop();
        return super.onUnbind(intent);
    }

}
