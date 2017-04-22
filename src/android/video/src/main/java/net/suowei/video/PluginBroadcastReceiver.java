package net.suowei.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class PluginBroadcastReceiver extends BroadcastReceiver
{
    public static String BROADCASTCODE = "net.suowei.video.broadcast";

    public PluginCallback callback;

    public void onReceive(Context context, Intent intent)
    {
        JSONObject message = new JSONObject();
        try
        {
            message.put("code", intent.getStringExtra("code"));
            message.put("message", intent.getStringExtra("message"));
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        if(callback != null)
        {
            callback.callback(message);
        }
    }
}
