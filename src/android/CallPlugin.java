package net.suowei.call;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import net.suowei.video.PluginBroadcastReceiver;
import net.suowei.video.PluginCallback;
import net.suowei.video.User;
import net.suowei.video.VideoActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CallPlugin extends CordovaPlugin implements PluginCallback
{
    private PluginBroadcastReceiver pluginBroadcastReceiver = new PluginBroadcastReceiver();

    private LocalBroadcastManager localBroadcastManager;

    private CallbackContext callbackContext;

    private JSONObject caller;

    private JSONObject callee;

    private JSONObject configs;


    public void initialize(CordovaInterface cordova, CordovaWebView webView)
    {
        super.initialize(cordova, webView);

        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.cordova.getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PluginBroadcastReceiver.BROADCASTCODE);
        pluginBroadcastReceiver.callback = this;
        localBroadcastManager.registerReceiver(pluginBroadcastReceiver, intentFilter);
    }

    public void onDestroy()
    {
        localBroadcastManager.unregisterReceiver(pluginBroadcastReceiver);
        super.onDestroy();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {
        if(action.equals("videocall"))
        {
            this.callbackContext = callbackContext;


            caller = args.getJSONObject(0);
            callee = args.getJSONObject(1);
            configs = args.getJSONObject(2);

            cordova.getThreadPool().execute(new Runnable()
            {
                public void run()
                {
                    try
                    {

                        CallPlugin.this.videocall();
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void videocall() throws JSONException
    {
        Intent intent = new Intent();
        intent.setClass(cordova.getActivity(), VideoActivity.class);

        User usercaller = new User();
        usercaller.id = caller.optString("id");
        usercaller.name = caller.optString("name");
        usercaller.icon = caller.optString("icon");

        User usercallee = new User();
        usercallee.id = callee.optString("id");
        usercallee.name = callee.optString("name");
        usercallee.icon = callee.optString("icon");

        String url = configs.optString("url");
        String stun = configs.optString("stun");

        intent.putExtra("caller", usercaller);
        intent.putExtra("callee", usercallee);
        intent.putExtra("url", url);
        intent.putExtra("stun", stun);

        this.cordova.startActivityForResult(this, intent, 100);
    }

    @Override
    public void callback(JSONObject message)
    {
        Log.e("CallPlugin回调函数", message.toString());

        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, message);
        pluginresult.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pluginresult);
    }
}
