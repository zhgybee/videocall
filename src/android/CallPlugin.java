package net.suowei.call;

import android.content.Intent;
import net.suowei.video.VideoActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class CallPlugin extends CordovaPlugin
{

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {
        if(action.equals("videocall"))
        {
            String caller = args.getString(0);
            String callee = args.getString(1);
            this.videocall(caller, callee, callbackContext);
            return true;
        }
        return false;
    }

    private void videocall(String caller, String callee, CallbackContext callbackContext)
    {
        if(!caller.equals(""))
        {
            Intent intent = new Intent();
            intent.setClass(cordova.getActivity(), VideoActivity.class);
            intent.putExtra("caller", caller);
            intent.putExtra("callee", callee);
            this.cordova.startActivityForResult(this, intent, 1);
            callbackContext.success("");
        }
        else
        {
            callbackContext.error("呼叫人为空");
        }
    }
}
